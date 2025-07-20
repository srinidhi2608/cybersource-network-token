package com.example.cybersource.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class InstrumentIdentifierService {

    @Autowired
    private CybersourceHttpClient cybersourceHttpClient;

    public String createInstrumentIdentifier(String cardNumber) throws Exception {
        String payload = """
        {
            "card": {
                "number": "%s"
            }
        }
        """.formatted(cardNumber);

        // POST /instrumentidentifiers endpoint (example)
        String response = cybersourceHttpClient.post("/pts/v2/instrumentidentifiers", payload);
        return response;
    }

    public String getInstrumentIdentifier(String instrumentIdentifierTokenId) throws Exception {
        // GET /instrumentidentifiers/{tokenId}
        String response = cybersourceHttpClient.get("/pts/v2/instrumentidentifiers/" + instrumentIdentifierTokenId);
        return response;
    }
}