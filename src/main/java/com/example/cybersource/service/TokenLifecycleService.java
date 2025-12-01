package com.example.cybersource.service;

import com.example.cybersource.dto.TokenLifecycleQueueMessage;
import com.example.cybersource.dto.TokenLifecycleUpdateMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * Service for processing token lifecycle update messages and forwarding to AWS SQS.
 */
@Service
public class TokenLifecycleService {

    private static final Logger logger = LoggerFactory.getLogger(TokenLifecycleService.class);

    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.sqs.queue-url}")
    private String queueUrl;

    @Value("${aws.sqs.dlq-url}")
    private String dlqUrl;

    public TokenLifecycleService(SqsClient sqsClient) {
        this.sqsClient = sqsClient;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Processes a token lifecycle update message from Cybersource.
     * Extracts required fields and sends to AWS SQS queue.
     * In case of failure, sends to DLQ.
     *
     * @param message the token lifecycle update message
     * @return message ID from SQS if successful
     * @throws RuntimeException if sending to both queue and DLQ fails
     */
    public String processTokenLifecycleUpdate(TokenLifecycleUpdateMessage message) {
        logger.info("Processing token lifecycle update for event type: {}", message.getEventType());

        try {
            // Extract required fields from the nested structure
            TokenLifecycleQueueMessage queueMessage = extractQueueMessage(message);
            
            // Convert to JSON
            String messageBody = objectMapper.writeValueAsString(queueMessage);
            logger.debug("Queue message body: {}", messageBody);

            // Send to SQS queue
            String messageId = sendToQueue(messageBody, queueUrl);
            logger.info("Successfully sent message to SQS queue. MessageId: {}", messageId);
            
            return messageId;
        } catch (Exception e) {
            logger.error("Failed to process token lifecycle update: {}", e.getMessage(), e);
            
            // Attempt to send to DLQ
            try {
                String dlqMessageBody = createDlqMessage(message, e);
                sendToQueue(dlqMessageBody, dlqUrl);
                logger.info("Message sent to DLQ due to processing failure");
            } catch (Exception dlqException) {
                logger.error("Failed to send message to DLQ: {}", dlqException.getMessage(), dlqException);
                throw new RuntimeException("Failed to process message and send to DLQ", dlqException);
            }
            
            throw new RuntimeException("Failed to process token lifecycle update, message sent to DLQ", e);
        }
    }

    /**
     * Extracts required fields from the token lifecycle update message.
     *
     * @param message the full token lifecycle update message
     * @return simplified queue message with only required fields
     */
    TokenLifecycleQueueMessage extractQueueMessage(TokenLifecycleUpdateMessage message) {
        var builder = TokenLifecycleQueueMessage.builder()
                .eventType(message.getEventType())
                .eventTimestamp(message.getEventTimestamp());

        // Extract token information
        if (message.getTokenInformation() != null) {
            builder.tokenId(message.getTokenInformation().getTokenId())
                    .tokenStatus(message.getTokenInformation().getTokenStatus())
                    .paymentAccountReference(message.getTokenInformation().getPaymentAccountReference());
        }

        // Extract instrument identifier
        if (message.getInstrumentIdentifier() != null) {
            builder.instrumentIdentifierId(message.getInstrumentIdentifier().getId());
        }

        // Extract organization information
        if (message.getOrganizationInformation() != null) {
            builder.organizationId(message.getOrganizationInformation().getOrganizationId());
        }

        return builder.build();
    }

    /**
     * Sends a message to the specified SQS queue.
     *
     * @param messageBody the message body to send
     * @param targetQueueUrl the URL of the target queue
     * @return the message ID from SQS
     * @throws SqsException if sending fails
     */
    private String sendToQueue(String messageBody, String targetQueueUrl) {
        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(targetQueueUrl)
                .messageBody(messageBody)
                .build();

        SendMessageResponse response = sqsClient.sendMessage(request);
        return response.messageId();
    }

    /**
     * Creates a DLQ message including the original message and error details.
     *
     * @param originalMessage the original message that failed processing
     * @param error the exception that caused the failure
     * @return JSON string for DLQ
     * @throws JsonProcessingException if JSON conversion fails
     */
    private String createDlqMessage(TokenLifecycleUpdateMessage originalMessage, Exception error) 
            throws JsonProcessingException {
        var dlqPayload = new java.util.HashMap<String, Object>();
        dlqPayload.put("originalMessage", originalMessage);
        dlqPayload.put("errorMessage", error.getMessage());
        dlqPayload.put("errorType", error.getClass().getSimpleName());
        dlqPayload.put("timestamp", java.time.Instant.now().toString());
        
        return objectMapper.writeValueAsString(dlqPayload);
    }
}
