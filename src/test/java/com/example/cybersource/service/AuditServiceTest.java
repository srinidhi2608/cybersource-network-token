package com.example.cybersource.service;

import com.example.cybersource.dto.UpdateExternalReferenceRequest;
import com.example.cybersource.dto.UpdateExternalReferenceResponse;
import com.example.cybersource.entity.FetchInformationAudit;
import com.example.cybersource.entity.TokenAudit;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.FetchInformationAuditRepository;
import com.example.cybersource.repository.TokenAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private TokenAuditRepository tokenAuditRepository;

    @Mock
    private FetchInformationAuditRepository fetchInformationAuditRepository;

    @InjectMocks
    private AuditService auditService;

    private String testTraceId;
    private String testExternalReference;
    private UpdateExternalReferenceRequest testRequest;

    @BeforeEach
    void setUp() {
        testTraceId = UUID.randomUUID().toString();
        testExternalReference = "new-external-ref-123";
        testRequest = UpdateExternalReferenceRequest.builder()
                .traceId(testTraceId)
                .externalReference(testExternalReference)
                .build();
    }

    @Test
    void testUpdateTokenAuditExternalReference_Success() throws Exception {
        // Arrange
        TokenAudit existingAudit = TokenAudit.builder()
                .id("mongo-id-123")
                .traceId(testTraceId)
                .externalReference("old-external-ref")
                .paymentTokenId("payment-token-123")
                .timestamp(LocalDateTime.now())
                .build();

        when(tokenAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.of(existingAudit));
        when(tokenAuditRepository.save(any(TokenAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UpdateExternalReferenceResponse response = auditService.updateTokenAuditExternalReference(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(testTraceId, response.getTraceId());
        assertEquals(testExternalReference, response.getExternalReference());
        assertEquals("TokenAudit", response.getAuditType());
        assertEquals("External reference updated successfully", response.getMessage());

        verify(tokenAuditRepository).findByTraceId(testTraceId);
        verify(tokenAuditRepository).save(any(TokenAudit.class));
    }

    @Test
    void testUpdateTokenAuditExternalReference_NotFound() {
        // Arrange
        when(tokenAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.empty());

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class, () -> {
            auditService.updateTokenAuditExternalReference(testRequest);
        });

        assertTrue(exception.getMessage().contains("TokenAudit not found for traceId"));
        verify(tokenAuditRepository).findByTraceId(testTraceId);
        verify(tokenAuditRepository, never()).save(any(TokenAudit.class));
    }

    @Test
    void testUpdateFetchInformationAuditExternalReference_Success() throws Exception {
        // Arrange
        FetchInformationAudit existingAudit = FetchInformationAudit.builder()
                .id("mongo-id-456")
                .traceId(testTraceId)
                .externalReference("old-external-ref")
                .paymentTokenId("payment-token-456")
                .informationType("Cryptogram")
                .timestamp(LocalDateTime.now())
                .build();

        when(fetchInformationAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.of(existingAudit));
        when(fetchInformationAuditRepository.save(any(FetchInformationAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UpdateExternalReferenceResponse response = auditService.updateFetchInformationAuditExternalReference(testRequest);

        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(testTraceId, response.getTraceId());
        assertEquals(testExternalReference, response.getExternalReference());
        assertEquals("FetchInformationAudit", response.getAuditType());
        assertEquals("External reference updated successfully", response.getMessage());

        verify(fetchInformationAuditRepository).findByTraceId(testTraceId);
        verify(fetchInformationAuditRepository).save(any(FetchInformationAudit.class));
    }

    @Test
    void testUpdateFetchInformationAuditExternalReference_NotFound() {
        // Arrange
        when(fetchInformationAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.empty());

        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class, () -> {
            auditService.updateFetchInformationAuditExternalReference(testRequest);
        });

        assertTrue(exception.getMessage().contains("FetchInformationAudit not found for traceId"));
        verify(fetchInformationAuditRepository).findByTraceId(testTraceId);
        verify(fetchInformationAuditRepository, never()).save(any(FetchInformationAudit.class));
    }

    @Test
    void testUpdateTokenAuditExternalReference_UpdatesCorrectField() throws Exception {
        // Arrange
        String oldExternalRef = "old-ref";
        TokenAudit existingAudit = TokenAudit.builder()
                .id("mongo-id-789")
                .traceId(testTraceId)
                .externalReference(oldExternalRef)
                .paymentTokenId("payment-token-789")
                .merchantTokenRegistrationId("merchant-123")
                .instrumentIdentifierId("instr-123")
                .timestamp(LocalDateTime.now())
                .build();

        when(tokenAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.of(existingAudit));
        when(tokenAuditRepository.save(any(TokenAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.updateTokenAuditExternalReference(testRequest);

        // Assert - verify the saved audit has the new external reference
        verify(tokenAuditRepository).save(argThat(audit -> 
                testExternalReference.equals(audit.getExternalReference()) &&
                testTraceId.equals(audit.getTraceId())
        ));
    }

    @Test
    void testUpdateFetchInformationAuditExternalReference_UpdatesCorrectField() throws Exception {
        // Arrange
        String oldExternalRef = "old-ref";
        FetchInformationAudit existingAudit = FetchInformationAudit.builder()
                .id("mongo-id-abc")
                .traceId(testTraceId)
                .externalReference(oldExternalRef)
                .paymentTokenId("payment-token-abc")
                .informationType("InstrumentIdentifier")
                .isRequestComplete(true)
                .timestamp(LocalDateTime.now())
                .build();

        when(fetchInformationAuditRepository.findByTraceId(testTraceId))
                .thenReturn(Optional.of(existingAudit));
        when(fetchInformationAuditRepository.save(any(FetchInformationAudit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        auditService.updateFetchInformationAuditExternalReference(testRequest);

        // Assert - verify the saved audit has the new external reference
        verify(fetchInformationAuditRepository).save(argThat(audit -> 
                testExternalReference.equals(audit.getExternalReference()) &&
                testTraceId.equals(audit.getTraceId())
        ));
    }
}
