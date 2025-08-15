package com.example.cybersource.service;

import com.example.cybersource.config.CybersourceConfig;
import com.example.cybersource.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentIdentifierServiceTest {

    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    
    @Mock
    private WebClient.RequestBodySpec requestBodySpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    @Mock
    private CybersourceConfig cybersourceConfig;
    
    @Mock
    private JwtTokenUtil jwtTokenUtil;

    @InjectMocks
    private InstrumentIdentifierService instrumentIdentifierService;

    private static final String CARD_NUMBER = "4111111111111111";
    private static final String MERCHANT_ID = "test-merchant-123";
    private static final String INSTRUMENT_IDENTIFIER_TOKEN_ID = "test-instrument-id";
    private static final String JWT_TOKEN = "test-jwt-token";
    private static final String API_RESPONSE = "{\"id\":\"" + INSTRUMENT_IDENTIFIER_TOKEN_ID + "\"}";
    private static final String BASE_URL = "https://api.cybersource.com";
    private static final String API_KEY = "test-api-key";
    private static final String SECRET_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        lenient().when(cybersourceConfig.getBaseUrl()).thenReturn(BASE_URL);
        lenient().when(cybersourceConfig.getApiKey()).thenReturn(API_KEY);
        lenient().when(cybersourceConfig.getSecretKey()).thenReturn(SECRET_KEY);
    }

    @Test
    void testCreateInstrumentIdentifier_Success() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));

        // Act
        String result = instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, MERCHANT_ID);

        // Assert
        assertEquals(API_RESPONSE, result);
        verify(jwtTokenUtil).generateJwt(MERCHANT_ID, API_KEY, SECRET_KEY, "/pts/v2/instrumentidentifiers", "POST");
        verify(requestBodyUriSpec).uri(BASE_URL + "/pts/v2/instrumentidentifiers");
        verify(requestBodySpec).header("v-c-merchant-id", MERCHANT_ID);
        verify(requestBodySpec).header("Authorization", "Bearer " + JWT_TOKEN);
        verify(requestBodySpec).bodyValue(contains(CARD_NUMBER));
    }

    @Test
    void testCreateInstrumentIdentifier_CybersourceApiException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        WebClientResponseException webClientException = WebClientResponseException.create(
                400, "Bad Request", null, "Invalid card number".getBytes(), null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientException));

        // Act & Assert
        CybersourceApiException exception = assertThrows(CybersourceApiException.class,
                () -> instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, MERCHANT_ID));

        assertEquals(400, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to create instrument identifier"));
        assertTrue(exception.getResponseBody().contains("Invalid card number"));
    }

    @Test
    void testCreateInstrumentIdentifier_NetworkException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        WebClientException webClientException = new WebClientException("Connection timeout") {};
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientException));

        // Act & Assert
        NetworkException exception = assertThrows(NetworkException.class,
                () -> instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, MERCHANT_ID));

        assertTrue(exception.getMessage().contains("Network error while calling Cybersource API"));
    }

    @Test
    void testCreateInstrumentIdentifier_UnexpectedException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class,
                () -> instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, MERCHANT_ID));

        assertTrue(exception.getMessage().contains("Unexpected error while creating instrument identifier"));
    }

    @Test
    void testGetInstrumentIdentifier_Success() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(mock(WebClient.RequestHeadersUriSpec.class));
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = webClient.get();
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(mock(WebClient.RequestHeadersSpec.class));
        WebClient.RequestHeadersSpec requestHeadersSpecMock = requestHeadersUriSpec.uri(anyString());
        when(requestHeadersSpecMock.header(anyString(), anyString())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));

        // Act
        String result = instrumentIdentifierService.getInstrumentIdentifier(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID);

        // Assert
        assertEquals(API_RESPONSE, result);
        verify(jwtTokenUtil).generateJwt(MERCHANT_ID, API_KEY, SECRET_KEY, 
                                       "/pts/v2/instrumentidentifiers/" + INSTRUMENT_IDENTIFIER_TOKEN_ID, "GET");
    }

    @Test
    void testGetInstrumentIdentifier_CybersourceApiException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(mock(WebClient.RequestHeadersUriSpec.class));
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = webClient.get();
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(mock(WebClient.RequestHeadersSpec.class));
        WebClient.RequestHeadersSpec requestHeadersSpecMock = requestHeadersUriSpec.uri(anyString());
        when(requestHeadersSpecMock.header(anyString(), anyString())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpec);

        WebClientResponseException webClientException = WebClientResponseException.create(
                404, "Not Found", null, "Instrument identifier not found".getBytes(), null);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientException));

        // Act & Assert
        CybersourceApiException exception = assertThrows(CybersourceApiException.class,
                () -> instrumentIdentifierService.getInstrumentIdentifier(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));

        assertEquals(404, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Failed to get instrument identifier"));
        assertTrue(exception.getResponseBody().contains("Instrument identifier not found"));
    }

    @Test
    void testGetInstrumentIdentifier_NetworkException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.get()).thenReturn(mock(WebClient.RequestHeadersUriSpec.class));
        WebClient.RequestHeadersUriSpec requestHeadersUriSpec = webClient.get();
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(mock(WebClient.RequestHeadersSpec.class));
        WebClient.RequestHeadersSpec requestHeadersSpecMock = requestHeadersUriSpec.uri(anyString());
        when(requestHeadersSpecMock.header(anyString(), anyString())).thenReturn(requestHeadersSpecMock);
        when(requestHeadersSpecMock.retrieve()).thenReturn(responseSpec);

        WebClientException webClientException = new WebClientException("Connection timeout") {};
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(webClientException));

        // Act & Assert
        NetworkException exception = assertThrows(NetworkException.class,
                () -> instrumentIdentifierService.getInstrumentIdentifier(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));

        assertTrue(exception.getMessage().contains("Network error while calling Cybersource API"));
    }

    @Test
    void testGetInstrumentIdentifier_UnexpectedException() throws Exception {
        // Arrange
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class,
                () -> instrumentIdentifierService.getInstrumentIdentifier(INSTRUMENT_IDENTIFIER_TOKEN_ID, MERCHANT_ID));

        assertTrue(exception.getMessage().contains("Unexpected error while getting instrument identifier"));
    }

    @Test
    void testCreateInstrumentIdentifier_WithDifferentMerchantIds() throws Exception {
        // Test multiple merchant IDs
        String merchantId1 = "merchant-001";
        String merchantId2 = "merchant-002";
        
        // Arrange for first call
        when(jwtTokenUtil.generateJwt(eq(merchantId1), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN + "-1");
        when(jwtTokenUtil.generateJwt(eq(merchantId2), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN + "-2");

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));

        // Act
        String result1 = instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, merchantId1);
        String result2 = instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER, merchantId2);

        // Assert
        assertEquals(API_RESPONSE, result1);
        assertEquals(API_RESPONSE, result2);
        
        verify(jwtTokenUtil).generateJwt(merchantId1, API_KEY, SECRET_KEY, "/pts/v2/instrumentidentifiers", "POST");
        verify(jwtTokenUtil).generateJwt(merchantId2, API_KEY, SECRET_KEY, "/pts/v2/instrumentidentifiers", "POST");
    }

    @Test
    void testCreateInstrumentIdentifier_ValidatesCardNumberInPayload() throws Exception {
        // Arrange
        String longCardNumber = "4111111111111111";
        when(jwtTokenUtil.generateJwt(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(JWT_TOKEN);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(API_RESPONSE));

        // Act
        instrumentIdentifierService.createInstrumentIdentifier(longCardNumber, MERCHANT_ID);

        // Assert
        // Verify that the card number is properly included in the JSON payload
        verify(requestBodySpec).bodyValue(argThat(payload -> 
            payload.toString().contains(longCardNumber) && 
            payload.toString().contains("\"card\"") &&
            payload.toString().contains("\"number\"")
        ));
    }
}