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
 * Entity for auditing all token creation requests.
 * This is the FIRST table to save data.
 * Every request including duplicates creates an audit.
 * PaymentTokenId and InstrumentIdentifier are 1:1 mapping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "TokenAudits")
public class TokenAudit {
    
    @Id
    private String id;
    
    /**
     * TMS internal payment token ID.
     */
    @Indexed
    private String paymentTokenId;
    
    /**
     * CF internal merchant token registration ID.
     */
    @Indexed
    private String merchantTokenRegistrationId;
    
    /**
     * Cybersource instrument identifier ID.
     */
    @Indexed
    private String instrumentIdentifierId;
    
    /**
     * Card expiry month from request.
     */
    private String cardExpiryMonth;
    
    /**
     * Card expiry year from request.
     */
    private String cardExpiryYear;
    
    /**
     * Failure reason (only populated for failed requests).
     */
    private String failureReason;
    
    /**
     * Duplicate flag - only ONE record per instrumentIdentifierId should have false.
     * All subsequent requests with same instrumentIdentifierId should be marked as true.
     */
    @Indexed
    private Boolean isDuplicate;
    
    /**
     * Timestamp when the audit record was created.
     */
    @Indexed
    private LocalDateTime timestamp;
    
    /**
     * Flag indicating if the request was completed successfully.
     */
    private Boolean isRequestComplete;
    
    /**
     * External reference or transaction reference number from the request.
     */
    @Indexed
    private String externalReference;
    
    /**
     * Unique trace ID (UUID) generated at insert time for identifying the document.
     */
    @Indexed(unique = true)
    private String traceId;
}
