package com.example.cybersource.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SignatureValidator.
 */
class SignatureValidatorTest {

    private SignatureValidator signatureValidator;
    private static final String TEST_SECRET = "test-secret-key";
    private static final String TEST_BODY = "{\"eventType\":\"TOKEN_UPDATED\",\"tokenId\":\"123\"}";

    @BeforeEach
    void setUp() {
        signatureValidator = new SignatureValidator();
    }

    @Test
    void testGenerateSignature_Success() throws Exception {
        // Act
        String signature = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);

        // Assert
        assertNotNull(signature);
        assertFalse(signature.isEmpty());
        assertTrue(signature.length() > 0);
    }

    @Test
    void testGenerateSignature_ConsistentOutput() throws Exception {
        // Act
        String signature1 = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);
        String signature2 = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);

        // Assert
        assertEquals(signature1, signature2, "Same input should produce same signature");
    }

    @Test
    void testGenerateSignature_DifferentSecretProducesDifferentSignature() throws Exception {
        // Act
        String signature1 = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);
        String signature2 = signatureValidator.generateSignature(TEST_BODY, "different-secret");

        // Assert
        assertNotEquals(signature1, signature2, "Different secrets should produce different signatures");
    }

    @Test
    void testGenerateSignature_DifferentBodyProducesDifferentSignature() throws Exception {
        // Act
        String signature1 = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);
        String signature2 = signatureValidator.generateSignature("{\"eventType\":\"TOKEN_CREATED\"}", TEST_SECRET);

        // Assert
        assertNotEquals(signature1, signature2, "Different bodies should produce different signatures");
    }

    @Test
    void testValidateSignature_ValidSignature() throws Exception {
        // Arrange
        String validSignature = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);

        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, validSignature, TEST_SECRET);

        // Assert
        assertTrue(isValid, "Valid signature should be accepted");
    }

    @Test
    void testValidateSignature_InvalidSignature() {
        // Arrange
        String invalidSignature = "invalid-signature-12345";

        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, invalidSignature, TEST_SECRET);

        // Assert
        assertFalse(isValid, "Invalid signature should be rejected");
    }

    @Test
    void testValidateSignature_ModifiedBody() throws Exception {
        // Arrange
        String originalSignature = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);
        String modifiedBody = TEST_BODY + " ";

        // Act
        boolean isValid = signatureValidator.validateSignature(modifiedBody, originalSignature, TEST_SECRET);

        // Assert
        assertFalse(isValid, "Modified body should fail validation");
    }

    @Test
    void testValidateSignature_NullBody() {
        // Act
        boolean isValid = signatureValidator.validateSignature(null, "signature", TEST_SECRET);

        // Assert
        assertFalse(isValid, "Null body should fail validation");
    }

    @Test
    void testValidateSignature_NullSignature() {
        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, null, TEST_SECRET);

        // Assert
        assertFalse(isValid, "Null signature should fail validation");
    }

    @Test
    void testValidateSignature_NullSecret() throws Exception {
        // Arrange
        String signature = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);

        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, signature, null);

        // Assert
        assertFalse(isValid, "Null secret should fail validation");
    }

    @Test
    void testValidateSignature_EmptyBody() {
        // Act
        boolean isValid = signatureValidator.validateSignature("", "signature", TEST_SECRET);

        // Assert
        assertFalse(isValid, "Empty body should fail validation");
    }

    @Test
    void testValidateSignature_EmptySignature() {
        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, "", TEST_SECRET);

        // Assert
        assertFalse(isValid, "Empty signature should fail validation");
    }

    @Test
    void testValidateSignature_EmptySecret() throws Exception {
        // Arrange
        String signature = signatureValidator.generateSignature(TEST_BODY, TEST_SECRET);

        // Act
        boolean isValid = signatureValidator.validateSignature(TEST_BODY, signature, "");

        // Assert
        assertFalse(isValid, "Empty secret should fail validation");
    }
}
