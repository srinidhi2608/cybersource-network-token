package com.example.cybersource.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity representing token lifecycle events received from Cybersource webhook.
 * Stores all lifecycle event notifications for audit and tracking purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "TokenEvents")
public class TokenEvents {
    
    @Id
    private String id;
    
    /**
     * Unique trace ID for this event.
     */
    @Indexed
    private String traceId;
    
    /**
     * TMS internal payment token ID.
     */
    @Indexed
    private String paymentTokenId;
    
    /**
     * Token expiry month (MM format).
     */
    private String tokenExpiryMonth;
    
    /**
     * Token expiry year (YYYY format).
     */
    private String tokenExpiryYear;
    
    /**
     * Last 4 digits of the card number.
     */
    private String cardSuffix;
    
    /**
     * Card expiry month (MM format).
     */
    private String cardExpiryMonth;
    
    /**
     * Card expiry year (YYYY format).
     */
    private String cardExpiryYear;
    
    /**
     * Current status of the token (e.g., ACTIVE, SUSPENDED, DELETED).
     */
    private String tokenStatus;
    
    /**
     * Name of the lifecycle event (e.g., TOKEN_UPDATED, TOKEN_SUSPENDED).
     */
    private String lifecycleEventName;
    
    /**
     * Timestamp when the event occurred.
     */
    @Indexed
    private LocalDateTime timestamp;
    
    /**
     * Timestamp when the merchant was updated.
     */
    private LocalDateTime merchantUpdateTimestamp;
}
