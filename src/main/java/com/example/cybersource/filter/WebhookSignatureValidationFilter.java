package com.example.cybersource.filter;

import com.example.cybersource.util.SignatureValidator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to validate the HMAC-256 signature of webhook requests.
 * This filter validates the v-c-signature header against the request body
 * using HMAC-256 algorithm before allowing the request to proceed.
 */
@Component
public class WebhookSignatureValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureValidationFilter.class);
    private static final String SIGNATURE_HEADER = "v-c-signature";
    private static final String WEBHOOK_PATH = "/api/v1/webhooks/token-lifecycle";

    private final SignatureValidator signatureValidator;
    private final String webhookSecret;

    public WebhookSignatureValidationFilter(
            SignatureValidator signatureValidator,
            @Value("${webhook.signature.secret}") String webhookSecret) {
        this.signatureValidator = signatureValidator;
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only apply this filter to webhook endpoints
        if (!request.getRequestURI().equals(WEBHOOK_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        logger.debug("Processing webhook signature validation for: {}", request.getRequestURI());

        // Get the signature from the header
        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        
        if (providedSignature == null || providedSignature.isEmpty()) {
            logger.error("Missing v-c-signature header in webhook request");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Missing signature header\"}");
            return;
        }

        // Wrap the request to cache the body for multiple reads
        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);
        String requestBody = cachedRequest.getBodyAsString();
        
        if (requestBody == null || requestBody.isEmpty()) {
            logger.error("Empty request body in webhook request");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Empty request body\"}");
            return;
        }

        // Validate the signature
        boolean isValid = signatureValidator.validateSignature(requestBody, providedSignature, webhookSecret);
        
        if (!isValid) {
            logger.error("Invalid signature for webhook request");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Invalid signature\"}");
            return;
        }

        logger.info("Webhook signature validated successfully");
        
        // Signature is valid, continue with the cached request (body can be read again by controller)
        filterChain.doFilter(cachedRequest, response);
    }
}
