package com.example.cybersource.enums;

/**
 * Enum representing different event references in FetchInformationAudit.
 * Used to categorize the type of information fetch operation.
 */
public enum EventReference {
    
    /**
     * Token creation event.
     */
    CREATE_TOKEN("CreateToken"),
    
    /**
     * Cryptogram fetch event.
     */
    FETCH_CRYPTOGRAM("FetchCryptogram"),
    
    /**
     * Lifecycle management query event.
     */
    LCM("LCM");
    
    private final String value;
    
    EventReference(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    @Override
    public String toString() {
        return value;
    }
    
    /**
     * Parse string value to EventReference enum.
     * 
     * @param value the string value to parse
     * @return matching EventReference or null if not found
     */
    public static EventReference fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (EventReference ref : EventReference.values()) {
            if (ref.getValue().equalsIgnoreCase(value)) {
                return ref;
            }
        }
        return null;
    }
}
