package com.example.cybersource.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter @Setter
@ConfigurationProperties(prefix = "cybersource")
public class CybersourceConfig {
    private String apiKey;
    private String secretKey;
    private String merchantId;
    private String baseUrl;
    private String keyId;
}