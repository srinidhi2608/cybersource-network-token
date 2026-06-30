package com.example.cybersource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing token lifecycle update message from Cybersource Token Management Service.
 * This is a nested structure containing information about token updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenLifecycleUpdateMessage {
    
    /**
     * Token information containing token ID and status.
     */
    private TokenInfo tokenInformation;
    
    /**
     * Instrument identifier information.
     */
    private InstrumentIdentifierInfo instrumentIdentifier;
    
    /**
     * Organization information containing merchant details.
     */
    private OrganizationInfo organizationInformation;
    
    /**
     * Event type (e.g., TOKEN_CREATED, TOKEN_UPDATED, TOKEN_SUSPENDED, TOKEN_DELETED).
     */
    private String eventType;
    
    /**
     * Timestamp of the event.
     */
    private String eventTimestamp;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TokenInfo {
        /**
         * Unique token identifier from Cybersource.
         */
        private String tokenId;
        
        /**
         * Current status of the token.
         */
        private String tokenStatus;
        
        /**
         * Token expiration month.
         */
        private String expirationMonth;
        
        /**
         * Token expiration year.
         */
        private String expirationYear;
        
        /**
         * Payment Account Reference (PAR).
         */
        private String paymentAccountReference;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InstrumentIdentifierInfo {
        /**
         * Instrument identifier ID.
         */
        private String id;
        
        /**
         * State of the instrument identifier.
         */
        private String state;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrganizationInfo {
        /**
         * Organization/Merchant ID.
         */
        private String organizationId;
        
        /**
         * Merchant name.
         */
        private String merchantName;
    }
}
