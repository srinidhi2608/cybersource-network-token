package com.example.cybersource.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        Map<String, Object> detailsMap = new HashMap<>(errors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .details(detailsMap)
                .build();

        logger.warn("Validation error: {}", errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles CybersourceApiException (API errors with status codes).
     */
    @ExceptionHandler(CybersourceApiException.class)
    public ResponseEntity<ErrorResponse> handleCybersourceApiException(CybersourceApiException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_GATEWAY.value())
                .error("Cybersource API Error")
                .message(ex.getMessage())
                .details(Map.of(
                        "statusCode", ex.getStatusCode(),
                        "responseBody", ex.getResponseBody()
                ))
                .build();

        logger.error("Cybersource API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
    }

    /**
     * Handles NetworkException (connectivity issues).
     */
    @ExceptionHandler(NetworkException.class)
    public ResponseEntity<ErrorResponse> handleNetworkException(NetworkException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Network Error")
                .message("Unable to connect to Cybersource API")
                .details(Map.of("cause", ex.getMessage()))
                .build();

        logger.error("Network error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Handles DataAccessException (database errors).
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(DataAccessException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Database Error")
                .message("Error accessing database")
                .details(Map.of("cause", ex.getMessage()))
                .build();

        logger.error("Data access error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles CybersourceException (general business logic errors).
     */
    @ExceptionHandler(CybersourceException.class)
    public ResponseEntity<ErrorResponse> handleCybersourceException(CybersourceException ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Business Logic Error")
                .message(ex.getMessage())
                .details(Map.of("type", ex.getClass().getSimpleName()))
                .build();

        logger.error("Cybersource exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Handles all other exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .details(Map.of("type", ex.getClass().getSimpleName()))
                .build();

        logger.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
