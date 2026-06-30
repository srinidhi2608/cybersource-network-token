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
 * Entity representing the latest state of network tokens.
 * This collection only contains the LATEST state of tokens.
 * Duplicate calls should NOT create new records here.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "TokenTransactions")
public class TokenTransaction {
    
    @Id
    private String id;
    
    /**
     * TMS internal payment token ID (UUID).
     */
    @Indexed(unique = true)
    private String paymentTokenId;
    
    /**
     * CF internal merchant token registration ID.
     */
    @Indexed
    private String merchantTokenRegistrationId;
    
    /**
     * Encrypted network token (Base64 encoded).
     */
    private String networkToken;
    
    /**
     * Cybersource instrument identifier ID.
     */
    @Indexed(unique = true)
    private String instrumentIdentifierId;
    
    /**
     * Payment Account Reference from Cybersource.
     */
    private String par;
    
    /**
     * Token expiry month (MM format).
     */
    private String tokenExpiryMonth;
    
    /**
     * Token expiry year (YYYY format).
     */
    private String tokenExpiryYear;
    
    /**
     * Token status (e.g., ACTIVE, SUSPENDED, DELETED).
     */
    private String tokenStatus;
    
    /**
     * Timestamp when this token record was created/updated.
     */
    private LocalDateTime timestamp;
}
