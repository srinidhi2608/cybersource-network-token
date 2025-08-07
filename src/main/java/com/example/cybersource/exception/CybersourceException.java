package com.example.cybersource.exception;

/**
 * Base exception class for all Cybersource-related exceptions
 */
public class CybersourceException extends Exception {
    
    public CybersourceException(String message) {
        super(message);
    }
    
    public CybersourceException(String message, Throwable cause) {
        super(message, cause);
    }
}