package com.example.cybersource.repository;

import com.example.cybersource.entity.TokenStorage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface TokenStorageRepository extends MongoRepository<TokenStorage, String> {
    
    /**
     * Find token storage by payment token ID
     * @param paymentTokenId the payment token ID to search for
     * @return Optional containing the token storage if found
     */
    Optional<TokenStorage> findByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find all token storage records for a given merchant ID
     * @param merchantId the merchant ID to search for
     * @return List of token storage records
     */
    List<TokenStorage> findByMerchantId(String merchantId);
    
    /**
     * Check if a payment token ID already exists
     * @param paymentTokenId the payment token ID to check
     * @return true if exists, false otherwise
     */
    boolean existsByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find token storage by merchant ID and cryptogram
     * @param merchantId the merchant ID
     * @param cryptogram the cryptogram
     * @return Optional containing the token storage if found
     */
    @Query("{ 'merchantId': ?0, 'cryptogram': ?1 }")
    Optional<TokenStorage> findByMerchantIdAndCryptogram(String merchantId, String cryptogram);
}