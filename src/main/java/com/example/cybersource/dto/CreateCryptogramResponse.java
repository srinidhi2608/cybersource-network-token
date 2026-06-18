package com.example.cybersource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for creating a cryptogram.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCryptogramResponse {
    
    /**
     * Cryptogram for the transaction.
     */
    private String cryptogram;
    
    /**
     * TMS internal payment token ID.
     */
    private String paymentTokenId;
}
