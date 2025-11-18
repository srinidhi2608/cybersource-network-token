package com.example.cybersource.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Configuration class for Spring RestClient.
 * Replaces WebClient with RestClient for synchronous HTTP calls.
 */
@Configuration
public class RestClientConfig {

    /**
     * Creates a configured RestClient bean for making HTTP requests.
     * 
     * @return RestClient configured with appropriate timeouts
     */
    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    /**
     * Creates a ClientHttpRequestFactory with configured timeouts.
     * 
     * @return ClientHttpRequestFactory with connection and read timeouts
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000); // 30 seconds
        factory.setReadTimeout(30000);    // 30 seconds
        return factory;
    }
}
