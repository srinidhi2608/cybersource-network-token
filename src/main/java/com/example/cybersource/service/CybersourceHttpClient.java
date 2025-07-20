package com.example.cybersource.service;// ... imports and component annotations ...


import java.net.URI;
import java.net.http.HttpResponse;


import com.example.cybersource.config.CybersourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;

@Component
public class CybersourceHttpClient {

    @Autowired
    private CybersourceConfig config;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private final HttpClient httpClient = HttpClient.newHttpClient();


    public String get(String path) throws Exception {
        String jwt = jwtTokenUtil.generateJwt(config.getMerchantId(), config.getApiKey(), config.getSecretKey(), path, "GET");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(config.getBaseUrl() + path))
                .header("v-c-merchant-id", config.getMerchantId())
                .header("Authorization", "Bearer " + jwt)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }


public String post(String path, String payload) throws Exception {
        String jwt = jwtTokenUtil.generateJwt(
            config.getMerchantId(), config.getApiKey(), config.getKeyId(), path, "POST"
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(config.getBaseUrl() + path))
                .header("Content-Type", "application/json")
                .header("v-c-merchant-id", config.getMerchantId())
                .header("Authorization", "Bearer " + jwt)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return response.body();
    }
}