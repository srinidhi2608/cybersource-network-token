package com.example.cybersource.service;

import com.example.cybersource.dto.*;
import com.example.cybersource.entity.*;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Comprehensive service for managing network tokens with audit logging and duplicate detection.
 */
@Service
public class NetworkTokenService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkTokenService.class);

    private final MerchantEnrollResponseRepository merchantRepository;
    private final TokenTransactionRepository tokenTransactionRepository;
    private final TokenAuditRepository tokenAuditRepository;
    private final FetchInformationAuditRepository fetchInformationAuditRepository;
    private final InstrumentIdentifierService instrumentIdentifierService;
    private final CybersourceRestClient cybersourceRestClient;
    private final EncryptionService encryptionService;

    public NetworkTokenService(
            MerchantEnrollResponseRepository merchantRepository,
            TokenTransactionRepository tokenTransactionRepository,
            TokenAuditRepository tokenAuditRepository,
            FetchInformationAuditRepository fetchInformationAuditRepository,
            InstrumentIdentifierService instrumentIdentifierService,
            CybersourceRestClient cybersourceRestClient,
            EncryptionService encryptionService) {
        this.merchantRepository = merchantRepository;
        this.tokenTransactionRepository = tokenTransactionRepository;
        this.tokenAuditRepository = tokenAuditRepository;
        this.fetchInformationAuditRepository = fetchInformationAuditRepository;
        this.instrumentIdentifierService = instrumentIdentifierService;
        this.cybersourceRestClient = cybersourceRestClient;
        this.encryptionService = encryptionService;
    }

    /**
     * Creates a network token with comprehensive audit logging and duplicate detection.
     * 
     * @param request the network token creation request
     * @return the network token response with token details and cryptogram
     * @throws CybersourceException if token creation fails
     */
    @Transactional
    public CreateNetworkTokenResponse createNetworkToken(CreateNetworkTokenRequest request) throws CybersourceException {
        logger.info("Creating network token for merchant: {}, externalReference: {}", 
                request.getMerchantTokenRegistrationId(), request.getExternalReference());

        // Step 1: Validate merchant exists and is active
        MerchantEnrollResponse merchant = validateMerchant(request.getMerchantTokenRegistrationId());
        
        // Step 2: Generate UUID as paymentTokenId
        String paymentTokenId = UUID.randomUUID().toString();
        logger.info("Generated paymentTokenId: {}", paymentTokenId);

        // Step 3: Create audit record (ALWAYS created, even for duplicates)
        TokenAudit audit = createInitialAudit(request, paymentTokenId);

        try {
            // Step 4: Call Cybersource createInstrumentIdentifier
            String instrumentResponse = instrumentIdentifierService.createInstrumentIdentifier(
                    request.getCardNumber(), 
                    merchant.getTransactingOrgId()
            );
            JSONObject instrumentJson = new JSONObject(instrumentResponse);
            String instrumentIdentifierId = instrumentJson.getString("id");
            
            logger.info("Created instrument identifier: {}", instrumentIdentifierId);
            audit.setInstrumentIdentifierId(instrumentIdentifierId);

            // Step 5: Check if instrumentIdentifierId exists (duplicate detection)
            Optional<TokenTransaction> existingToken = tokenTransactionRepository
                    .findByInstrumentIdentifierId(instrumentIdentifierId);

            if (existingToken.isPresent()) {
                logger.info("Duplicate token detected for instrumentIdentifierId: {}", instrumentIdentifierId);
                return handleDuplicateToken(existingToken.get(), audit, request.getExternalReference());
            }

            // Step 6: If new token, fetch network token from Cybersource
            String networkTokenPath = "/pts/v2/instrumentidentifiers/" + instrumentIdentifierId + "/networkTokens";
            String networkTokenResponse = cybersourceRestClient.get(networkTokenPath, merchant.getTransactingOrgId());
            JSONObject networkTokenJson = new JSONObject(networkTokenResponse);
            
            // Extract network token details
            JSONObject networkTokenData = networkTokenJson.getJSONObject("networkToken");
            String networkToken = networkTokenData.getString("number");
            String par = networkTokenData.optString("par", "");
            String tokenExpiryMonth = networkTokenData.getString("expirationMonth");
            String tokenExpiryYear = networkTokenData.getString("expirationYear");
            String tokenStatus = networkTokenData.optString("status", "ACTIVE");

            logger.info("Retrieved network token for instrumentIdentifierId: {}", instrumentIdentifierId);

            // Step 7: Fetch cryptogram (payment credentials)
            String cryptogramPath = "/pts/v2/instrumentidentifiers/" + instrumentIdentifierId + "/paymentCredentials";
            String cryptogramResponse = cybersourceRestClient.get(cryptogramPath, merchant.getTransactingOrgId());
            JSONObject cryptogramJson = new JSONObject(cryptogramResponse);
            String cryptogram = cryptogramJson.getJSONObject("networkToken").getString("cryptogram");

            logger.info("Retrieved cryptogram for instrumentIdentifierId: {}", instrumentIdentifierId);

            // Step 8: Encrypt networkToken using Base64
            String encryptedNetworkToken = encryptionService.encrypt(networkToken);

            // Step 9: Save to TokenTransactions
            TokenTransaction tokenTransaction = TokenTransaction.builder()
                    .paymentTokenId(paymentTokenId)
                    .merchantTokenRegistrationId(request.getMerchantTokenRegistrationId())
                    .networkToken(encryptedNetworkToken)
                    .instrumentIdentifierId(instrumentIdentifierId)
                    .par(par)
                    .tokenExpiryMonth(tokenExpiryMonth)
                    .tokenExpiryYear(tokenExpiryYear)
                    .tokenStatus(tokenStatus)
                    .timestamp(LocalDateTime.now())
                    .build();
            tokenTransactionRepository.save(tokenTransaction);

            logger.info("Saved token transaction with paymentTokenId: {}", paymentTokenId);

            // Step 10: Record in FetchInformationAudit
            FetchInformationAudit fetchAudit = FetchInformationAudit.builder()
                    .paymentTokenId(paymentTokenId)
                    .informationType("InstrumentIdentifier")
                    .timestamp(LocalDateTime.now())
                    .isRequestComplete(true)
                    .externalReference(request.getExternalReference())
                    .traceId(UUID.randomUUID().toString())
                    .build();
            fetchInformationAuditRepository.save(fetchAudit);

            // Step 11: Update TokenAudit as complete and not duplicate
            audit.setIsRequestComplete(true);
            audit.setIsDuplicate(false);
            tokenAuditRepository.save(audit);

            logger.info("Successfully created network token for paymentTokenId: {}", paymentTokenId);

            // Step 12: Return response with both cryptogram AND network token with PAR
            return CreateNetworkTokenResponse.builder()
                    .paymentTokenId(paymentTokenId)
                    .networkToken(networkToken) // Return decrypted for response
                    .cryptogram(cryptogram)
                    .par(par)
                    .tokenExpiryMonth(tokenExpiryMonth)
                    .tokenExpiryYear(tokenExpiryYear)
                    .tokenStatus(tokenStatus)
                    .instrumentIdentifierId(instrumentIdentifierId)
                    .build();

        } catch (Exception e) {
            logger.error("Error creating network token for merchant: {}, externalReference: {}", 
                    request.getMerchantTokenRegistrationId(), request.getExternalReference(), e);
            
            // Update audit with failure reason
            audit.setFailureReason(e.getMessage());
            audit.setIsRequestComplete(false);
            tokenAuditRepository.save(audit);
            
            throw new CybersourceException("Failed to create network token", e);
        }
    }

    /**
     * Creates a cryptogram for an existing payment token.
     * 
     * @param request the cryptogram creation request
     * @return the cryptogram response
     * @throws CybersourceException if cryptogram creation fails
     */
    @Transactional
    public CreateCryptogramResponse createCryptogram(CreateCryptogramRequest request) throws CybersourceException {
        logger.info("Creating cryptogram for paymentTokenId: {}, externalReference: {}", 
                request.getPaymentTokenId(), request.getExternalReference());

        try {
            // Step 1: Lookup TokenTransaction by paymentTokenId
            TokenTransaction tokenTransaction = tokenTransactionRepository
                    .findByPaymentTokenId(request.getPaymentTokenId())
                    .orElseThrow(() -> new CybersourceException(
                            "Token not found for paymentTokenId: " + request.getPaymentTokenId()));

            // Step 2: Get merchant info from merchantTokenRegistrationId
            MerchantEnrollResponse merchant = validateMerchant(tokenTransaction.getMerchantTokenRegistrationId());

            // Step 3: Call Cybersource to fetch fresh cryptogram
            String cryptogramPath = "/pts/v2/instrumentidentifiers/" + 
                    tokenTransaction.getInstrumentIdentifierId() + "/paymentCredentials";
            String cryptogramResponse = cybersourceRestClient.get(cryptogramPath, merchant.getTransactingOrgId());
            JSONObject cryptogramJson = new JSONObject(cryptogramResponse);
            String cryptogram = cryptogramJson.getJSONObject("networkToken").getString("cryptogram");

            logger.info("Retrieved cryptogram for paymentTokenId: {}", request.getPaymentTokenId());

            // Step 4: Record in FetchInformationAudit with informationType="Cryptogram"
            FetchInformationAudit fetchAudit = FetchInformationAudit.builder()
                    .paymentTokenId(request.getPaymentTokenId())
                    .informationType("Cryptogram")
                    .timestamp(LocalDateTime.now())
                    .isRequestComplete(true)
                    .externalReference(request.getExternalReference())
                    .traceId(UUID.randomUUID().toString())
                    .build();
            fetchInformationAuditRepository.save(fetchAudit);

            logger.info("Successfully created cryptogram for paymentTokenId: {}", request.getPaymentTokenId());

            // Step 5: Return ONLY cryptogram
            return CreateCryptogramResponse.builder()
                    .cryptogram(cryptogram)
                    .paymentTokenId(request.getPaymentTokenId())
                    .build();

        } catch (CybersourceException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error creating cryptogram for paymentTokenId: {}, externalReference: {}", 
                    request.getPaymentTokenId(), request.getExternalReference(), e);
            throw new CybersourceException("Failed to create cryptogram", e);
        }
    }

    /**
     * Validates that a merchant exists and is active.
     */
    private MerchantEnrollResponse validateMerchant(String merchantTokenRegistrationId) throws CybersourceException {
        MerchantEnrollResponse merchant = merchantRepository
                .findByMerchantTokenRegistrationId(merchantTokenRegistrationId)
                .orElseThrow(() -> new CybersourceException(
                        "Merchant not found: " + merchantTokenRegistrationId));

        if (merchant.getStatus() != MerchantEnrollResponse.MerchantStatus.ACTIVE) {
            throw new CybersourceException(
                    "Merchant is not active: " + merchantTokenRegistrationId + ", status: " + merchant.getStatus());
        }

        return merchant;
    }

    /**
     * Creates initial audit record for a token creation request.
     */
    private TokenAudit createInitialAudit(CreateNetworkTokenRequest request, String paymentTokenId) {
        TokenAudit audit = TokenAudit.builder()
                .paymentTokenId(paymentTokenId)
                .merchantTokenRegistrationId(request.getMerchantTokenRegistrationId())
                .cardExpiryMonth(request.getCardExpiryMonth())
                .cardExpiryYear(request.getCardExpiryYear())
                .timestamp(LocalDateTime.now())
                .isRequestComplete(false)
                .isDuplicate(false)
                .externalReference(request.getExternalReference())
                .traceId(UUID.randomUUID().toString())
                .build();
        return tokenAuditRepository.save(audit);
    }

    /**
     * Handles duplicate token detection by returning existing token info.
     */
    private CreateNetworkTokenResponse handleDuplicateToken(
            TokenTransaction existingToken, 
            TokenAudit audit, 
            String externalReference) throws CybersourceException {
        
        logger.info("Handling duplicate token for instrumentIdentifierId: {}", 
                existingToken.getInstrumentIdentifierId());

        try {
            // Mark audit as duplicate
            audit.setIsDuplicate(true);
            audit.setIsRequestComplete(true);
            tokenAuditRepository.save(audit);

            // Get merchant to fetch fresh cryptogram
            MerchantEnrollResponse merchant = validateMerchant(existingToken.getMerchantTokenRegistrationId());

            // Fetch fresh cryptogram for duplicate request
            String cryptogramPath = "/pts/v2/instrumentidentifiers/" + 
                    existingToken.getInstrumentIdentifierId() + "/paymentCredentials";
            String cryptogramResponse = cybersourceRestClient.get(cryptogramPath, merchant.getTransactingOrgId());
            JSONObject cryptogramJson = new JSONObject(cryptogramResponse);
            String cryptogram = cryptogramJson.getJSONObject("networkToken").getString("cryptogram");

            // Decrypt network token for response
            String decryptedNetworkToken = encryptionService.decrypt(existingToken.getNetworkToken());

            // Record fetch audit
            FetchInformationAudit fetchAudit = FetchInformationAudit.builder()
                    .paymentTokenId(existingToken.getPaymentTokenId())
                    .informationType("Cryptogram")
                    .timestamp(LocalDateTime.now())
                    .isRequestComplete(true)
                    .externalReference(externalReference)
                    .traceId(UUID.randomUUID().toString())
                    .build();
            fetchInformationAuditRepository.save(fetchAudit);

            logger.info("Successfully handled duplicate token for paymentTokenId: {}", 
                    existingToken.getPaymentTokenId());

            return CreateNetworkTokenResponse.builder()
                    .paymentTokenId(existingToken.getPaymentTokenId())
                    .networkToken(decryptedNetworkToken)
                    .cryptogram(cryptogram)
                    .par(existingToken.getPar())
                    .tokenExpiryMonth(existingToken.getTokenExpiryMonth())
                    .tokenExpiryYear(existingToken.getTokenExpiryYear())
                    .tokenStatus(existingToken.getTokenStatus())
                    .instrumentIdentifierId(existingToken.getInstrumentIdentifierId())
                    .build();

        } catch (Exception e) {
            logger.error("Error handling duplicate token", e);
            audit.setFailureReason(e.getMessage());
            audit.setIsRequestComplete(false);
            tokenAuditRepository.save(audit);
            throw new CybersourceException("Failed to handle duplicate token", e);
        }
    }
}
