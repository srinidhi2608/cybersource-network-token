package com.example.cybersource.repository;

import com.example.cybersource.entity.TokenAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TokenAudit entity.
 */
@Repository
public interface TokenAuditRepository extends MongoRepository<TokenAudit, String> {
    
    /**
     * Find token audits by paymentTokenId.
     *
     * @param paymentTokenId the payment token ID
     * @return List of token audits
     */
    List<TokenAudit> findByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find token audits by instrumentIdentifierId.
     *
     * @param instrumentIdentifierId the instrument identifier ID
     * @return List of token audits
     */
    List<TokenAudit> findByInstrumentIdentifierId(String instrumentIdentifierId);
    
    /**
     * Find the first (original) non-duplicate audit record by instrumentIdentifierId.
     *
     * @param instrumentIdentifierId the instrument identifier ID
     * @param isDuplicate should be false to find the original
     * @return Optional containing the original audit record
     */
    Optional<TokenAudit> findFirstByInstrumentIdentifierIdAndIsDuplicate(String instrumentIdentifierId, Boolean isDuplicate);
    
    /**
     * Find token audits by externalReference.
     *
     * @param externalReference the external reference
     * @return List of token audits
     */
    List<TokenAudit> findByExternalReference(String externalReference);
    
    /**
     * Find token audit by traceId.
     *
     * @param traceId the unique trace ID
     * @return Optional containing the token audit if found
     */
    Optional<TokenAudit> findByTraceId(String traceId);
}
