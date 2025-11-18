package com.example.cybersource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for creating a network token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNetworkTokenResponse {
    
    /**
     * TMS internal payment token ID (UUID).
     */
    private String paymentTokenId;
    
    /**
     * Network token number.
     */
    private String networkToken;
    
    /**
     * Cryptogram for the transaction.
     */
    private String cryptogram;
    
    /**
     * Payment Account Reference.
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
     * Token status.
     */
    private String tokenStatus;
    
    /**
     * Cybersource instrument identifier ID.
     */
    private String instrumentIdentifierId;
}
