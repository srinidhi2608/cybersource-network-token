package com.example.cybersource.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Standard error response DTO for API errors.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    
    /**
     * Timestamp when the error occurred.
     */
    private LocalDateTime timestamp;
    
    /**
     * HTTP status code.
     */
    private int status;
    
    /**
     * Error type/category.
     */
    private String error;
    
    /**
     * Error message.
     */
    private String message;
    
    /**
     * Additional error details.
     */
    private Map<String, Object> details;
}
