package com.example.cybersource.repository;

import com.example.cybersource.entity.BillingAuditSyncInformation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for BillingAuditSyncInformation entity.
 * Manages the singleton record that tracks sync state.
 */
@Repository
public interface BillingAuditSyncInformationRepository extends MongoRepository<BillingAuditSyncInformation, String> {
    
    /**
     * Find the singleton sync information record.
     * Should return at most one record.
     *
     * @return the first sync information record if exists
     */
    default BillingAuditSyncInformation findSyncInfo() {
        return findAll().stream().findFirst().orElse(null);
    }
}
