package com.example.cybersource.controller;

import com.example.cybersource.dto.TokenLifecycleUpdateMessage;
import com.example.cybersource.service.TokenLifecycleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for receiving token lifecycle update webhooks from Cybersource.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
public class TokenLifecycleController {

    private static final Logger logger = LoggerFactory.getLogger(TokenLifecycleController.class);

    private final TokenLifecycleService tokenLifecycleService;

    public TokenLifecycleController(TokenLifecycleService tokenLifecycleService) {
        this.tokenLifecycleService = tokenLifecycleService;
    }

    /**
     * Endpoint to receive token lifecycle update messages from Cybersource Token Management Service.
     * Processes the message and forwards it to AWS SQS queue.
     *
     * @param message the token lifecycle update message
     * @return response indicating success or failure
     */
    @PostMapping("/token-lifecycle")
    public ResponseEntity<Map<String, Object>> handleTokenLifecycleUpdate(
            @Valid @RequestBody TokenLifecycleUpdateMessage message) {
        
        logger.info("Received token lifecycle update webhook for event type: {}", message.getEventType());
        
        try {
            String messageId = tokenLifecycleService.processTokenLifecycleUpdate(message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("messageId", messageId);
            response.put("message", "Token lifecycle update processed successfully");
            
            logger.info("Successfully processed token lifecycle update. SQS MessageId: {}", messageId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Failed to process token lifecycle update: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            response.put("message", "Failed to process token lifecycle update");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
