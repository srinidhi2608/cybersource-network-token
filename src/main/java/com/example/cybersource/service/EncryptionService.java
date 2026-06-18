package com.example.cybersource.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for encrypting and decrypting network tokens using Base64 encoding.
 */
@Service
public class EncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(EncryptionService.class);

    /**
     * Encrypts a network token using Base64 encoding.
     *
     * @param networkToken the network token to encrypt
     * @return the Base64 encoded network token
     */
    public String encrypt(String networkToken) {
        if (networkToken == null || networkToken.isEmpty()) {
            logger.warn("Attempted to encrypt null or empty network token");
            return null;
        }
        
        try {
            String encrypted = Base64.getEncoder().encodeToString(networkToken.getBytes(StandardCharsets.UTF_8));
            logger.debug("Successfully encrypted network token");
            return encrypted;
        } catch (Exception e) {
            logger.error("Error encrypting network token", e);
            throw new RuntimeException("Failed to encrypt network token", e);
        }
    }

    /**
     * Decrypts a Base64 encoded network token.
     *
     * @param encryptedToken the Base64 encoded network token
     * @return the decrypted network token
     */
    public String decrypt(String encryptedToken) {
        if (encryptedToken == null || encryptedToken.isEmpty()) {
            logger.warn("Attempted to decrypt null or empty encrypted token");
            return null;
        }
        
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedToken);
            String decrypted = new String(decodedBytes, StandardCharsets.UTF_8);
            logger.debug("Successfully decrypted network token");
            return decrypted;
        } catch (Exception e) {
            logger.error("Error decrypting network token", e);
            throw new RuntimeException("Failed to decrypt network token", e);
        }
    }
}
