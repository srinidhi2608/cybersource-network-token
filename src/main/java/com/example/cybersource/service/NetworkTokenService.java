package com.example.cybersource.service;

import com.example.cybersource.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

@Service
public class NetworkTokenService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkTokenService.class);
    
    private final InstrumentIdentifierService instrumentIdentifierService;
    private final PaymentCredentialsService paymentCredentialsService;

    public NetworkTokenService(InstrumentIdentifierService instrumentIdentifierService, PaymentCredentialsService paymentCredentialsService) {
        this.instrumentIdentifierService = instrumentIdentifierService;
        this.paymentCredentialsService = paymentCredentialsService;
    }

    public NetworkTokenResult generateNetworkTokenAndCryptogram(String cardNumber) throws CybersourceException {
        logger.info("Generating network token and cryptogram for card number ending in: {}", 
                   cardNumber.substring(Math.max(0, cardNumber.length() - 4)));
        
        long start = System.currentTimeMillis();

        try {
            // Step 1: Create Instrument Identifier
            String instrumentResponse = instrumentIdentifierService.createInstrumentIdentifier(cardNumber);
            JSONObject instrumentJson = new JSONObject(instrumentResponse);
            String instrumentIdentifierId = instrumentJson.getString("id");

            // Step 2: Get Network Token and Cryptogram with enhanced error handling
            String credentialsResponse = paymentCredentialsService.getPaymentCredentials(instrumentIdentifierId);
            JSONObject credentialsJson = new JSONObject(credentialsResponse);

            // Parse network token and cryptogram from response
            String networkToken = credentialsJson.getJSONObject("networkToken").getString("number");
            String cryptogram = credentialsJson.getJSONObject("networkToken").getString("cryptogram");

            long end = System.currentTimeMillis();
            long elapsedMs = end - start;
            
            logger.info("Successfully generated network token and cryptogram in {}ms", elapsedMs);

            return new NetworkTokenResult(networkToken, cryptogram, elapsedMs);
            
        } catch (PaymentCredentialsException | NetworkException | DataAccessException | CybersourceApiException e) {
            logger.error("Failed to generate network token and cryptogram", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error while generating network token and cryptogram", e);
            throw new CybersourceException("Unexpected error while generating network token and cryptogram", e);
        }
    }

    public static record NetworkTokenResult(String networkToken, String cryptogram, long elapsedMilliseconds) {}
}