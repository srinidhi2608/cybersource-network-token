package com.example.cybersource.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating external reference in audit records.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExternalReferenceRequest {
    
    /**
     * Unique trace ID to identify the audit record.
     */
    @NotBlank(message = "traceId is required")
    private String traceId;
    
    /**
     * New external reference value to set.
     */
    @NotBlank(message = "externalReference is required")
    private String externalReference;
}
