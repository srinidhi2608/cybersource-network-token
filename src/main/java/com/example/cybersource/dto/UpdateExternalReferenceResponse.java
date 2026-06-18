package com.example.cybersource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for updating external reference in audit records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExternalReferenceResponse {
    
    /**
     * Trace ID of the updated record.
     */
    private String traceId;
    
    /**
     * Updated external reference value.
     */
    private String externalReference;
    
    /**
     * Type of audit record updated ("TokenAudit" or "FetchInformationAudit").
     */
    private String auditType;
    
    /**
     * Flag indicating if the update was successful.
     */
    private boolean success;
    
    /**
     * Message providing details about the operation.
     */
    private String message;
}
