package com.example.cybersource.exception;

/**
 * Exception thrown when Cybersource API calls fail
 */
public class CybersourceApiException extends CybersourceException {
    
    private final int statusCode;
    private final String responseBody;
    
    public CybersourceApiException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public CybersourceApiException(String message, int statusCode, String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
}