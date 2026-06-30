package com.example.cybersource.entity;

import com.example.cybersource.enums.EventType;
import com.example.cybersource.enums.TraceIdEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity representing consolidated billing audit records.
 * This collection aggregates data from TokenAudits, FetchInformationAudits, and TokenEvents
 * for billing and reporting purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "BillingAudit")
@CompoundIndexes({
    @CompoundIndex(name = "billing_batch_idx", def = "{'billingBatchNumber': 1, 'timestamp': -1}"),
    @CompoundIndex(name = "merchant_event_idx", def = "{'merchantTokenRegistrationId': 1, 'traceIdEvent': 1, 'timestamp': -1}")
})
public class BillingAudit {
    
    @Id
    private String id;
    
    /**
     * Reference to the trace ID from the source collection (TokenAudit, FetchInformationAudit, or TokenEvents).
     */
    @Indexed
    private String traceIdReference;
    
    /**
     * Type of event: "NetworkTokenProcessing", "Request Cryptogram", or "TokenLifeCycleManagement".
     */
    @Indexed
    private String traceIdEvent;
    
    /**
     * Timestamp of the original event from the source collection.
     */
    @Indexed
    private LocalDateTime eventTimeStamp;
    
    /**
     * Type of event result: "Success" or "Failure".
     */
    @Indexed
    private String eventType;
    
    /**
     * CF internal merchant token registration ID.
     */
    @Indexed
    private String merchantTokenRegistrationId;
    
    /**
     * Unique billing reference number generated for this record.
     * Format: BILL-{timestamp}-{sequence}
     * Unique across all threads and containers.
     */
    @Indexed(unique = true)
    private String billingReferenceNumber;
    
    /**
     * Billing batch number - same for all records in a single sync operation.
     * UUID generated once per API call.
     */
    @Indexed
    private String billingBatchNumber;
    
    /**
     * Timestamp when this billing audit record was created.
     */
    @Indexed
    private LocalDateTime timestamp;
    
    /**
     * TMS internal payment token ID (optional, may not be present in all source records).
     */
    private String paymentTokenId;
    
    /**
     * External reference or transaction reference number from the source record (optional).
     */
    private String externalReference;
    
    /**
     * Additional context or notes about this billing record (optional).
     */
    private String notes;
}
