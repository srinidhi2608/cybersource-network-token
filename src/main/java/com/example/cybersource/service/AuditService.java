package com.example.cybersource.service;

import com.example.cybersource.dto.UpdateExternalReferenceRequest;
import com.example.cybersource.dto.UpdateExternalReferenceResponse;
import com.example.cybersource.entity.FetchInformationAudit;
import com.example.cybersource.entity.TokenAudit;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.FetchInformationAuditRepository;
import com.example.cybersource.repository.TokenAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing audit records including updating external references.
 */
@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final TokenAuditRepository tokenAuditRepository;
    private final FetchInformationAuditRepository fetchInformationAuditRepository;

    public AuditService(TokenAuditRepository tokenAuditRepository, 
                       FetchInformationAuditRepository fetchInformationAuditRepository) {
        this.tokenAuditRepository = tokenAuditRepository;
        this.fetchInformationAuditRepository = fetchInformationAuditRepository;
    }

    /**
     * Updates the external reference in a TokenAudit record identified by traceId.
     *
     * @param request the update request containing traceId and new externalReference
     * @return response containing the update result
     * @throws CybersourceException if the record is not found
     */
    @Transactional
    public UpdateExternalReferenceResponse updateTokenAuditExternalReference(UpdateExternalReferenceRequest request) 
            throws CybersourceException {
        logger.info("Updating TokenAudit externalReference for traceId: {}", request.getTraceId());

        TokenAudit tokenAudit = tokenAuditRepository.findByTraceId(request.getTraceId())
                .orElseThrow(() -> new CybersourceException(
                        "TokenAudit not found for traceId: " + request.getTraceId()));

        String oldExternalReference = tokenAudit.getExternalReference();
        tokenAudit.setExternalReference(request.getExternalReference());
        tokenAuditRepository.save(tokenAudit);

        logger.info("Successfully updated TokenAudit externalReference from '{}' to '{}' for traceId: {}", 
                oldExternalReference, request.getExternalReference(), request.getTraceId());

        return UpdateExternalReferenceResponse.builder()
                .traceId(request.getTraceId())
                .externalReference(request.getExternalReference())
                .auditType("TokenAudit")
                .success(true)
                .message("External reference updated successfully")
                .build();
    }

    /**
     * Updates the external reference in a FetchInformationAudit record identified by traceId.
     *
     * @param request the update request containing traceId and new externalReference
     * @return response containing the update result
     * @throws CybersourceException if the record is not found
     */
    @Transactional
    public UpdateExternalReferenceResponse updateFetchInformationAuditExternalReference(UpdateExternalReferenceRequest request) 
            throws CybersourceException {
        logger.info("Updating FetchInformationAudit externalReference for traceId: {}", request.getTraceId());

        FetchInformationAudit fetchAudit = fetchInformationAuditRepository.findByTraceId(request.getTraceId())
                .orElseThrow(() -> new CybersourceException(
                        "FetchInformationAudit not found for traceId: " + request.getTraceId()));

        String oldExternalReference = fetchAudit.getExternalReference();
        fetchAudit.setExternalReference(request.getExternalReference());
        fetchInformationAuditRepository.save(fetchAudit);

        logger.info("Successfully updated FetchInformationAudit externalReference from '{}' to '{}' for traceId: {}", 
                oldExternalReference, request.getExternalReference(), request.getTraceId());

        return UpdateExternalReferenceResponse.builder()
                .traceId(request.getTraceId())
                .externalReference(request.getExternalReference())
                .auditType("FetchInformationAudit")
                .success(true)
                .message("External reference updated successfully")
                .build();
    }
}
