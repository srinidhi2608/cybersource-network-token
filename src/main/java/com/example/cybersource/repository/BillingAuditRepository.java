package com.example.cybersource.repository;

import com.example.cybersource.entity.BillingAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for BillingAudit entity.
 * Provides CRUD operations and custom queries for billing audit records.
 */
@Repository
public interface BillingAuditRepository extends MongoRepository<BillingAudit, String> {
    
    /**
     * Find billing audit records by batch number.
     *
     * @param billingBatchNumber the batch number
     * @return list of billing audit records
     */
    List<BillingAudit> findByBillingBatchNumber(String billingBatchNumber);
    
    /**
     * Find billing audit record by reference number.
     *
     * @param billingReferenceNumber the reference number
     * @return optional billing audit record
     */
    Optional<BillingAudit> findByBillingReferenceNumber(String billingReferenceNumber);
    
    /**
     * Find billing audit records for a merchant within a time range.
     *
     * @param merchantTokenRegistrationId the merchant token registration ID
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of billing audit records
     */
    List<BillingAudit> findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
            String merchantTokenRegistrationId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find billing audit records by trace ID event type.
     *
     * @param traceIdEvent the trace ID event type
     * @return list of billing audit records
     */
    List<BillingAudit> findByTraceIdEvent(String traceIdEvent);
    
    /**
     * Count billing audit records in a batch.
     *
     * @param billingBatchNumber the batch number
     * @return count of records
     */
    long countByBillingBatchNumber(String billingBatchNumber);
    
    /**
     * Find billing audit records within a time range (eventTimeStamp).
     *
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of billing audit records
     */
    List<BillingAudit> findByEventTimeStampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find distinct merchant token registration IDs within a time range.
     * Uses MongoDB aggregation to return unique merchant IDs.
     *
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of distinct merchant token registration IDs
     */
    @Query(value = "{ 'eventTimeStamp': { $gte: ?0, $lte: ?1 } }", fields = "{ 'merchantTokenRegistrationId': 1 }")
    List<BillingAudit> findByEventTimeStampBetweenWithMerchantIdOnly(LocalDateTime startTime, LocalDateTime endTime);
}
