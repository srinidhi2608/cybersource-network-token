package com.example.cybersource.repository;

import com.example.cybersource.entity.TokenTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for TokenTransaction entity.
 */
@Repository
public interface TokenTransactionRepository extends MongoRepository<TokenTransaction, String> {
    
    /**
     * Find token transaction by paymentTokenId.
     *
     * @param paymentTokenId the payment token ID
     * @return Optional containing the token transaction if found
     */
    Optional<TokenTransaction> findByPaymentTokenId(String paymentTokenId);
    
    /**
     * Find token transaction by instrumentIdentifierId.
     *
     * @param instrumentIdentifierId the instrument identifier ID
     * @return Optional containing the token transaction if found
     */
    Optional<TokenTransaction> findByInstrumentIdentifierId(String instrumentIdentifierId);
    
    /**
     * Check if token transaction exists with given instrumentIdentifierId.
     *
     * @param instrumentIdentifierId the instrument identifier ID
     * @return true if exists, false otherwise
     */
    boolean existsByInstrumentIdentifierId(String instrumentIdentifierId);
    
    /**
     * Find all token transactions by payment token IDs (batch query).
     * Used for efficient bulk lookups to get merchantTokenRegistrationId.
     *
     * @param paymentTokenIds list of payment token IDs
     * @return list of matching token transactions
     */
    List<TokenTransaction> findByPaymentTokenIdIn(List<String> paymentTokenIds);
}
