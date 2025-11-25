package com.example.cybersource.repository;

import com.example.cybersource.entity.FetchInformationAudit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for FetchInformationAudit entity.
 */
@Repository
public interface FetchInformationAuditRepository extends MongoRepository<FetchInformationAudit, String> {
    
    /**
     * Find fetch information audits by paymentTokenId.
     *
     * @param paymentTokenId the payment token ID
     * @return List of fetch information audits
     */
    List<FetchInformationAudit> findByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find fetch information audits by informationType.
     *
     * @param informationType the information type
     * @return List of fetch information audits
     */
    List<FetchInformationAudit> findByInformationType(String informationType);
    
    /**
     * Find fetch information audits by externalReference.
     *
     * @param externalReference the external reference
     * @return List of fetch information audits
     */
    List<FetchInformationAudit> findByExternalReference(String externalReference);
    
    /**
     * Find fetch information audit by traceId.
     *
     * @param traceId the unique trace ID
     * @return Optional containing the fetch information audit if found
     */
    Optional<FetchInformationAudit> findByTraceId(String traceId);
}
