package com.example.cybersource.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a cryptogram.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCryptogramRequest {
    
    /**
     * TMS internal payment token ID (UUID).
     */
    @NotBlank(message = "paymentTokenId is required")
    private String paymentTokenId;
    
    /**
     * External reference or transaction reference number.
     */
    @NotBlank(message = "externalReference is required")
    private String externalReference;
}
