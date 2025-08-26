package com.example.cybersource.exception;

/**
 * Exception thrown when network connectivity issues occur
 */
public class NetworkException extends CybersourceException {
    
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}