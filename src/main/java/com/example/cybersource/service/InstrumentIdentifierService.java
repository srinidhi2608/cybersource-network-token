package com.example.cybersource.service;

import com.cybersource.authsdk.core.MerchantConfig;
import com.example.cybersource.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.example.cybersource.config.CybersourceConfig;

import java.time.Duration;

@Service
public class InstrumentIdentifierService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentIdentifierService.class);

    @Autowired
    private CybersourceConfig cybersourceConfig;
    
    @Autowired
    private WebClient webClient;
    
    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    public String createInstrumentIdentifier(String cardNumber, String merchantId) throws CybersourceException {
        logger.info("Creating instrument identifier for merchant: {} and card ending in: {}", 
                   merchantId, cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
        
        try {
            String payload = String.format("""
            {
                "card": {
                    "number": "%s"
                }
            }
            """, cardNumber);

            // Build the API path
            String path = "/pts/v2/instrumentidentifiers";
            
            // Generate JWT for authentication with the specific merchant ID
            String jwt = jwtTokenUtil.generateJwt(
                merchantId, 
                cybersourceConfig.getApiKey(), 
                cybersourceConfig.getSecretKey(), 
                path, 
                "POST"
            );
            
            // Make the API call using WebClient with specific merchant ID
            String response = webClient.post()
                    .uri(cybersourceConfig.getBaseUrl() + path)
                    .header("Content-Type", "application/json")
                    .header("v-c-merchant-id", merchantId)
                    .header("Authorization", "Bearer " + jwt)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            logger.info("Successfully created instrument identifier for merchant: {}", merchantId);
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("Cybersource API error while creating instrument identifier for merchant {}: Status={}, Body={}", 
                        merchantId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CybersourceApiException(
                "Failed to create instrument identifier", 
                e.getStatusCode().value(), 
                e.getResponseBodyAsString(), 
                e
            );
        } catch (WebClientException e) {
            logger.error("Network error while creating instrument identifier for merchant {}", merchantId, e);
            throw new NetworkException("Network error while calling Cybersource API", e);
        } catch (Exception e) {
            logger.error("Unexpected error while creating instrument identifier for merchant {}", merchantId, e);
            throw new CybersourceException("Unexpected error while creating instrument identifier", e);
        }
    }

    public String getInstrumentIdentifier(String instrumentIdentifierTokenId, String merchantId) throws CybersourceException {
        logger.info("Getting instrument identifier: {} for merchant: {}", instrumentIdentifierTokenId, merchantId);
        
        try {
            // Build the API path
            String path = "/pts/v2/instrumentidentifiers/" + instrumentIdentifierTokenId;
            
            // Generate JWT for authentication with the specific merchant ID
            String jwt = jwtTokenUtil.generateJwt(
                merchantId, 
                cybersourceConfig.getApiKey(), 
                cybersourceConfig.getSecretKey(), 
                path, 
                "GET"
            );
            
            // Make the API call using WebClient with specific merchant ID
            String response = webClient.get()
                    .uri(cybersourceConfig.getBaseUrl() + path)
                    .header("v-c-merchant-id", merchantId)
                    .header("Authorization", "Bearer " + jwt)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
            
            logger.info("Successfully retrieved instrument identifier: {} for merchant: {}", 
                       instrumentIdentifierTokenId, merchantId);
            return response;
            
        } catch (WebClientResponseException e) {
            logger.error("Cybersource API error while getting instrument identifier {} for merchant {}: Status={}, Body={}", 
                        instrumentIdentifierTokenId, merchantId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new CybersourceApiException(
                "Failed to get instrument identifier", 
                e.getStatusCode().value(), 
                e.getResponseBodyAsString(), 
                e
            );
        } catch (WebClientException e) {
            logger.error("Network error while getting instrument identifier {} for merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw new NetworkException("Network error while calling Cybersource API", e);
        } catch (Exception e) {
            logger.error("Unexpected error while getting instrument identifier {} for merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw new CybersourceException("Unexpected error while getting instrument identifier", e);
        }
    }
}