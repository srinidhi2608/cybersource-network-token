package com.example.cybersource.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentCredentialsService {

    @Autowired
    private CybersourceHttpClient cybersourceHttpClient;

    public String getPaymentCredentials(String instrumentIdentifierTokenId) throws Exception {
        // GET /paymentinstruments/{instrumentIdentifierTokenId}/networktokens (example endpoint)
        String response = cybersourceHttpClient.get("/pts/v2/instrumentidentifiers/" + instrumentIdentifierTokenId + "/networkTokens");
        return response;
    }
}