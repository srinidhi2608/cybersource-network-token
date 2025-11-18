package com.example.cybersource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService.
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new EncryptionService();
    }

    @Test
    void testEncrypt_Success() {
        String plainText = "1234567890123456";
        String encrypted = encryptionService.encrypt(plainText);
        
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);
    }

    @Test
    void testDecrypt_Success() {
        String plainText = "1234567890123456";
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncryptDecrypt_RoundTrip() {
        String originalText = "test-network-token-123";
        String encrypted = encryptionService.encrypt(originalText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(originalText, decrypted);
    }

    @Test
    void testEncrypt_NullInput() {
        String encrypted = encryptionService.encrypt(null);
        assertNull(encrypted);
    }

    @Test
    void testEncrypt_EmptyInput() {
        String encrypted = encryptionService.encrypt("");
        assertNull(encrypted);
    }

    @Test
    void testDecrypt_NullInput() {
        String decrypted = encryptionService.decrypt(null);
        assertNull(decrypted);
    }

    @Test
    void testDecrypt_EmptyInput() {
        String decrypted = encryptionService.decrypt("");
        assertNull(decrypted);
    }

    @Test
    void testEncrypt_SpecialCharacters() {
        String plainText = "!@#$%^&*()_+-=[]{}|;':,.<>?";
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plainText, decrypted);
    }

    @Test
    void testEncrypt_LongString() {
        String plainText = "1".repeat(1000);
        String encrypted = encryptionService.encrypt(plainText);
        String decrypted = encryptionService.decrypt(encrypted);
        
        assertEquals(plainText, decrypted);
    }
}
