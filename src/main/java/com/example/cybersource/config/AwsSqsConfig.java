package com.example.cybersource.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

/**
 * Configuration for AWS SQS client.
 */
@Configuration
public class AwsSqsConfig {

    @Value("${aws.sqs.region:us-east-1}")
    private String region;

    @Value("${aws.sqs.access-key:}")
    private String accessKey;

    @Value("${aws.sqs.secret-key:}")
    private String secretKey;

    @Value("${aws.sqs.endpoint:}")
    private String endpoint;

    /**
     * Creates and configures the SQS client bean.
     *
     * @return configured SqsClient
     */
    @Bean
    public SqsClient sqsClient() {
        var builder = SqsClient.builder()
                .region(Region.of(region));

        // Add credentials if provided
        if (!accessKey.isEmpty() && !secretKey.isEmpty()) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)
                    )
            );
        }

        // Add custom endpoint if provided (useful for local testing with LocalStack)
        if (!endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        return builder.build();
    }
}
