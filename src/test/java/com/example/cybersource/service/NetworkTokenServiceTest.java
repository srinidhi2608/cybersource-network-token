package com.example.cybersource.service;

import com.example.cybersource.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NetworkTokenServiceTest {

    @Mock
    private InstrumentIdentifierService instrumentIdentifierService;
    
    @Mock
    private PaymentCredentialsService paymentCredentialsService;

    @InjectMocks
    private NetworkTokenService networkTokenService;

    private static final String CARD_NUMBER = "4111111111111111";
    private static final String INSTRUMENT_IDENTIFIER_ID = "test-instrument-id";
    private static final String INSTRUMENT_RESPONSE = "{\"id\":\"" + INSTRUMENT_IDENTIFIER_ID + "\"}";
    private static final String CREDENTIALS_RESPONSE = "{\"networkToken\":{\"number\":\"1234567890123456\",\"cryptogram\":\"test-cryptogram\"}}";

    @Test
    void testGenerateNetworkTokenAndCryptogram_Success() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenReturn(CREDENTIALS_RESPONSE);

        // Act
        NetworkTokenService.NetworkTokenResult result = 
                networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER);

        // Assert
        assertNotNull(result);
        assertEquals("1234567890123456", result.networkToken());
        assertEquals("test-cryptogram", result.cryptogram());
        assertTrue(result.elapsedMilliseconds() >= 0);
        
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_InstrumentIdentifierFailure() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenThrow(new RuntimeException("Instrument identifier creation failed"));

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertTrue(exception.getMessage().contains("Unexpected error while generating network token and cryptogram"));
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService, never()).getPaymentCredentials(anyString());
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_PaymentCredentialsException() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenThrow(new PaymentCredentialsException("Payment credentials failed"));

        // Act & Assert
        PaymentCredentialsException exception = assertThrows(PaymentCredentialsException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertEquals("Payment credentials failed", exception.getMessage());
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_NetworkException() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenThrow(new NetworkException("Network error"));

        // Act & Assert
        NetworkException exception = assertThrows(NetworkException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertEquals("Network error", exception.getMessage());
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_DataAccessException() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenThrow(new DataAccessException("Database error"));

        // Act & Assert
        DataAccessException exception = assertThrows(DataAccessException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertEquals("Database error", exception.getMessage());
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_CybersourceApiException() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenThrow(new CybersourceApiException("API error", 400, "Bad request"));

        // Act & Assert
        CybersourceApiException exception = assertThrows(CybersourceApiException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertEquals("API error", exception.getMessage());
        assertEquals(400, exception.getStatusCode());
        assertEquals("Bad request", exception.getResponseBody());
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_InvalidInstrumentResponse() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn("{\"invalid\":\"response\"}");

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertTrue(exception.getMessage().contains("Unexpected error while generating network token and cryptogram"));
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService, never()).getPaymentCredentials(anyString());
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_InvalidCredentialsResponse() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenReturn("{\"invalid\":\"response\"}");

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class,
                () -> networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER));
        
        assertTrue(exception.getMessage().contains("Unexpected error while generating network token and cryptogram"));
        verify(instrumentIdentifierService).createInstrumentIdentifier(CARD_NUMBER);
        verify(paymentCredentialsService).getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID);
    }

    @Test
    void testGenerateNetworkTokenAndCryptogram_TimingMeasurement() throws Exception {
        // Arrange
        when(instrumentIdentifierService.createInstrumentIdentifier(CARD_NUMBER))
                .thenReturn(INSTRUMENT_RESPONSE);
        when(paymentCredentialsService.getPaymentCredentials(INSTRUMENT_IDENTIFIER_ID))
                .thenReturn(CREDENTIALS_RESPONSE);

        // Act
        long startTime = System.currentTimeMillis();
        NetworkTokenService.NetworkTokenResult result = 
                networkTokenService.generateNetworkTokenAndCryptogram(CARD_NUMBER);
        long endTime = System.currentTimeMillis();

        // Assert
        assertNotNull(result);
        assertTrue(result.elapsedMilliseconds() >= 0);
        assertTrue(result.elapsedMilliseconds() <= (endTime - startTime + 10)); // Allow small margin for timing differences
    }

    @Test
    void testNetworkTokenResult_RecordProperties() {
        // Arrange
        String networkToken = "1234567890123456";
        String cryptogram = "test-cryptogram";
        long elapsedMs = 1500;

        // Act
        NetworkTokenService.NetworkTokenResult result = 
                new NetworkTokenService.NetworkTokenResult(networkToken, cryptogram, elapsedMs);

        // Assert
        assertEquals(networkToken, result.networkToken());
        assertEquals(cryptogram, result.cryptogram());
        assertEquals(elapsedMs, result.elapsedMilliseconds());
    }
}