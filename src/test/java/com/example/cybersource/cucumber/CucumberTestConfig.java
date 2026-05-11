package com.example.cybersource.cucumber;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.SpringBootConfiguration;

/**
 * Minimal Spring Boot configuration for Cucumber tests.
 *
 * <p>No component scanning of the main source packages is done here — all
 * collaborators (repositories, services, etc.) are created as Mockito mocks
 * directly inside each step definition class.  The only purpose of this class
 * is to give {@code @SpringBootTest} a valid {@code @SpringBootConfiguration}
 * entry point so the Cucumber context can start.
 */
@SpringBootConfiguration
@EnableAutoConfiguration(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoRepositoriesAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class,
        JmxAutoConfiguration.class,
        org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration.class
})
public class CucumberTestConfig {
    // Intentionally empty — step definitions manage all collaborators via Mockito.
}



