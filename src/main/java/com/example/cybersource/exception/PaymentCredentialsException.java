package com.example.cybersource.exception;

/**
 * Exception thrown when payment credentials cannot be retrieved
 */
public class PaymentCredentialsException extends CybersourceException {
    
    public PaymentCredentialsException(String message) {
        super(message);
    }
    
    public PaymentCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}