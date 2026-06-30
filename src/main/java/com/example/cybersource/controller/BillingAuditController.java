package com.example.cybersource.controller;

import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.dto.TokenInfo;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.service.BillingAuditQueryService;
import com.example.cybersource.service.BillingAuditSyncService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for billing audit synchronization and query operations.
 * Provides endpoints to sync billing data and query merchant and token event information.
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingAuditController {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingAuditController.class);
    
    private final BillingAuditSyncService billingAuditSyncService;
    private final BillingAuditQueryService billingAuditQueryService;
    
    public BillingAuditController(BillingAuditSyncService billingAuditSyncService,
                                  BillingAuditQueryService billingAuditQueryService) {
        this.billingAuditSyncService = billingAuditSyncService;
        this.billingAuditQueryService = billingAuditQueryService;
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
    
    /**
     * Gets a list of unique merchant token registration IDs for a given month and year.
     * Queries BillingAudit collection filtering by eventTimeStamp.
     *
     * @param month the month (1-12)
     * @param year the year (e.g., 2025)
     * @return list of distinct merchant token registration IDs
     */
    @GetMapping("/merchants/{month}/{year}")
    public ResponseEntity<List<String>> getMerchantListByMonthAndYear(
            @PathVariable int month,
            @PathVariable int year) {
        
        logger.info("Received request to get merchant list for month: {}, year: {}", month, year);
        
        try {
            List<String> merchantIds = billingAuditQueryService.getMerchantListByMonthAndYear(month, year);
            logger.info("Successfully retrieved {} merchants for month: {}, year: {}", 
                    merchantIds.size(), month, year);
            return ResponseEntity.ok(merchantIds);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid month or year: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Unexpected error while fetching merchant list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Gets a list of token events for a specific merchant and month/year.
     * Returns TokenInfo objects with event details from BillingAudit collection.
     *
     * @param merchantTokenRegistrationId the merchant token registration ID
     * @param month the month (1-12)
     * @param year the year (e.g., 2025)
     * @return list of TokenInfo objects containing event details
     */
    @GetMapping("/token-events/{merchantTokenRegistrationId}/{month}/{year}")
    public ResponseEntity<List<TokenInfo>> getTokenEventsByMerchant(
            @PathVariable String merchantTokenRegistrationId,
            @PathVariable int month,
            @PathVariable int year) {
        
        logger.info("Received request to get token events for merchant: {}, month: {}, year: {}",
                merchantTokenRegistrationId, month, year);
        
        try {
            List<TokenInfo> tokenEvents = billingAuditQueryService.getTokenEventsByMerchant(
                    merchantTokenRegistrationId, month, year);
            logger.info("Successfully retrieved {} token events for merchant: {}, month: {}, year: {}",
                    tokenEvents.size(), merchantTokenRegistrationId, month, year);
            return ResponseEntity.ok(tokenEvents);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("Unexpected error while fetching token events", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
