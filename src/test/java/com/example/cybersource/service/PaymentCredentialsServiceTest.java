package com.example.cybersource.service;

import com.example.cybersource.config.CybersourceConfig;
import com.example.cybersource.entity.TokenStorage;
import com.example.cybersource.exception.*;
import com.example.cybersource.repository.TokenStorageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentCredentialsServiceTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private CybersourceConfig cybersourceConfig;
    
    @Mock
    private JwtTokenUtil jwtTokenUtil;
    
    @Mock
    private TokenStorageRepository tokenStorageRepository;
    
    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private PaymentCredentialsService paymentCredentialsService;

    private static final String INSTRUMENT_IDENTIFIER_TOKEN_ID = "test-instrument-id";
    private static final String MERCHANT_ID = "test-merchant-123";
    private static final String JWT_TOKEN = "test-jwt-token";
    private static final String API_RESPONSE = "{\"networkToken\":{\"number\":\"1234567890123456\",\"cryptogram\":\"test-cryptogram\"}}";
    private static final String BASE_URL = "https://api.cybersource.com";
    private static final String API_KEY = "test-api-key";
    private static final String SECRET_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        // Only set up the basic config stubs that are used in all tests
        lenient().when(cybersourceConfig.getBaseUrl()).thenReturn(BASE_URL);
        lenient().when(cybersourceConfig.getApiKey()).thenReturn(API_KEY);
        lenient().when(cybersourceConfig.getSecretKey()).thenReturn(SECRET_KEY);
    }

    @Test
    void testGetPaymentCredentials_Success() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        when(tokenStorageRepository.save(any(TokenStorage.class))).thenReturn(new TokenStorage());

        // Act
        String result = paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID);

        // Assert
        assertEquals(API_RESPONSE, result);
        verify(jwtTokenUtil).generateJwt(MERCHANT_ID, API_KEY, SECRET_KEY, 
                                       "/pts/v2/instrumentidentifiers/" + INSTRUMENT_IDENTIFIER_TOKEN_ID + "/networkTokens", 
                                       "GET");
        verify(tokenStorageRepository).save(any(TokenStorage.class));
    }

    @Test
    void testGetPaymentCredentials_JwtGenerationFailure() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("JWT generation failed"));

        // Act & Assert
        PaymentCredentialsException exception = assertThrows(PaymentCredentialsException.class, 
                () -> paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));
        
        assertTrue(exception.getMessage().contains("Failed to generate JWT token"));
        verify(webClient, never()).get();
    }

    @Test
    void testGetPaymentCredentials_WebClientResponseException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        WebClientResponseException webClientException = WebClientResponseException.create(
                400, "Bad Request", null, "Invalid request".getBytes(), null);
        
        // Mock the entire reactive chain to throw the exception
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.error(webClientException));

        // Act & Assert
        CybersourceApiException exception = assertThrows(CybersourceApiException.class, 
                () -> paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));
        
        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to get payment credentials from Cybersource API"));
    }

    @Test
    void testGetPaymentCredentials_NetworkException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        
        WebClientException webClientException = new WebClientException("Connection timeout") {
            // Anonymous subclass to make the abstract class instantiable
        };
        
        // Mock the entire reactive chain to throw the exception
        when(responseSpec.bodyToMono(String.class))
            .thenReturn(Mono.error(webClientException));

        // Act & Assert
        NetworkException exception = assertThrows(NetworkException.class, 
                () -> paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));
        
        assertTrue(exception.getMessage().contains("Network error while calling Cybersource API"));
    }

    @Test
    void testGetPaymentCredentials_DataAccessException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        when(tokenStorageRepository.save(any(TokenStorage.class)))
                .thenThrow(new org.springframework.dao.DataAccessException("Database connection failed") {});

        // Act & Assert
        DataAccessException exception = assertThrows(DataAccessException.class, 
                () -> paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));
        
        assertTrue(exception.getMessage().contains("Failed to persist payment credentials"));
    }

    @Test
    void testGetTokenStorageByPaymentTokenId_Success() throws Exception {
        // Arrange
        String paymentTokenId = "test-payment-token-id";
        TokenStorage expectedTokenStorage = new TokenStorage();
        expectedTokenStorage.setPaymentTokenId(paymentTokenId);
        
        when(tokenStorageRepository.findByPaymentTokenId(paymentTokenId))
                .thenReturn(Optional.of(expectedTokenStorage));

        // Act
        Optional<TokenStorage> result = paymentCredentialsService.getTokenStorageByPaymentTokenId(paymentTokenId);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(paymentTokenId, result.get().getPaymentTokenId());
        verify(tokenStorageRepository).findByPaymentTokenId(paymentTokenId);
    }

    @Test
    void testGetTokenStorageByPaymentTokenId_NotFound() throws Exception {
        // Arrange
        String paymentTokenId = "non-existent-token-id";
        
        when(tokenStorageRepository.findByPaymentTokenId(paymentTokenId))
                .thenReturn(Optional.empty());

        // Act
        Optional<TokenStorage> result = paymentCredentialsService.getTokenStorageByPaymentTokenId(paymentTokenId);

        // Assert
        assertFalse(result.isPresent());
        verify(tokenStorageRepository).findByPaymentTokenId(paymentTokenId);
    }

    @Test
    void testGetTokenStorageByPaymentTokenId_DatabaseError() {
        // Arrange
        String paymentTokenId = "test-payment-token-id";
        
        when(tokenStorageRepository.findByPaymentTokenId(paymentTokenId))
                .thenThrow(new org.springframework.dao.DataAccessException("Database connection failed") {});

        // Act & Assert
        DataAccessException exception = assertThrows(DataAccessException.class, 
                () -> paymentCredentialsService.getTokenStorageByPaymentTokenId(paymentTokenId));
        
        assertTrue(exception.getMessage().contains("Failed to query token storage"));
    }

    @Test
    void testGetPaymentCredentials_VerifyApiPath() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        when(tokenStorageRepository.save(any(TokenStorage.class))).thenReturn(new TokenStorage());

        // Act
        paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID);

        // Assert
        String expectedUri = BASE_URL + "/pts/v2/instrumentidentifiers/" + INSTRUMENT_IDENTIFIER_TOKEN_ID + "/networkTokens";
        verify(requestHeadersUriSpec).uri(expectedUri);
        verify(requestHeadersSpec).header("v-c-merchant-id", MERCHANT_ID);
        verify(requestHeadersSpec).header("Authorization", "Bearer " + JWT_TOKEN);
    }

    @Test
    void testGetPaymentCredentials_VerifyTokenStoragePersistence() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        when(tokenStorageRepository.save(any(TokenStorage.class))).thenReturn(new TokenStorage());

        // Act
        paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID);

        // Assert
        verify(tokenStorageRepository).save(argThat(tokenStorage -> 
            tokenStorage.getMerchantId().equals(MERCHANT_ID) &&
            tokenStorage.getPaymentTokenId() != null &&
            tokenStorage.getCryptogram() != null &&
            tokenStorage.getMetadata() != null &&
            tokenStorage.getMetadata().containsKey("instrumentIdentifierTokenId") &&
            tokenStorage.getMetadata().get("instrumentIdentifierTokenId").equals(INSTRUMENT_IDENTIFIER_TOKEN_ID)
        ));
    }
    
    @Test
    void testGetPaymentCredentials_WithDifferentMerchantIds() throws Exception {
        // Test multiple merchant IDs
        String merchantId1 = "merchant-001";
        String merchantId2 = "merchant-002";
        
        // Arrange for first call
        when(jwtTokenUtil.generateJwt(eq(merchantId1), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN + "-1");
        when(jwtTokenUtil.generateJwt(eq(merchantId2), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN + "-2");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        when(tokenStorageRepository.save(any(TokenStorage.class))).thenReturn(new TokenStorage());

        // Act for first merchant
        String result1 = paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, merchantId1);
        
        // Act for second merchant
        String result2 = paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, merchantId2);

        // Assert both calls succeeded
        assertEquals(API_RESPONSE, result1);
        assertEquals(API_RESPONSE, result2);
        
        // Verify JWT tokens were generated for each merchant
        verify(jwtTokenUtil).generateJwt(merchantId1, API_KEY, SECRET_KEY, 
                                       "/pts/v2/instrumentidentifiers/" + INSTRUMENT_IDENTIFIER_TOKEN_ID + "/networkTokens", 
                                       "GET");
        verify(jwtTokenUtil).generateJwt(merchantId2, API_KEY, SECRET_KEY, 
                                       "/pts/v2/instrumentidentifiers/" + INSTRUMENT_IDENTIFIER_TOKEN_ID + "/networkTokens", 
                                       "GET");
        
        // Verify token storage saved with correct merchant IDs
        verify(tokenStorageRepository, times(2)).save(any(TokenStorage.class));
    }
    
    @Test
    void testGetPaymentCredentials_UnexpectedException() throws Exception {
        // Test unexpected exception handling
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.header(anyString(), anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));
        
        // Cause unexpected exception during token storage save
        when(tokenStorageRepository.save(any(TokenStorage.class)))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // Act & Assert
        PaymentCredentialsException exception = assertThrows(PaymentCredentialsException.class, 
                () -> paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));
        
        assertTrue(exception.getMessage().contains("Unexpected error while getting payment credentials"));
    }
}