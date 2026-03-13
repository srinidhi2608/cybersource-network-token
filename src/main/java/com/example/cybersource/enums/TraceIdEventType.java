package com.example.cybersource.enums;

/**
 * Enum representing the type of trace ID event for billing audit.
 * Used to categorize different types of operations in the billing audit trail.
 */
public enum TraceIdEventType {
    
    /**
     * Network token processing event from TokenAudit collection.
     */
    NETWORK_TOKEN_PROCESSING("NetworkTokenProcessing"),
    
    /**
     * Cryptogram request event from FetchInformationAudit collection.
     */
    REQUEST_CRYPTOGRAM("Request Cryptogram"),
    
    /**
     * Token lifecycle management event from TokenEvents collection.
     */
    TOKEN_LIFECYCLE_MANAGEMENT("TokenLifeCycleManagement");
    
    private final String displayName;
    
    TraceIdEventType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
