package com.example.cybersource.service;

import com.example.cybersource.constants.BillingConstants;
import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.entity.*;
import com.example.cybersource.enums.EventReference;
import com.example.cybersource.enums.EventType;
import com.example.cybersource.enums.TraceIdEventType;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.*;
import com.example.cybersource.util.BillingReferenceNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for synchronizing billing audit data from various source collections.
 * Aggregates data from TokenAudits, FetchInformationAudits, and TokenEvents into BillingAudit collection.
 * Implements thread-safe operations with optimistic locking and atomic sequence generation.
 */
@Service
public class BillingAuditSyncService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingAuditSyncService.class);
    
    private final TokenAuditRepository tokenAuditRepository;
    private final FetchInformationAuditRepository fetchInformationAuditRepository;
    private final TokenEventsRepository tokenEventsRepository;
    private final BillingAuditRepository billingAuditRepository;
    private final BillingAuditSyncInformationRepository syncInformationRepository;
    private final BillingReferenceNumberGenerator referenceNumberGenerator;
    
    @Value("${" + BillingConstants.CONFIG_SYNC_OFFSET_MINUTES + ":" + BillingConstants.DEFAULT_SYNC_OFFSET_MINUTES + "}")
    private int syncOffsetMinutes;
    
    @Value("${" + BillingConstants.CONFIG_SYNC_BATCH_SIZE + ":" + BillingConstants.DEFAULT_SYNC_BATCH_SIZE + "}")
    private int batchSize;
    
    public BillingAuditSyncService(
            TokenAuditRepository tokenAuditRepository,
            FetchInformationAuditRepository fetchInformationAuditRepository,
            TokenEventsRepository tokenEventsRepository,
            BillingAuditRepository billingAuditRepository,
            BillingAuditSyncInformationRepository syncInformationRepository,
            BillingReferenceNumberGenerator referenceNumberGenerator) {
        this.tokenAuditRepository = tokenAuditRepository;
        this.fetchInformationAuditRepository = fetchInformationAuditRepository;
        this.tokenEventsRepository = tokenEventsRepository;
        this.billingAuditRepository = billingAuditRepository;
        this.syncInformationRepository = syncInformationRepository;
        this.referenceNumberGenerator = referenceNumberGenerator;
    }
    
    /**
     * Synchronizes billing audit data from source collections to BillingAudit collection.
     * 
     * @param request optional request parameters to override default sync behavior
     * @return response containing sync statistics and results
     * @throws CybersourceException if sync operation fails
     */
    @Transactional
    public BillingAuditSyncResponse syncBillingAudit(BillingAuditSyncRequest request) throws CybersourceException {
        logger.info("Starting billing audit sync operation");
        LocalDateTime operationStartTime = LocalDateTime.now();
        
        try {
            // Generate unique batch number for this sync operation
            String batchNumber = UUID.randomUUID().toString();
            logger.info("Billing batch number: {}", batchNumber);
            
            // Determine sync time window
            SyncWindow syncWindow = determineSyncWindow(request);
            logger.info("Sync window: {} to {}", syncWindow.getStartTime(), syncWindow.getEndTime());
            
            // Update sync information status to IN_PROGRESS
            BillingAuditSyncInformation syncInfo = updateSyncStatus(syncWindow, "IN_PROGRESS", operationStartTime);
            
            // Fetch data from source collections
            List<TokenAudit> tokenAudits = fetchTokenAudits(syncWindow);
            List<FetchInformationAudit> fetchInfoAudits = fetchFetchInformationAudits(syncWindow);
            List<TokenEvents> tokenEvents = fetchTokenEvents(syncWindow);
            
            logger.info("Fetched {} TokenAudits, {} FetchInformationAudits, {} TokenEvents",
                    tokenAudits.size(), fetchInfoAudits.size(), tokenEvents.size());
            
            // Transform and create billing audit records
            List<BillingAudit> billingAudits = new ArrayList<>();
            List<String> sampleReferenceNumbers = new ArrayList<>();
            
            // Process TokenAudits
            for (TokenAudit tokenAudit : tokenAudits) {
                BillingAudit billingAudit = createBillingAuditFromTokenAudit(tokenAudit, batchNumber);
                billingAudits.add(billingAudit);
                if (sampleReferenceNumbers.size() < 10) {
                    sampleReferenceNumbers.add(billingAudit.getBillingReferenceNumber());
                }
            }
            
            // Process FetchInformationAudits
            for (FetchInformationAudit fetchInfoAudit : fetchInfoAudits) {
                BillingAudit billingAudit = createBillingAuditFromFetchInfoAudit(fetchInfoAudit, batchNumber);
                billingAudits.add(billingAudit);
                if (sampleReferenceNumbers.size() < 10) {
                    sampleReferenceNumbers.add(billingAudit.getBillingReferenceNumber());
                }
            }
            
            // Process TokenEvents (if needed for lifecycle management tracking)
            for (TokenEvents tokenEvent : tokenEvents) {
                BillingAudit billingAudit = createBillingAuditFromTokenEvent(tokenEvent, batchNumber);
                billingAudits.add(billingAudit);
                if (sampleReferenceNumbers.size() < 10) {
                    sampleReferenceNumbers.add(billingAudit.getBillingReferenceNumber());
                }
            }
            
            // Batch insert billing audit records
            if (!billingAudits.isEmpty()) {
                billingAuditRepository.saveAll(billingAudits);
                logger.info("Saved {} billing audit records", billingAudits.size());
            }
            
            // Update sync information with success status
            LocalDateTime operationEndTime = LocalDateTime.now();
            syncInfo = updateSyncCompletion(syncInfo, syncWindow.getEndTime(), billingAudits.size(), "SUCCESS", operationEndTime);
            
            // Build response
            return BillingAuditSyncResponse.builder()
                    .success(true)
                    .message("Billing audit sync completed successfully")
                    .billingBatchNumber(batchNumber)
                    .syncStartTime(syncWindow.getStartTime())
                    .syncEndTime(syncWindow.getEndTime())
                    .operationStartedAt(operationStartTime)
                    .operationCompletedAt(operationEndTime)
                    .tokenAuditCount(tokenAudits.size())
                    .fetchInfoAuditCount(fetchInfoAudits.size())
                    .tokenEventsCount(tokenEvents.size())
                    .totalRecordsCreated(billingAudits.size())
                    .sampleBillingReferenceNumbers(sampleReferenceNumbers)
                    .build();
                    
        } catch (OptimisticLockingFailureException e) {
            logger.error("Concurrent sync operation detected", e);
            throw new CybersourceException("Another sync operation is in progress. Please try again later.");
        } catch (Exception e) {
            logger.error("Billing audit sync failed", e);
            
            // Update sync information with failure status
            try {
                BillingAuditSyncInformation syncInfo = syncInformationRepository.findSyncInfo();
                if (syncInfo != null) {
                    syncInfo.setLastSyncStatus("FAILURE");
                    syncInfo.setUpdatedAt(LocalDateTime.now());
                    syncInformationRepository.save(syncInfo);
                }
            } catch (Exception ex) {
                logger.error("Failed to update sync status to FAILURE", ex);
            }
            
            throw new CybersourceException("Billing audit sync failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines the sync time window based on request parameters or default configuration.
     */
    private SyncWindow determineSyncWindow(BillingAuditSyncRequest request) {
        LocalDateTime startTime;
        LocalDateTime endTime;
        
        if (request != null && request.getStartTime() != null) {
            startTime = request.getStartTime();
        } else {
            // Get last sync timestamp from sync information
            BillingAuditSyncInformation syncInfo = syncInformationRepository.findSyncInfo();
            if (syncInfo != null && syncInfo.getLastSyncTimestamp() != null) {
                startTime = syncInfo.getLastSyncTimestamp();
            } else {
                // First time sync - start from 24 hours ago
                startTime = LocalDateTime.now().minusHours(24);
                logger.info("First time sync, starting from 24 hours ago: {}", startTime);
            }
        }
        
        if (request != null && request.getEndTime() != null) {
            endTime = request.getEndTime();
        } else {
            // Default: current time minus offset minutes (to allow for database replication lag)
            endTime = LocalDateTime.now().minusMinutes(syncOffsetMinutes);
        }
        
        return new SyncWindow(startTime, endTime);
    }
    
    /**
     * Fetches TokenAudit records within the sync window.
     */
    private List<TokenAudit> fetchTokenAudits(SyncWindow window) {
        return tokenAuditRepository.findByTimestampBetween(window.getStartTime(), window.getEndTime());
    }
    
    /**
     * Fetches FetchInformationAudit records within the sync window.
     */
    private List<FetchInformationAudit> fetchFetchInformationAudits(SyncWindow window) {
        return fetchInformationAuditRepository.findByTimestampBetween(window.getStartTime(), window.getEndTime());
    }
    
    /**
     * Fetches TokenEvents records within the sync window.
     */
    private List<TokenEvents> fetchTokenEvents(SyncWindow window) {
        return tokenEventsRepository.findByTimestampBetween(window.getStartTime(), window.getEndTime());
    }
    
    /**
     * Creates a BillingAudit record from a TokenAudit record.
     * TraceIdEvent = "NetworkTokenProcessing"
     */
    private BillingAudit createBillingAuditFromTokenAudit(TokenAudit tokenAudit, String batchNumber) {
        EventType eventType = determineEventType(tokenAudit.getIsRequestComplete(), tokenAudit.getFailureReason());
        
        return BillingAudit.builder()
                .traceIdReference(tokenAudit.getTraceId())
                .traceIdEvent(TraceIdEventType.NETWORK_TOKEN_PROCESSING.getDisplayName())
                .eventTimeStamp(tokenAudit.getTimestamp())
                .eventType(eventType.getDisplayName())
                .merchantTokenRegistrationId(tokenAudit.getMerchantTokenRegistrationId())
                .billingReferenceNumber(referenceNumberGenerator.generateUniqueReferenceNumber())
                .billingBatchNumber(batchNumber)
                .timestamp(LocalDateTime.now())
                .paymentTokenId(tokenAudit.getPaymentTokenId())
                .externalReference(tokenAudit.getExternalReference())
                .build();
    }
    
    /**
     * Creates a BillingAudit record from a FetchInformationAudit record.
     * TraceIdEvent = "Request Cryptogram" for FetchCryptogram or LCM event references
     */
    private BillingAudit createBillingAuditFromFetchInfoAudit(FetchInformationAudit fetchInfoAudit, String batchNumber) {
        EventType eventType = determineEventType(fetchInfoAudit.getIsRequestComplete(), null);
        
        // Determine trace ID event based on event reference
        String traceIdEvent;
        EventReference eventRef = EventReference.fromValue(fetchInfoAudit.getEventReference());
        
        if (eventRef == EventReference.FETCH_CRYPTOGRAM || eventRef == EventReference.LCM) {
            traceIdEvent = TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName();
        } else {
            // Default to REQUEST_CRYPTOGRAM for FetchInformationAudits
            traceIdEvent = TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName();
        }
        
        return BillingAudit.builder()
                .traceIdReference(fetchInfoAudit.getTraceId())
                .traceIdEvent(traceIdEvent)
                .eventTimeStamp(fetchInfoAudit.getTimestamp())
                .eventType(eventType.getDisplayName())
                .billingReferenceNumber(referenceNumberGenerator.generateUniqueReferenceNumber())
                .billingBatchNumber(batchNumber)
                .timestamp(LocalDateTime.now())
                .paymentTokenId(fetchInfoAudit.getPaymentTokenId())
                .externalReference(fetchInfoAudit.getExternalReference())
                .notes("InformationType: " + fetchInfoAudit.getInformationType() +
                       ", EventReference: " + fetchInfoAudit.getEventReference())
                .build();
    }
    
    /**
     * Creates a BillingAudit record from a TokenEvents record.
     * TraceIdEvent = "TokenLifeCycleManagement"
     */
    private BillingAudit createBillingAuditFromTokenEvent(TokenEvents tokenEvent, String batchNumber) {
        // Token events are generally successful lifecycle notifications
        EventType eventType = EventType.SUCCESS;
        
        return BillingAudit.builder()
                .traceIdReference(tokenEvent.getTraceId())
                .traceIdEvent(TraceIdEventType.TOKEN_LIFECYCLE_MANAGEMENT.getDisplayName())
                .eventTimeStamp(tokenEvent.getTimestamp())
                .eventType(eventType.getDisplayName())
                .billingReferenceNumber(referenceNumberGenerator.generateUniqueReferenceNumber())
                .billingBatchNumber(batchNumber)
                .timestamp(LocalDateTime.now())
                .paymentTokenId(tokenEvent.getPaymentTokenId())
                .notes("LifecycleEvent: " + tokenEvent.getLifecycleEventName() +
                       ", TokenStatus: " + tokenEvent.getTokenStatus())
                .build();
    }
    
    /**
     * Determines the event type based on request completion status and failure reason.
     */
    private EventType determineEventType(Boolean isRequestComplete, String failureReason) {
        if (Boolean.TRUE.equals(isRequestComplete) && (failureReason == null || failureReason.isBlank())) {
            return EventType.SUCCESS;
        } else {
            return EventType.FAILURE;
        }
    }
    
    /**
     * Updates sync information status at the start of sync operation.
     */
    private BillingAuditSyncInformation updateSyncStatus(SyncWindow window, String status, LocalDateTime startTime) {
        BillingAuditSyncInformation syncInfo = syncInformationRepository.findSyncInfo();
        
        if (syncInfo == null) {
            // Create new sync information record
            syncInfo = BillingAuditSyncInformation.builder()
                    .lastSyncTimestamp(window.getStartTime())
                    .lastSyncStartedAt(startTime)
                    .lastSyncStatus(status)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
        } else {
            syncInfo.setLastSyncStartedAt(startTime);
            syncInfo.setLastSyncStatus(status);
            syncInfo.setUpdatedAt(LocalDateTime.now());
        }
        
        return syncInformationRepository.save(syncInfo);
    }
    
    /**
     * Updates sync information after successful completion.
     */
    private BillingAuditSyncInformation updateSyncCompletion(
            BillingAuditSyncInformation syncInfo,
            LocalDateTime newSyncTimestamp,
            long recordCount,
            String status,
            LocalDateTime completionTime) {
        
        syncInfo.setLastSyncTimestamp(newSyncTimestamp);
        syncInfo.setLastSyncCompletedAt(completionTime);
        syncInfo.setLastSyncRecordCount(recordCount);
        syncInfo.setLastSyncStatus(status);
        syncInfo.setUpdatedAt(LocalDateTime.now());
        
        return syncInformationRepository.save(syncInfo);
    }
    
    /**
     * Inner class representing a sync time window.
     */
    private static class SyncWindow {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        
        public SyncWindow(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
}
