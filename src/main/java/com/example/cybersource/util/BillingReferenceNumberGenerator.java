package com.example.cybersource.util;

import com.example.cybersource.constants.BillingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Utility class for generating unique billing reference numbers.
 * Uses MongoDB atomic findAndModify operation to ensure uniqueness across multiple threads and containers.
 * 
 * Format: BILL-{timestamp}-{sequence}
 * Example: BILL-1710345678-00000001
 */
@Component
public class BillingReferenceNumberGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(BillingReferenceNumberGenerator.class);
    
    private final MongoTemplate mongoTemplate;
    
    public BillingReferenceNumberGenerator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }
    
    /**
     * Generates a unique billing reference number using MongoDB atomic counter.
     * Thread-safe and works across multiple containers.
     *
     * @return unique billing reference number
     */
    public String generateUniqueReferenceNumber() {
        long timestamp = Instant.now().getEpochSecond();
        long sequence = getNextSequence(BillingConstants.SEQUENCE_NAME);
        
        String referenceNumber = String.format(
                BillingConstants.BILLING_REF_FORMAT,
                BillingConstants.BILLING_REF_PREFIX,
                timestamp,
                sequence
        );
        
        logger.debug("Generated billing reference number: {}", referenceNumber);
        return referenceNumber;
    }
    
    /**
     * Gets the next sequence value from MongoDB using findAndModify atomic operation.
     *
     * @param sequenceName the name of the sequence
     * @return next sequence value
     */
    private long getNextSequence(String sequenceName) {
        Query query = new Query(Criteria.where(BillingConstants.SEQUENCE_ID_FIELD).is(sequenceName));
        Update update = new Update().inc(BillingConstants.SEQUENCE_VALUE_FIELD, 1);
        FindAndModifyOptions options = FindAndModifyOptions.options()
                .returnNew(true)
                .upsert(true);
        
        SequenceDocument sequenceDoc = mongoTemplate.findAndModify(
                query,
                update,
                options,
                SequenceDocument.class,
                BillingConstants.DEFAULT_SEQUENCE_COLLECTION
        );
        
        if (sequenceDoc == null) {
            logger.warn("Failed to get sequence, using fallback value 1");
            return 1L;
        }
        
        return sequenceDoc.getSeq();
    }
    
    /**
     * Inner class representing the sequence document in MongoDB.
     * Package-private for testing purposes.
     */
    static class SequenceDocument {
        private String _id;
        private long seq;
        
        public String get_id() {
            return _id;
        }
        
        public void set_id(String _id) {
            this._id = _id;
        }
        
        public long getSeq() {
            return seq;
        }
        
        public void setSeq(long seq) {
            this.seq = seq;
        }
    }
}
