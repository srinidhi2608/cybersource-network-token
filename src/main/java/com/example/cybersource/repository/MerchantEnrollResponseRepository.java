package com.example.cybersource.repository;

import com.example.cybersource.entity.MerchantEnrollResponse;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for MerchantEnrollResponse entity.
 */
@Repository
public interface MerchantEnrollResponseRepository extends MongoRepository<MerchantEnrollResponse, String> {
    
    /**
     * Find merchant by merchantTokenRegistrationId.
     *
     * @param merchantTokenRegistrationId the merchant token registration ID
     * @return Optional containing the merchant if found
     */
    Optional<MerchantEnrollResponse> findByMerchantTokenRegistrationId(String merchantTokenRegistrationId);
    
    /**
     * Check if merchant exists with given merchantTokenRegistrationId.
     *
     * @param merchantTokenRegistrationId the merchant token registration ID
     * @return true if exists, false otherwise
     */
    boolean existsByMerchantTokenRegistrationId(String merchantTokenRegistrationId);
}
