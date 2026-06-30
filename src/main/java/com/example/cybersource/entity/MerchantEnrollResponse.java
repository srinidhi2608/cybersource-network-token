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
 * Entity representing merchant enrollment information.
 * This collection stores manually enrolled merchants required for createInstrumentIdentifier calls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "MerchantEnrollResponse")
public class MerchantEnrollResponse {
    
    @Id
    private String id;
    
    /**
     * Unique merchant token registration ID (CF internal).
     */
    @Indexed(unique = true)
    private String merchantTokenRegistrationId;
    
    /**
     * Transacting organization ID - used as merchantId for Cybersource API calls.
     */
    private String transactingOrgId;
    
    /**
     * Merchant name for reference.
     */
    private String merchantName;
    
    /**
     * Merchant enrollment status.
     */
    private MerchantStatus status;
    
    /**
     * Timestamp when merchant was enrolled.
     */
    private LocalDateTime enrolledAt;
    
    /**
     * Timestamp when merchant record was last updated.
     */
    private LocalDateTime updatedAt;
    
    /**
     * Merchant status enumeration.
     */
    public enum MerchantStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
