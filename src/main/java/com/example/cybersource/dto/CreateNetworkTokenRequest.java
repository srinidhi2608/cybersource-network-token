package com.example.cybersource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a network token.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNetworkTokenRequest {
    
    /**
     * Merchant token registration ID (CF internal).
     */
    @NotBlank(message = "merchantTokenRegistrationId is required")
    private String merchantTokenRegistrationId;
    
    /**
     * Card number (PAN).
     */
    @NotBlank(message = "cardNumber is required")
    @Pattern(regexp = "^[0-9]{13,19}$", message = "cardNumber must be 13-19 digits")
    private String cardNumber;
    
    /**
     * Card expiry month (MM format).
     */
    @NotBlank(message = "cardExpiryMonth is required")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "cardExpiryMonth must be in MM format (01-12)")
    private String cardExpiryMonth;
    
    /**
     * Card expiry year (YYYY format).
     */
    @NotBlank(message = "cardExpiryYear is required")
    @Pattern(regexp = "^20[0-9]{2}$", message = "cardExpiryYear must be in YYYY format")
    private String cardExpiryYear;
    
    /**
     * External reference or transaction reference number.
     */
    @NotBlank(message = "externalReference is required")
    private String externalReference;
}
