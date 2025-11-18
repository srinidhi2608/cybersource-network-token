package com.example.cybersource.service;

import com.example.cybersource.config.CybersourceConfig;
import com.example.cybersource.exception.CybersourceApiException;
import com.example.cybersource.exception.NetworkException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service wrapper for making REST calls to Cybersource API using Spring RestClient.
 * Handles authentication, headers, and error handling for all Cybersource API calls.
 */
@Service
public class CybersourceRestClient {

    private static final Logger logger = LoggerFactory.getLogger(CybersourceRestClient.class);

    private final RestClient restClient;
    private final CybersourceConfig config;
    private final JwtTokenUtil jwtTokenUtil;

    public CybersourceRestClient(RestClient restClient, CybersourceConfig config, JwtTokenUtil jwtTokenUtil) {
        this.restClient = restClient;
        this.config = config;
        this.jwtTokenUtil = jwtTokenUtil;
    }

    /**
     * Makes a GET request to Cybersource API.
     *
     * @param path the API path (e.g., "/pts/v2/instrumentidentifiers/123")
     * @param merchantId the merchant ID to use for the request
     * @return the response body as a String
     * @throws CybersourceApiException if the API returns an error response
     * @throws NetworkException if a network error occurs
     */
    public String get(String path, String merchantId) throws CybersourceApiException, NetworkException {
        logger.info("Making GET request to Cybersource API: {} for merchant: {}", path, merchantId);
        
        try {
            String jwt = jwtTokenUtil.generateJwt(merchantId, config.getApiKey(), config.getSecretKey(), path, "GET");
            
            String response = restClient.get()
                    .uri(config.getBaseUrl() + path)
                    .header("v-c-merchant-id", merchantId)
                    .header("Authorization", "Bearer " + jwt)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        String errorBody;
                        try {
                            errorBody = new String(clientResponse.getBody().readAllBytes());
                        } catch (Exception e) {
                            errorBody = "Unable to read error response";
                        }
                        logger.error("Cybersource API error for GET {}: Status={}, Body={}", 
                                path, clientResponse.getStatusCode(), errorBody);
                        throw new RuntimeException(new CybersourceApiException(
                                "Cybersource API error for GET " + path,
                                clientResponse.getStatusCode().value(),
                                errorBody,
                                null
                        ));
                    })
                    .body(String.class);
            
            logger.info("Successfully completed GET request to Cybersource API: {}", path);
            return response;
            
        } catch (RestClientException e) {
            logger.error("Network error during GET request to Cybersource API: {}", path, e);
            throw new NetworkException("Network error while calling Cybersource API", e);
        } catch (RuntimeException e) {
            // Unwrap CybersourceApiException if it was wrapped
            if (e.getCause() instanceof CybersourceApiException) {
                throw (CybersourceApiException) e.getCause();
            }
            logger.error("Unexpected runtime error during GET request to Cybersource API: {}", path, e);
            throw new NetworkException("Unexpected error while calling Cybersource API", e);
        } catch (Exception e) {
            logger.error("Unexpected error during GET request to Cybersource API: {}", path, e);
            throw new NetworkException("Unexpected error while calling Cybersource API", e);
        }
    }

    /**
     * Makes a POST request to Cybersource API.
     *
     * @param path the API path (e.g., "/pts/v2/instrumentidentifiers")
     * @param payload the JSON payload as a String
     * @param merchantId the merchant ID to use for the request
     * @return the response body as a String
     * @throws CybersourceApiException if the API returns an error response
     * @throws NetworkException if a network error occurs
     */
    public String post(String path, String payload, String merchantId) throws CybersourceApiException, NetworkException {
        logger.info("Making POST request to Cybersource API: {} for merchant: {}", path, merchantId);
        
        try {
            String jwt = jwtTokenUtil.generateJwt(merchantId, config.getApiKey(), config.getKeyId(), path, "POST");
            
            String response = restClient.post()
                    .uri(config.getBaseUrl() + path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("v-c-merchant-id", merchantId)
                    .header("Authorization", "Bearer " + jwt)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, clientResponse) -> {
                        String errorBody;
                        try {
                            errorBody = new String(clientResponse.getBody().readAllBytes());
                        } catch (Exception e) {
                            errorBody = "Unable to read error response";
                        }
                        logger.error("Cybersource API error for POST {}: Status={}, Body={}", 
                                path, clientResponse.getStatusCode(), errorBody);
                        throw new RuntimeException(new CybersourceApiException(
                                "Cybersource API error for POST " + path,
                                clientResponse.getStatusCode().value(),
                                errorBody,
                                null
                        ));
                    })
                    .body(String.class);
            
            logger.info("Successfully completed POST request to Cybersource API: {}", path);
            return response;
            
        } catch (RestClientException e) {
            logger.error("Network error during POST request to Cybersource API: {}", path, e);
            throw new NetworkException("Network error while calling Cybersource API", e);
        } catch (RuntimeException e) {
            // Unwrap CybersourceApiException if it was wrapped
            if (e.getCause() instanceof CybersourceApiException) {
                throw (CybersourceApiException) e.getCause();
            }
            logger.error("Unexpected runtime error during POST request to Cybersource API: {}", path, e);
            throw new NetworkException("Unexpected error while calling Cybersource API", e);
        } catch (Exception e) {
            logger.error("Unexpected error during POST request to Cybersource API: {}", path, e);
            throw new NetworkException("Unexpected error while calling Cybersource API", e);
        }
    }
}
