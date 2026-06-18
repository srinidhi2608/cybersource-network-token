package com.example.cybersource.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Entity for tracking the last successful sync timestamp for billing audit.
 * This collection helps maintain incremental sync state to avoid reprocessing data.
 * Uses optimistic locking to handle concurrent sync requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "BillingAuditSyncInformation")
public class BillingAuditSyncInformation {
    
    /**
     * Singleton document ID. Only one record should exist in this collection.
     */
    @Id
    private String id;
    
    /**
     * Last successful sync timestamp.
     * Next sync will fetch data from this timestamp onwards.
     */
    private LocalDateTime lastSyncTimestamp;
    
    /**
     * Timestamp when the last sync was initiated.
     */
    private LocalDateTime lastSyncStartedAt;
    
    /**
     * Timestamp when the last sync was completed.
     */
    private LocalDateTime lastSyncCompletedAt;
    
    /**
     * Number of records synced in the last operation.
     */
    private Long lastSyncRecordCount;
    
    /**
     * Status of the last sync (SUCCESS, FAILURE, IN_PROGRESS).
     */
    private String lastSyncStatus;
    
    /**
     * Version field for optimistic locking to handle concurrent updates.
     */
    @Version
    private Long version;
    
    /**
     * Timestamp when this document was created.
     */
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this document was last updated.
     */
    private LocalDateTime updatedAt;
}
