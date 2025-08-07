package com.example.cybersource.service;

import com.example.cybersource.entity.TokenStorage;
import com.example.cybersource.exception.*;
import com.example.cybersource.repository.TokenStorageRepository;
import com.example.cybersource.config.CybersourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentCredentialsService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCredentialsService.class);
    
    @Autowired
    private WebClient webClient;
    
    @Autowired
    private CybersourceConfig cybersourceConfig;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    
    @Autowired
    private TokenStorageRepository tokenStorageRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Enhanced getPaymentCredentials with robust exception handling and MongoDB persistence
     * @param instrumentIdentifierTokenId the instrument identifier token ID
     * @return the payment credentials response
     * @throws PaymentCredentialsException if payment credentials cannot be retrieved
     * @throws NetworkException if network connectivity issues occur
     * @throws DataAccessException if data access operations fail
     */
    public String getPaymentCredentials(String instrumentIdentifierTokenId) 
            throws PaymentCredentialsException, NetworkException, DataAccessException, CybersourceApiException {
        
        logger.info("Getting payment credentials for instrument identifier: {}", instrumentIdentifierTokenId);
        
        try {
            // Build the API path
            String path = "/pts/v2/instrumentidentifiers/" + instrumentIdentifierTokenId + "/networkTokens";
            
            // Generate JWT for authentication
            String jwt = generateJwtToken(path, "GET");
            
            // Make the API call using WebClient
            String response = makeApiCall(path, jwt);
            
            // Parse and persist the response
            persistPaymentCredentials(instrumentIdentifierTokenId, response);
            
            logger.info("Successfully retrieved and persisted payment credentials for instrument: {}", 
                       instrumentIdentifierTokenId);
            
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("Cybersource API error for instrument {}: Status={}, Body={}", 
                        instrumentIdentifierTokenId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CybersourceApiException(
                "Failed to get payment credentials from Cybersource API", 
                e.getStatusCode().value(), 
                e.getResponseBodyAsString(), 
                e
            );
        } catch (WebClientException e) {
            logger.error("Network error while calling Cybersource API for instrument {}", 
                        instrumentIdentifierTokenId, e);
            throw new NetworkException("Network error while calling Cybersource API", e);
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Database error while persisting payment credentials for instrument {}", 
                        instrumentIdentifierTokenId, e);
            throw new DataAccessException("Failed to persist payment credentials", e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting payment credentials for instrument {}", 
                        instrumentIdentifierTokenId, e);
            throw new PaymentCredentialsException("Unexpected error while getting payment credentials", e);
        }
    }
    
    /**
     * Query token storage by payment token ID
     * @param paymentTokenId the payment token ID to search for
     * @return Optional containing the token storage if found
     * @throws DataAccessException if data access operations fail
     */
    public Optional<TokenStorage> getTokenStorageByPaymentTokenId(String paymentTokenId) 
            throws DataAccessException {
        
        logger.info("Querying token storage for payment token ID: {}", paymentTokenId);
        
        try {
            Optional<TokenStorage> result = tokenStorageRepository.findByPaymentTokenId(paymentTokenId);
            logger.info("Token storage query result for payment token ID {}: {}", 
                       paymentTokenId, result.isPresent() ? "found" : "not found");
            return result;
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Database error while querying token storage for payment token ID {}", 
                        paymentTokenId, e);
            throw new DataAccessException("Failed to query token storage", e);
        }
    }
    
    private String generateJwtToken(String path, String method) throws PaymentCredentialsException {
        try {
            return jwtTokenUtil.generateJwt(
                cybersourceConfig.getMerchantId(), 
                cybersourceConfig.getApiKey(), 
                cybersourceConfig.getSecretKey(), 
                path, 
                method
            );
        } catch (Exception e) {
            throw new PaymentCredentialsException("Failed to generate JWT token", e);
        }
    }
    
    private String makeApiCall(String path, String jwt) {
        try {
            return webClient.get()
                    .uri(cybersourceConfig.getBaseUrl() + path)
                    .header("v-c-merchant-id", cybersourceConfig.getMerchantId())
                    .header("Authorization", "Bearer " + jwt)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            if (e instanceof WebClientResponseException) {
                throw (WebClientResponseException) e;
            } else if (e instanceof WebClientException) {
                throw (WebClientException) e;
            } else {
                throw new RuntimeException("Unexpected error during API call", e);
            }
        }
    }
    
    private void persistPaymentCredentials(String instrumentIdentifierTokenId, String response) 
            throws DataAccessException {
        
        try {
            // Parse the response to extract relevant information
            // This is a simplified parsing - in real implementation, you'd use proper JSON parsing
            String paymentTokenId = UUID.randomUUID().toString(); // Generate unique payment token ID
            String cryptogram = extractCryptogramFromResponse(response);
            String merchantId = cybersourceConfig.getMerchantId();
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("instrumentIdentifierTokenId", instrumentIdentifierTokenId);
            metadata.put("apiResponse", response);
            metadata.put("creationTimestamp", LocalDateTime.now().toString());
            
            // Create and save token storage
            TokenStorage tokenStorage = new TokenStorage(paymentTokenId, cryptogram, merchantId, metadata);
            tokenStorageRepository.save(tokenStorage);
            
            logger.info("Successfully persisted token storage with payment token ID: {}", paymentTokenId);
            
        } catch (org.springframework.dao.DataAccessException e) {
            throw new DataAccessException("Failed to persist payment credentials to database", e);
        } catch (Exception e) {
            throw new DataAccessException("Unexpected error while persisting payment credentials", e);
        }
    }
    
    private String extractCryptogramFromResponse(String response) {
        // Simplified cryptogram extraction - in real implementation, use proper JSON parsing
        try {
            // This would typically use Jackson or similar to parse JSON response
            // For now, return a placeholder
            return "extracted_cryptogram_" + System.currentTimeMillis();
        } catch (Exception e) {
            logger.warn("Could not extract cryptogram from response, using placeholder");
            return "placeholder_cryptogram";
        }
    }
}