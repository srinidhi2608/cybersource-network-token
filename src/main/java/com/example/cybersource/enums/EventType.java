package com.example.cybersource.enums;

/**
 * Enum representing the result type of an event operation.
 */
public enum EventType {
    
    /**
     * Event completed successfully.
     */
    SUCCESS("Success"),
    
    /**
     * Event failed with an error.
     */
    FAILURE("Failure");
    
    private final String displayName;
    
    EventType(String displayName) {
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
