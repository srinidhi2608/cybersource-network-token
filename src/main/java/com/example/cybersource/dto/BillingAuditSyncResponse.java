package com.example.cybersource.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for billing audit sync operation.
 * Contains summary statistics and details about the sync operation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAuditSyncResponse {
    
    /**
     * Indicates if the sync operation was successful.
     */
    private boolean success;
    
    /**
     * Message describing the sync result.
     */
    private String message;
    
    /**
     * Billing batch number for this sync operation.
     * All records created in this sync share this batch number.
     */
    private String billingBatchNumber;
    
    /**
     * Start time of the sync window.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime syncStartTime;
    
    /**
     * End time of the sync window.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime syncEndTime;
    
    /**
     * Timestamp when the sync operation started.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime operationStartedAt;
    
    /**
     * Timestamp when the sync operation completed.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime operationCompletedAt;
    
    /**
     * Total number of TokenAudit records processed.
     */
    private long tokenAuditCount;
    
    /**
     * Total number of FetchInformationAudit records processed.
     */
    private long fetchInfoAuditCount;
    
    /**
     * Total number of TokenEvents records processed.
     */
    private long tokenEventsCount;
    
    /**
     * Total number of BillingAudit records created.
     */
    private long totalRecordsCreated;
    
    /**
     * List of sample billing reference numbers created (first 10).
     */
    private List<String> sampleBillingReferenceNumbers;
    
    /**
     * Error message if the sync failed.
     */
    private String errorMessage;
}
