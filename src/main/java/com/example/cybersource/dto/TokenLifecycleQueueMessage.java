package com.example.cybersource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simplified DTO containing only the required fields extracted from TokenLifecycleUpdateMessage
 * to be placed on the AWS SQS queue.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenLifecycleQueueMessage {
    
    /**
     * Token ID from Cybersource.
     */
    private String tokenId;
    
    /**
     * Instrument identifier ID.
     */
    private String instrumentIdentifierId;
    
    /**
     * Organization/Merchant ID.
     */
    private String organizationId;
    
    /**
     * Current status of the token.
     */
    private String tokenStatus;
    
    /**
     * Event type (e.g., TOKEN_CREATED, TOKEN_UPDATED, TOKEN_SUSPENDED, TOKEN_DELETED).
     */
    private String eventType;
    
    /**
     * Timestamp of the event.
     */
    private String eventTimestamp;
    
    /**
     * Payment Account Reference (PAR).
     */
    private String paymentAccountReference;
}
