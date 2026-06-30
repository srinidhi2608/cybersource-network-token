package com.example.cybersource.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Request DTO for billing audit sync operation.
 * Allows optional override of sync parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAuditSyncRequest {
    
    /**
     * Optional: Override the start time for sync.
     * If not provided, uses lastSyncTimestamp from BillingAuditSyncInformation.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * Optional: Override the end time for sync.
     * If not provided, uses current time minus offset minutes.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * Optional: Force sync even if another sync is in progress.
     * Default is false.
     */
    private Boolean forceSync;
    
    /**
     * Optional: Batch size for processing records.
     * If not provided, uses configured default.
     */
    private Integer batchSize;
}
