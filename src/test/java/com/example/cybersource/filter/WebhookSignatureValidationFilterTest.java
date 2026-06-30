package com.example.cybersource.filter;

import com.example.cybersource.service.TokenLifecycleService;
import com.example.cybersource.util.SignatureValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for WebhookSignatureValidationFilter.
 */
@WebMvcTest
@ContextConfiguration(classes = {
        com.example.cybersource.controller.TokenLifecycleController.class,
        com.example.cybersource.filter.WebhookSignatureValidationFilter.class,
        com.example.cybersource.util.SignatureValidator.class,
        com.example.cybersource.config.SecurityConfig.class
})
@TestPropertySource(properties = {
        "webhook.signature.secret=test-webhook-secret-key"
})
class WebhookSignatureValidationFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SignatureValidator signatureValidator;

    @MockBean
    private TokenLifecycleService tokenLifecycleService;

    private static final String WEBHOOK_ENDPOINT = "/api/v1/webhooks/token-lifecycle";
    private static final String SIGNATURE_HEADER = "v-c-signature";
    private static final String TEST_SECRET = "test-webhook-secret-key";
    private static final String TEST_REQUEST_BODY = "{\"eventType\":\"TOKEN_UPDATED\",\"eventTimestamp\":\"2024-01-15T10:30:00Z\",\"tokenInformation\":{\"tokenId\":\"token-123\"}}";

    private String validSignature;

    @BeforeEach
    void setUp() throws Exception {
        validSignature = signatureValidator.generateSignature(TEST_REQUEST_BODY, TEST_SECRET);
        // Mock the service to return a successful response
        when(tokenLifecycleService.processTokenLifecycleUpdate(any())).thenReturn("test-message-id");
    }

    @Test
    void testWebhookRequest_WithValidSignature_ShouldSucceed() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header(SIGNATURE_HEADER, validSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEST_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testWebhookRequest_WithInvalidSignature_ShouldFail() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header(SIGNATURE_HEADER, "invalid-signature")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEST_REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }

    @Test
    void testWebhookRequest_WithMissingSignature_ShouldFail() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEST_REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing signature header"));
    }

    @Test
    void testWebhookRequest_WithEmptySignature_ShouldFail() throws Exception {
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header(SIGNATURE_HEADER, "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TEST_REQUEST_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Missing signature header"));
    }

    @Test
    void testWebhookRequest_WithEmptyBody_ShouldFail() throws Exception {
        String emptyBodySignature = signatureValidator.generateSignature("", TEST_SECRET);
        
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header(SIGNATURE_HEADER, emptyBodySignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Empty request body"));
    }

    @Test
    void testWebhookRequest_WithModifiedBody_ShouldFail() throws Exception {
        String modifiedBody = TEST_REQUEST_BODY + " ";
        
        mockMvc.perform(post(WEBHOOK_ENDPOINT)
                        .header(SIGNATURE_HEADER, validSignature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(modifiedBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid signature"));
    }
}
