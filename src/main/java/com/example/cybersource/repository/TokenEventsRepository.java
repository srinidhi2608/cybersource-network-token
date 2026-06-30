package com.example.cybersource.repository;

import com.example.cybersource.entity.TokenEvents;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TokenEvents entity.
 * Provides CRUD operations and custom queries for token lifecycle events.
 */
@Repository
public interface TokenEventsRepository extends MongoRepository<TokenEvents, String> {
    
    /**
     * Find token events by payment token ID.
     *
     * @param paymentTokenId the payment token ID
     * @return list of token events
     */
    List<TokenEvents> findByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find token events within a time range.
     *
     * @param startTime start of the time range
     * @param endTime end of the time range
     * @return list of token events
     */
    List<TokenEvents> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find token event by trace ID.
     *
     * @param traceId the trace ID
     * @return optional token event
     */
    Optional<TokenEvents> findByTraceId(String traceId);
    
    /**
     * Find token events after a specific timestamp.
     *
     * @param timestamp the timestamp
     * @return list of token events
     */
    List<TokenEvents> findByTimestampAfter(LocalDateTime timestamp);
}
