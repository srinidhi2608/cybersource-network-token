package com.example.cybersource.service;

import com.example.cybersource.dto.CreateCryptogramRequest;
import com.example.cybersource.dto.CreateCryptogramResponse;
import com.example.cybersource.dto.CreateNetworkTokenRequest;
import com.example.cybersource.dto.CreateNetworkTokenResponse;
import com.example.cybersource.entity.*;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NetworkTokenService.
 */
@ExtendWith(MockitoExtension.class)
class NetworkTokenServiceTest {

    @Mock
    private MerchantEnrollResponseRepository merchantRepository;

    @Mock
    private TokenTransactionRepository tokenTransactionRepository;

    @Mock
    private TokenAuditRepository tokenAuditRepository;

    @Mock
    private FetchInformationAuditRepository fetchInformationAuditRepository;

    @Mock
    private InstrumentIdentifierService instrumentIdentifierService;

    @Mock
    private CybersourceRestClient cybersourceRestClient;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private NetworkTokenService networkTokenService;

    private MerchantEnrollResponse mockMerchant;
    private CreateNetworkTokenRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockMerchant = MerchantEnrollResponse.builder()
                .merchantTokenRegistrationId("merchant-123")
                .transactingOrgId("org-123")
                .merchantName("Test Merchant")
                .status(MerchantEnrollResponse.MerchantStatus.ACTIVE)
                .enrolledAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        mockRequest = CreateNetworkTokenRequest.builder()
                .merchantTokenRegistrationId("merchant-123")
                .cardNumber("4111111111111111")
                .cardExpiryMonth("12")
                .cardExpiryYear("2025")
                .externalReference("ref-123")
                .build();
    }

    @Test
    void testCreateNetworkToken_Success() throws Exception {
        // Arrange
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.of(mockMerchant));
        
        TokenAudit mockAudit = TokenAudit.builder().build();
        when(tokenAuditRepository.save(any(TokenAudit.class))).thenReturn(mockAudit);
        
        when(instrumentIdentifierService.createInstrumentIdentifier(anyString(), anyString()))
                .thenReturn("{\"id\":\"instr-123\"}");
        
        when(tokenTransactionRepository.findByInstrumentIdentifierId(anyString()))
                .thenReturn(Optional.empty());
        
        when(cybersourceRestClient.get(contains("/networkTokens"), anyString()))
                .thenReturn("{\"networkToken\":{\"number\":\"4111000011110000\",\"par\":\"PAR123\",\"expirationMonth\":\"12\",\"expirationYear\":\"2025\",\"status\":\"ACTIVE\"}}");
        
        when(cybersourceRestClient.get(contains("/paymentCredentials"), anyString()))
                .thenReturn("{\"networkToken\":{\"cryptogram\":\"ABC123DEF456\"}}");
        
        when(encryptionService.encrypt(anyString())).thenReturn("encrypted-token");
        
        when(tokenTransactionRepository.save(any(TokenTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        when(fetchInformationAuditRepository.save(any(FetchInformationAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateNetworkTokenResponse response = networkTokenService.createNetworkToken(mockRequest);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getPaymentTokenId());
        assertEquals("4111000011110000", response.getNetworkToken());
        assertEquals("ABC123DEF456", response.getCryptogram());
        assertEquals("PAR123", response.getPar());
        assertEquals("12", response.getTokenExpiryMonth());
        assertEquals("2025", response.getTokenExpiryYear());
        assertEquals("ACTIVE", response.getTokenStatus());
        assertEquals("instr-123", response.getInstrumentIdentifierId());

        verify(merchantRepository).findByMerchantTokenRegistrationId("merchant-123");
        verify(tokenAuditRepository, times(2)).save(any(TokenAudit.class));
        verify(tokenTransactionRepository).save(any(TokenTransaction.class));
        verify(fetchInformationAuditRepository).save(any(FetchInformationAudit.class));
    }

    @Test
    void testCreateNetworkToken_MerchantNotFound() {
        // Arrange
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CybersourceException.class, () -> {
            networkTokenService.createNetworkToken(mockRequest);
        });

        verify(merchantRepository).findByMerchantTokenRegistrationId("merchant-123");
        verify(tokenAuditRepository, never()).save(any(TokenAudit.class));
    }

    @Test
    void testCreateNetworkToken_MerchantInactive() {
        // Arrange
        mockMerchant.setStatus(MerchantEnrollResponse.MerchantStatus.INACTIVE);
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.of(mockMerchant));

        // Act & Assert
        assertThrows(CybersourceException.class, () -> {
            networkTokenService.createNetworkToken(mockRequest);
        });

        verify(merchantRepository).findByMerchantTokenRegistrationId("merchant-123");
    }

    @Test
    void testCreateNetworkToken_DuplicateDetection() throws Exception {
        // Arrange
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.of(mockMerchant));
        
        TokenAudit mockAudit = TokenAudit.builder().build();
        when(tokenAuditRepository.save(any(TokenAudit.class))).thenReturn(mockAudit);
        
        when(instrumentIdentifierService.createInstrumentIdentifier(anyString(), anyString()))
                .thenReturn("{\"id\":\"instr-123\"}");
        
        TokenTransaction existingToken = TokenTransaction.builder()
                .paymentTokenId("existing-token-id")
                .merchantTokenRegistrationId("merchant-123")
                .networkToken("encrypted-existing-token")
                .instrumentIdentifierId("instr-123")
                .par("PAR123")
                .tokenExpiryMonth("12")
                .tokenExpiryYear("2025")
                .tokenStatus("ACTIVE")
                .build();
        
        when(tokenTransactionRepository.findByInstrumentIdentifierId(anyString()))
                .thenReturn(Optional.of(existingToken));
        
        when(cybersourceRestClient.get(contains("/paymentCredentials"), anyString()))
                .thenReturn("{\"networkToken\":{\"cryptogram\":\"ABC123DEF456\"}}");
        
        when(encryptionService.decrypt(anyString())).thenReturn("4111000011110000");
        
        when(fetchInformationAuditRepository.save(any(FetchInformationAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateNetworkTokenResponse response = networkTokenService.createNetworkToken(mockRequest);

        // Assert
        assertNotNull(response);
        assertEquals("existing-token-id", response.getPaymentTokenId());
        assertEquals("4111000011110000", response.getNetworkToken());
        assertEquals("ABC123DEF456", response.getCryptogram());

        verify(tokenTransactionRepository).findByInstrumentIdentifierId("instr-123");
        verify(tokenTransactionRepository, never()).save(any(TokenTransaction.class));
        verify(tokenAuditRepository, times(2)).save(any(TokenAudit.class));
    }

    @Test
    void testCreateCryptogram_Success() throws Exception {
        // Arrange
        String paymentTokenId = "token-123";
        CreateCryptogramRequest request = CreateCryptogramRequest.builder()
                .paymentTokenId(paymentTokenId)
                .externalReference("ref-456")
                .build();
        
        TokenTransaction mockToken = TokenTransaction.builder()
                .paymentTokenId(paymentTokenId)
                .merchantTokenRegistrationId("merchant-123")
                .instrumentIdentifierId("instr-123")
                .build();
        
        when(tokenTransactionRepository.findByPaymentTokenId(paymentTokenId))
                .thenReturn(Optional.of(mockToken));
        
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.of(mockMerchant));
        
        when(cybersourceRestClient.get(contains("/paymentCredentials"), anyString()))
                .thenReturn("{\"networkToken\":{\"cryptogram\":\"XYZ789ABC123\"}}");
        
        when(fetchInformationAuditRepository.save(any(FetchInformationAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CreateCryptogramResponse response = networkTokenService.createCryptogram(request);

        // Assert
        assertNotNull(response);
        assertEquals("XYZ789ABC123", response.getCryptogram());
        assertEquals(paymentTokenId, response.getPaymentTokenId());

        verify(tokenTransactionRepository).findByPaymentTokenId(paymentTokenId);
        verify(fetchInformationAuditRepository).save(any(FetchInformationAudit.class));
    }

    @Test
    void testCreateCryptogram_TokenNotFound() {
        // Arrange
        String paymentTokenId = "non-existent-token";
        CreateCryptogramRequest request = CreateCryptogramRequest.builder()
                .paymentTokenId(paymentTokenId)
                .externalReference("ref-456")
                .build();
        
        when(tokenTransactionRepository.findByPaymentTokenId(paymentTokenId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CybersourceException.class, () -> {
            networkTokenService.createCryptogram(request);
        });

        verify(tokenTransactionRepository).findByPaymentTokenId(paymentTokenId);
        verify(fetchInformationAuditRepository, never()).save(any(FetchInformationAudit.class));
    }

    @Test
    void testCreateNetworkToken_FailureRecordsAudit() throws Exception {
        // Arrange
        when(merchantRepository.findByMerchantTokenRegistrationId(anyString()))
                .thenReturn(Optional.of(mockMerchant));
        
        TokenAudit mockAudit = TokenAudit.builder().build();
        when(tokenAuditRepository.save(any(TokenAudit.class))).thenReturn(mockAudit);
        
        when(instrumentIdentifierService.createInstrumentIdentifier(anyString(), anyString()))
                .thenThrow(new RuntimeException("Cybersource API error"));

        // Act & Assert
        assertThrows(CybersourceException.class, () -> {
            networkTokenService.createNetworkToken(mockRequest);
        });

        // Verify audit was saved with failure information
        verify(tokenAuditRepository, times(2)).save(any(TokenAudit.class));
    }
}
