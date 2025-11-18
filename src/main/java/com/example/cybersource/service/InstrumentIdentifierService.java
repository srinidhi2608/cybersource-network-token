package com.example.cybersource.service;

import com.cybersource.authsdk.core.MerchantConfig;
import com.example.cybersource.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.cybersource.config.CybersourceConfig;

import java.time.Duration;

@Service
public class InstrumentIdentifierService {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentIdentifierService.class);

    @Autowired
    private CybersourceConfig cybersourceConfig;
    
    @Autowired
    private CybersourceRestClient cybersourceRestClient;

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
            
            // Make the API call using RestClient
            String response = cybersourceRestClient.post(path, payload, merchantId);
            
            logger.info("Successfully created instrument identifier for merchant: {}", merchantId);
            return response;
            
        } catch (CybersourceApiException | NetworkException e) {
            logger.error("Error while creating instrument identifier for merchant {}", merchantId, e);
            throw e;
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
            
            // Make the API call using RestClient
            String response = cybersourceRestClient.get(path, merchantId);
            
            logger.info("Successfully retrieved instrument identifier: {} for merchant: {}", 
                       instrumentIdentifierTokenId, merchantId);
            return response;
            
        } catch (CybersourceApiException | NetworkException e) {
            logger.error("Error while getting instrument identifier {} for merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while getting instrument identifier {} for merchant {}", 
                        instrumentIdentifierTokenId, merchantId, e);
            throw new CybersourceException("Unexpected error while getting instrument identifier", e);
        }
    }
}