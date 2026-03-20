package com.example.cybersource.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for token event information returned in billing audit queries.
 * Contains essential information about token-related events.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenInfo {
    
    /**
     * Trace ID reference from the billing audit record.
     */
    private String traceId;
    
    /**
     * Transaction type - corresponds to traceIdEvent from BillingAudit.
     * Examples: "NetworkTokenProcessing", "Request Cryptogram", "TokenLifeCycleManagement"
     */
    private String transactionType;
    
    /**
     * Timestamp of the event.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    /**
     * Event status - true for success, false for failure.
     */
    private Boolean eventStatus;
}
