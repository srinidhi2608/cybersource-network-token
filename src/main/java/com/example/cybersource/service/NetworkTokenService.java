package com.example.cybersource.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

@Service
public class NetworkTokenService {

    private final InstrumentIdentifierService instrumentIdentifierService;

    private final PaymentCredentialsService paymentCredentialsService;

    public NetworkTokenService(InstrumentIdentifierService instrumentIdentifierService, PaymentCredentialsService paymentCredentialsService) {
        this.instrumentIdentifierService = instrumentIdentifierService;
        this.paymentCredentialsService = paymentCredentialsService;
    }

    public NetworkTokenResult generateNetworkTokenAndCryptogram(String cardNumber) throws Exception {
        long start = System.currentTimeMillis();

        // Step 1: Create Instrument Identifier
        String instrumentResponse = instrumentIdentifierService.createInstrumentIdentifier(cardNumber);
        JSONObject instrumentJson = new JSONObject(instrumentResponse);
        String instrumentIdentifierId = instrumentJson.getString("id");

        // Step 2: Get Network Token and Cryptogram
        String credentialsResponse = paymentCredentialsService.getPaymentCredentials(instrumentIdentifierId);
        JSONObject credentialsJson = new JSONObject(credentialsResponse);

        // Parse network token and cryptogram from response
        String networkToken = credentialsJson.getJSONObject("networkToken").getString("number");
        String cryptogram = credentialsJson.getJSONObject("networkToken").getString("cryptogram");

        long end = System.currentTimeMillis();
        long elapsedMs = end - start;

        return new NetworkTokenResult(networkToken, cryptogram, elapsedMs);
    }

    public static record NetworkTokenResult(String networkToken, String cryptogram, long elapsedMilliseconds) {}
}