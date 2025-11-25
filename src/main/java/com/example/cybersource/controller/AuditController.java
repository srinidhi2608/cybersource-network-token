package com.example.cybersource.controller;

import com.example.cybersource.dto.UpdateExternalReferenceRequest;
import com.example.cybersource.dto.UpdateExternalReferenceResponse;
import com.example.cybersource.service.AuditService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for audit management operations.
 */
@RestController
@RequestMapping("/api/v1/audits")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Updates the external reference for a TokenAudit record.
     *
     * @param request the update request containing traceId and externalReference
     * @return the update response
     */
    @PutMapping("/token-audits/external-reference")
    public ResponseEntity<UpdateExternalReferenceResponse> updateTokenAuditExternalReference(
            @Valid @RequestBody UpdateExternalReferenceRequest request) throws Exception {
        
        logger.info("Received request to update TokenAudit externalReference for traceId: {}", 
                request.getTraceId());
        
        UpdateExternalReferenceResponse response = auditService.updateTokenAuditExternalReference(request);
        
        logger.info("Successfully updated TokenAudit externalReference for traceId: {}", 
                request.getTraceId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Updates the external reference for a FetchInformationAudit record.
     *
     * @param request the update request containing traceId and externalReference
     * @return the update response
     */
    @PutMapping("/fetch-information-audits/external-reference")
    public ResponseEntity<UpdateExternalReferenceResponse> updateFetchInformationAuditExternalReference(
            @Valid @RequestBody UpdateExternalReferenceRequest request) throws Exception {
        
        logger.info("Received request to update FetchInformationAudit externalReference for traceId: {}", 
                request.getTraceId());
        
        UpdateExternalReferenceResponse response = auditService.updateFetchInformationAuditExternalReference(request);
        
        logger.info("Successfully updated FetchInformationAudit externalReference for traceId: {}", 
                request.getTraceId());
        
        return ResponseEntity.ok(response);
    }
}
