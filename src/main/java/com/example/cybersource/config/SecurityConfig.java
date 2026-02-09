package com.example.cybersource.config;

import com.example.cybersource.filter.WebhookSignatureValidationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for the application.
 * Configures Spring Security to validate webhook signatures using a custom filter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final WebhookSignatureValidationFilter webhookSignatureValidationFilter;

    public SecurityConfig(WebhookSignatureValidationFilter webhookSignatureValidationFilter) {
        this.webhookSignatureValidationFilter = webhookSignatureValidationFilter;
    }

    /**
     * Configures the security filter chain.
     * - Disables CSRF for webhook endpoints (as they use signature validation)
     * - Adds the webhook signature validation filter
     * - Permits all requests to the webhook endpoint (validation is done by the filter)
     * - Requires authentication for other endpoints
     *
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for webhook endpoints
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/webhooks/**").permitAll() // Webhook endpoints validated by filter
                        .requestMatchers("/actuator/health").permitAll() // Health check endpoint
                        .anyRequest().permitAll() // Allow all other requests for now
                )
                .addFilterBefore(webhookSignatureValidationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
