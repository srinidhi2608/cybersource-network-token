package com.example.cybersource.controller;

import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.service.BillingAuditSyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for billing audit synchronization operations.
 * Provides endpoint to trigger sync of billing data from source collections.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingAuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingAuditController.class);
    
    private final BillingAuditSyncService billingAuditSyncService;
    
    public BillingAuditController(BillingAuditSyncService billingAuditSyncService) {
        this.billingAuditSyncService = billingAuditSyncService;
    }
    
    /**
     * Synchronizes billing audit data from TokenAudits, FetchInformationAudits, and TokenEvents
     * into the BillingAudit collection.
     * 
     * Fetches data from lastSyncTimestamp to current time minus offset minutes.
     * Generates unique billing reference numbers for each record.
     * Uses a single billing batch number for all records in the sync operation.
     *
     * @param request optional request parameters to override default sync behavior
     * @return response containing sync statistics and results
     */
    @PostMapping("/audits/sync")
    public ResponseEntity<BillingAuditSyncResponse> syncBillingAudit(
            @Valid @RequestBody(required = false) BillingAuditSyncRequest request) {
        
        logger.info("Received billing audit sync request");
        
        try {
            BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(request);
            logger.info("Billing audit sync completed successfully. Batch: {}, Records: {}",
                    response.getBillingBatchNumber(), response.getTotalRecordsCreated());
            return ResponseEntity.ok(response);
            
        } catch (CybersourceException e) {
            logger.error("Billing audit sync failed: {}", e.getMessage());
            BillingAuditSyncResponse errorResponse = BillingAuditSyncResponse.builder()
                    .success(false)
                    .message("Billing audit sync failed")
                    .errorMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            
        } catch (Exception e) {
            logger.error("Unexpected error during billing audit sync", e);
            BillingAuditSyncResponse errorResponse = BillingAuditSyncResponse.builder()
                    .success(false)
                    .message("Unexpected error during billing audit sync")
                    .errorMessage(e.getMessage())
                    .build();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Health check endpoint for billing audit sync service.
     *
     * @return simple status response
     */
    @GetMapping("/audits/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Billing Audit Sync Service is running");
    }
}
