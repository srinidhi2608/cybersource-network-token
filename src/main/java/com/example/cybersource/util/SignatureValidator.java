package com.example.cybersource.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Utility class for validating HMAC-256 signatures for webhook requests.
 */
@Component
public class SignatureValidator {

    private static final Logger logger = LoggerFactory.getLogger(SignatureValidator.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Validates the HMAC-256 signature of a request body.
     *
     * @param requestBody the request body as a string
     * @param providedSignature the signature provided in the request header
     * @param secretKey the secret key to use for signature generation
     * @return true if the signature is valid, false otherwise
     */
    public boolean validateSignature(String requestBody, String providedSignature, String secretKey) {
        if (requestBody == null || providedSignature == null || secretKey == null) {
            logger.error("Signature validation failed: null parameters");
            return false;
        }

        try {
            String expectedSignature = generateSignature(requestBody, secretKey);
            boolean isValid = MessageDigest.isEqual(
                    providedSignature.getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8)
            );
            
            if (!isValid) {
                logger.warn("Signature validation failed: provided={}, expected={}", 
                        providedSignature, expectedSignature);
            } else {
                logger.debug("Signature validation successful");
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("Error validating signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Generates an HMAC-256 signature for the given request body.
     *
     * @param requestBody the request body as a string
     * @param secretKey the secret key to use for signature generation
     * @return the Base64-encoded signature
     * @throws NoSuchAlgorithmException if HMAC-SHA256 algorithm is not available
     * @throws InvalidKeyException if the secret key is invalid
     */
    public String generateSignature(String requestBody, String secretKey) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), 
                HMAC_SHA256_ALGORITHM
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
}
