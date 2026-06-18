package com.example.cybersource.controller;

import com.example.cybersource.dto.CreateCryptogramRequest;
import com.example.cybersource.dto.CreateCryptogramResponse;
import com.example.cybersource.dto.CreateNetworkTokenRequest;
import com.example.cybersource.dto.CreateNetworkTokenResponse;
import com.example.cybersource.service.NetworkTokenService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for network token and cryptogram management.
 */
@RestController
@RequestMapping("/api/v1/network-tokens")
public class NetworkTokenController {

    private static final Logger logger = LoggerFactory.getLogger(NetworkTokenController.class);

    private final NetworkTokenService networkTokenService;

    public NetworkTokenController(NetworkTokenService networkTokenService) {
        this.networkTokenService = networkTokenService;
    }

    /**
     * Creates a network token with comprehensive audit logging.
     * 
     * @param request the network token creation request
     * @return the network token response with token details and cryptogram
     */
    @PostMapping
    public ResponseEntity<CreateNetworkTokenResponse> createNetworkToken(
            @Valid @RequestBody CreateNetworkTokenRequest request) throws Exception {
        
        logger.info("Received request to create network token for merchant: {}", 
                request.getMerchantTokenRegistrationId());
        
        CreateNetworkTokenResponse response = networkTokenService.createNetworkToken(request);
        logger.info("Successfully created network token with paymentTokenId: {}", 
                response.getPaymentTokenId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Creates a cryptogram for an existing payment token.
     * 
     * @param request the cryptogram creation request
     * @return the cryptogram response
     */
    @PostMapping("/cryptogram")
    public ResponseEntity<CreateCryptogramResponse> createCryptogram(
            @Valid @RequestBody CreateCryptogramRequest request) throws Exception {
        
        logger.info("Received request to create cryptogram for paymentTokenId: {}", 
                request.getPaymentTokenId());
        
        CreateCryptogramResponse response = networkTokenService.createCryptogram(request);
        logger.info("Successfully created cryptogram for paymentTokenId: {}", 
                response.getPaymentTokenId());
        return ResponseEntity.ok(response);
    }
}
