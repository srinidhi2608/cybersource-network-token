package com.example.cybersource.exception;

/**
 * Exception thrown when data access operations fail
 */
public class DataAccessException extends CybersourceException {
    
    public DataAccessException(String message) {
        super(message);
    }
    
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}