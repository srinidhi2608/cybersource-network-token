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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentCredentialsService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentCredentialsService.class);
    
    @Autowired
    private CybersourceRestClient cybersourceRestClient;
    
    @Autowired
    private CybersourceConfig cybersourceConfig;
    
    @Autowired
    private TokenStorageRepository tokenStorageRepository;
    
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Enhanced getPaymentCredentials with robust exception handling and MongoDB persistence
     * @param instrumentIdentifierTokenId the instrument identifier token ID
     * @param merchantId the merchant ID for the request
     * @return the payment credentials response
     * @throws PaymentCredentialsException if payment credentials cannot be retrieved
     * @throws NetworkException if network connectivity issues occur
     * @throws DataAccessException if data access operations fail
     * @throws CybersourceApiException if Cybersource API errors occur
     */
    public String getPaymentCredentials(String instrumentIdentifierTokenId, String merchantId) 
            throws PaymentCredentialsException, NetworkException, DataAccessException, CybersourceApiException {
        
        logger.info("Getting payment credentials for instrument identifier: {} and merchant: {}", 
                   instrumentIdentifierTokenId, merchantId);
        
        try {
            // Build the API path
            String path = "/pts/v2/instrumentidentifiers/" + instrumentIdentifierTokenId + "/networkTokens";
            
            // Make the API call using RestClient
            String response = cybersourceRestClient.get(path, merchantId);
            
            // Parse and persist the response
            persistPaymentCredentials(instrumentIdentifierTokenId, merchantId, response);
            
            logger.info("Successfully retrieved and persisted payment credentials for instrument: {} and merchant: {}", 
                       instrumentIdentifierTokenId, merchantId);
            
            return response;
            
        } catch (CybersourceApiException | NetworkException e) {
            logger.error("API error for instrument {} and merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw e;
        } catch (DataAccessException e) {
            // Re-throw DataAccessException (from persistence operations)
            logger.error("Database error while persisting payment credentials for instrument {} and merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw e;
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Spring database error while persisting payment credentials for instrument {} and merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw new DataAccessException("Failed to persist payment credentials", e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting payment credentials for instrument {} and merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
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
    
    private void persistPaymentCredentials(String instrumentIdentifierTokenId, String merchantId, String response) 
            throws DataAccessException {
        
        try {
            // Parse the response to extract relevant information
            String paymentTokenId = UUID.randomUUID().toString(); // Generate unique payment token ID
            String cryptogram = extractCryptogramFromResponse(response);
            
            // Create metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("instrumentIdentifierTokenId", instrumentIdentifierTokenId);
            metadata.put("apiResponse", response);
            metadata.put("creationTimestamp", LocalDateTime.now().toString());
            
            // Create and save token storage
            TokenStorage tokenStorage = new TokenStorage(paymentTokenId, cryptogram, merchantId, metadata);
            tokenStorageRepository.save(tokenStorage);
            
            logger.info("Successfully persisted token storage with payment token ID: {} for merchant: {}", 
                       paymentTokenId, merchantId);
            
        } catch (org.springframework.dao.DataAccessException e) {
            throw new DataAccessException("Failed to persist payment credentials to database", e);
        }
        // Note: Let other exceptions bubble up so they can be caught by the main method
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