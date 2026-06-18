package com.example.cybersource.service;

import com.example.cybersource.dto.TokenLifecycleQueueMessage;
import com.example.cybersource.dto.TokenLifecycleUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenLifecycleService.
 */
@ExtendWith(MockitoExtension.class)
class TokenLifecycleServiceTest {

    @Mock
    private SqsClient sqsClient;

    private TokenLifecycleService tokenLifecycleService;
    private ObjectMapper objectMapper;

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/token-lifecycle-queue";
    private static final String DLQ_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/token-lifecycle-dlq";

    @BeforeEach
    void setUp() {
        tokenLifecycleService = new TokenLifecycleService(sqsClient);
        ReflectionTestUtils.setField(tokenLifecycleService, "queueUrl", QUEUE_URL);
        ReflectionTestUtils.setField(tokenLifecycleService, "dlqUrl", DLQ_URL);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testProcessTokenLifecycleUpdate_Success() {
        // Arrange
        TokenLifecycleUpdateMessage message = createTestMessage();
        SendMessageResponse sqsResponse = SendMessageResponse.builder()
                .messageId("test-message-id-123")
                .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sqsResponse);

        // Act
        String result = tokenLifecycleService.processTokenLifecycleUpdate(message);

        // Assert
        assertEquals("test-message-id-123", result);
        
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        
        SendMessageRequest capturedRequest = captor.getValue();
        assertEquals(QUEUE_URL, capturedRequest.queueUrl());
        assertNotNull(capturedRequest.messageBody());
    }

    @Test
    void testProcessTokenLifecycleUpdate_SendsToDlqOnFailure() {
        // Arrange
        TokenLifecycleUpdateMessage message = createTestMessage();
        
        // First call fails, second call (to DLQ) succeeds
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("Queue error").build())
                .thenReturn(SendMessageResponse.builder().messageId("dlq-message-id").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tokenLifecycleService.processTokenLifecycleUpdate(message);
        });
        
        assertTrue(exception.getMessage().contains("DLQ"));
        
        // Verify both queue and DLQ were called
        verify(sqsClient, times(2)).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    void testProcessTokenLifecycleUpdate_ThrowsWhenBothQueueAndDlqFail() {
        // Arrange
        TokenLifecycleUpdateMessage message = createTestMessage();
        
        // Both calls fail
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
                .thenThrow(SqsException.builder().message("Queue error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tokenLifecycleService.processTokenLifecycleUpdate(message);
        });
        
        assertTrue(exception.getMessage().contains("Failed to process message and send to DLQ"));
    }

    @Test
    void testExtractQueueMessage_ExtractsAllFields() {
        // Arrange
        TokenLifecycleUpdateMessage message = createTestMessage();

        // Act
        TokenLifecycleQueueMessage result = tokenLifecycleService.extractQueueMessage(message);

        // Assert
        assertEquals("token-123", result.getTokenId());
        assertEquals("instr-456", result.getInstrumentIdentifierId());
        assertEquals("org-789", result.getOrganizationId());
        assertEquals("ACTIVE", result.getTokenStatus());
        assertEquals("TOKEN_UPDATED", result.getEventType());
        assertEquals("2024-01-15T10:30:00Z", result.getEventTimestamp());
        assertEquals("PAR-ABC-123", result.getPaymentAccountReference());
    }

    @Test
    void testExtractQueueMessage_HandlesNullTokenInfo() {
        // Arrange
        TokenLifecycleUpdateMessage message = TokenLifecycleUpdateMessage.builder()
                .eventType("TOKEN_DELETED")
                .eventTimestamp("2024-01-15T10:30:00Z")
                .tokenInformation(null)
                .instrumentIdentifier(TokenLifecycleUpdateMessage.InstrumentIdentifierInfo.builder()
                        .id("instr-456")
                        .build())
                .build();

        // Act
        TokenLifecycleQueueMessage result = tokenLifecycleService.extractQueueMessage(message);

        // Assert
        assertNull(result.getTokenId());
        assertNull(result.getTokenStatus());
        assertEquals("instr-456", result.getInstrumentIdentifierId());
        assertEquals("TOKEN_DELETED", result.getEventType());
    }

    @Test
    void testExtractQueueMessage_HandlesNullInstrumentIdentifier() {
        // Arrange
        TokenLifecycleUpdateMessage message = TokenLifecycleUpdateMessage.builder()
                .eventType("TOKEN_CREATED")
                .eventTimestamp("2024-01-15T10:30:00Z")
                .tokenInformation(TokenLifecycleUpdateMessage.TokenInfo.builder()
                        .tokenId("token-123")
                        .tokenStatus("ACTIVE")
                        .build())
                .instrumentIdentifier(null)
                .build();

        // Act
        TokenLifecycleQueueMessage result = tokenLifecycleService.extractQueueMessage(message);

        // Assert
        assertEquals("token-123", result.getTokenId());
        assertEquals("ACTIVE", result.getTokenStatus());
        assertNull(result.getInstrumentIdentifierId());
    }

    @Test
    void testExtractQueueMessage_HandlesNullOrganizationInfo() {
        // Arrange
        TokenLifecycleUpdateMessage message = TokenLifecycleUpdateMessage.builder()
                .eventType("TOKEN_SUSPENDED")
                .eventTimestamp("2024-01-15T10:30:00Z")
                .tokenInformation(TokenLifecycleUpdateMessage.TokenInfo.builder()
                        .tokenId("token-123")
                        .build())
                .organizationInformation(null)
                .build();

        // Act
        TokenLifecycleQueueMessage result = tokenLifecycleService.extractQueueMessage(message);

        // Assert
        assertEquals("token-123", result.getTokenId());
        assertNull(result.getOrganizationId());
    }

    @Test
    void testProcessTokenLifecycleUpdate_VerifiesMessageContent() throws Exception {
        // Arrange
        TokenLifecycleUpdateMessage message = createTestMessage();
        SendMessageResponse sqsResponse = SendMessageResponse.builder()
                .messageId("test-message-id-456")
                .build();
        
        when(sqsClient.sendMessage(any(SendMessageRequest.class))).thenReturn(sqsResponse);

        // Act
        tokenLifecycleService.processTokenLifecycleUpdate(message);

        // Assert - verify message content
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(captor.capture());
        
        String messageBody = captor.getValue().messageBody();
        TokenLifecycleQueueMessage sentMessage = objectMapper.readValue(messageBody, TokenLifecycleQueueMessage.class);
        
        assertEquals("token-123", sentMessage.getTokenId());
        assertEquals("instr-456", sentMessage.getInstrumentIdentifierId());
        assertEquals("org-789", sentMessage.getOrganizationId());
    }

    /**
     * Helper method to create a test message with all fields populated.
     */
    private TokenLifecycleUpdateMessage createTestMessage() {
        return TokenLifecycleUpdateMessage.builder()
                .eventType("TOKEN_UPDATED")
                .eventTimestamp("2024-01-15T10:30:00Z")
                .tokenInformation(TokenLifecycleUpdateMessage.TokenInfo.builder()
                        .tokenId("token-123")
                        .tokenStatus("ACTIVE")
                        .expirationMonth("12")
                        .expirationYear("2025")
                        .paymentAccountReference("PAR-ABC-123")
                        .build())
                .instrumentIdentifier(TokenLifecycleUpdateMessage.InstrumentIdentifierInfo.builder()
                        .id("instr-456")
                        .state("ACTIVE")
                        .build())
                .organizationInformation(TokenLifecycleUpdateMessage.OrganizationInfo.builder()
                        .organizationId("org-789")
                        .merchantName("Test Merchant")
                        .build())
                .build();
    }
}
