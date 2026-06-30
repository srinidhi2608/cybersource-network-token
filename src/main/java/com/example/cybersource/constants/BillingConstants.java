package com.example.cybersource.constants;

/**
 * Constants for billing audit functionality.
 * Centralizes configuration keys and default values to avoid string literals.
 */
public final class BillingConstants {
    
    private BillingConstants() {
        // Prevent instantiation
    }
    
    // Configuration Keys
    public static final String CONFIG_SYNC_OFFSET_MINUTES = "billing.sync.offset-minutes";
    public static final String CONFIG_SYNC_BATCH_SIZE = "billing.sync.batch-size";
    public static final String CONFIG_SEQUENCE_COLLECTION = "billing.sequence.collection";
    
    // Default Values
    public static final int DEFAULT_SYNC_OFFSET_MINUTES = 5;
    public static final int DEFAULT_SYNC_BATCH_SIZE = 1000;
    public static final String DEFAULT_SEQUENCE_COLLECTION = "BillingSequence";
    
    // Sequence Fields
    public static final String SEQUENCE_ID_FIELD = "_id";
    public static final String SEQUENCE_VALUE_FIELD = "seq";
    public static final String SEQUENCE_NAME = "billingReferenceNumber";
    
    // Billing Reference Number Format
    public static final String BILLING_REF_PREFIX = "BILL";
    public static final String BILLING_REF_FORMAT = "%s-%d-%08d"; // PREFIX-timestamp-sequence
    
    // Collection Names
    public static final String COLLECTION_BILLING_AUDIT = "BillingAudit";
    public static final String COLLECTION_BILLING_SYNC_INFO = "BillingAuditSyncInformation";
    public static final String COLLECTION_TOKEN_AUDITS = "TokenAudits";
    public static final String COLLECTION_FETCH_INFO_AUDITS = "FetchInformationAudits";
    public static final String COLLECTION_TOKEN_EVENTS = "TokenEvents";
    
    // Field Names
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_TRACE_ID = "traceId";
    public static final String FIELD_EVENT_REFERENCE = "eventReference";
    public static final String FIELD_INFORMATION_TYPE = "informationType";
    public static final String FIELD_IS_REQUEST_COMPLETE = "isRequestComplete";
    public static final String FIELD_FAILURE_REASON = "failureReason";
}
