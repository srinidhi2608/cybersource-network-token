package com.example.cybersource.service;

import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.entity.*;
import com.example.cybersource.enums.EventReference;
import com.example.cybersource.enums.EventType;
import com.example.cybersource.enums.TraceIdEventType;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.*;
import com.example.cybersource.util.BillingReferenceNumberGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingAuditSyncService.
 */
@ExtendWith(MockitoExtension.class)
class BillingAuditSyncServiceTest {
    
    @Mock
    private TokenAuditRepository tokenAuditRepository;
    
    @Mock
    private FetchInformationAuditRepository fetchInformationAuditRepository;
    
    @Mock
    private TokenEventsRepository tokenEventsRepository;
    
    @Mock
    private BillingAuditRepository billingAuditRepository;
    
    @Mock
    private BillingAuditSyncInformationRepository syncInformationRepository;
    
    @Mock
    private BillingReferenceNumberGenerator referenceNumberGenerator;
    
    @InjectMocks
    private BillingAuditSyncService billingAuditSyncService;
    
    @BeforeEach
    void setUp() {
        // Set configuration values using reflection
        ReflectionTestUtils.setField(billingAuditSyncService, "syncOffsetMinutes", 5);
        ReflectionTestUtils.setField(billingAuditSyncService, "batchSize", 1000);
    }
    
    @Test
    void testSyncBillingAudit_Success_WithExistingSyncInfo() throws CybersourceException {
        // Arrange
        LocalDateTime lastSync = LocalDateTime.now().minusHours(1);
        BillingAuditSyncInformation syncInfo = createSyncInfo(lastSync);
        
        List<TokenAudit> tokenAudits = createTokenAudits(2);
        List<FetchInformationAudit> fetchInfoAudits = createFetchInfoAudits(2);
        List<TokenEvents> tokenEvents = createTokenEvents(1);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(syncInfo);
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(tokenAudits);
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(tokenEvents);
        when(referenceNumberGenerator.generateUniqueReferenceNumber())
            .thenReturn("BILL-1234567890-00000001", "BILL-1234567890-00000002",
                       "BILL-1234567890-00000003", "BILL-1234567890-00000004",
                       "BILL-1234567890-00000005");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(5, response.getTotalRecordsCreated());
        assertEquals(2, response.getTokenAuditCount());
        assertEquals(2, response.getFetchInfoAuditCount());
        assertEquals(1, response.getTokenEventsCount());
        assertNotNull(response.getBillingBatchNumber());
        assertNotNull(response.getSyncStartTime());
        assertNotNull(response.getSyncEndTime());
        
        verify(billingAuditRepository).saveAll(anyList());
        verify(syncInformationRepository, times(2)).save(any());
    }
    
    @Test
    void testSyncBillingAudit_Success_FirstTimeSync() throws CybersourceException {
        // Arrange
        List<TokenAudit> tokenAudits = createTokenAudits(1);
        List<FetchInformationAudit> fetchInfoAudits = createFetchInfoAudits(1);
        List<TokenEvents> tokenEvents = new ArrayList<>();
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(null);
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(tokenAudits);
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(tokenEvents);
        when(referenceNumberGenerator.generateUniqueReferenceNumber())
            .thenReturn("BILL-1234567890-00000001", "BILL-1234567890-00000002");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(2, response.getTotalRecordsCreated());
        
        verify(syncInformationRepository, times(2)).save(any());
    }
    
    @Test
    void testSyncBillingAudit_WithCustomTimeWindow() throws CybersourceException {
        // Arrange
        LocalDateTime customStart = LocalDateTime.now().minusDays(1);
        LocalDateTime customEnd = LocalDateTime.now().minusHours(2);
        
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
                .startTime(customStart)
                .endTime(customEnd)
                .build();
        
        BillingAuditSyncInformation syncInfo = createSyncInfo(LocalDateTime.now().minusHours(1));
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(syncInfo);
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(0, response.getTotalRecordsCreated());
        assertEquals(customStart, response.getSyncStartTime());
        assertEquals(customEnd, response.getSyncEndTime());
    }
    
    @Test
    void testSyncBillingAudit_TokenAuditMapping() throws CybersourceException {
        // Arrange
        TokenAudit tokenAudit = createTokenAudit("trace-1", true, null);
        List<TokenAudit> tokenAudits = Arrays.asList(tokenAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(tokenAudits);
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            assertEquals("trace-1", audit.getTraceIdReference());
            assertEquals(TraceIdEventType.NETWORK_TOKEN_PROCESSING.getDisplayName(), audit.getTraceIdEvent());
            assertEquals(EventType.SUCCESS.getDisplayName(), audit.getEventType());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        verify(billingAuditRepository).saveAll(anyList());
    }
    
    @Test
    void testSyncBillingAudit_FetchInfoAuditMapping_FetchCryptogram() throws CybersourceException {
        // Arrange
        FetchInformationAudit fetchInfoAudit = createFetchInfoAudit("trace-2", "FetchCryptogram", true);
        List<FetchInformationAudit> fetchInfoAudits = Arrays.asList(fetchInfoAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            assertEquals("trace-2", audit.getTraceIdReference());
            assertEquals(TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName(), audit.getTraceIdEvent());
            assertEquals(EventType.SUCCESS.getDisplayName(), audit.getEventType());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        verify(billingAuditRepository).saveAll(anyList());
    }
    
    @Test
    void testSyncBillingAudit_FetchInfoAuditMapping_LCM() throws CybersourceException {
        // Arrange
        FetchInformationAudit fetchInfoAudit = createFetchInfoAudit("trace-3", "LCM", true);
        List<FetchInformationAudit> fetchInfoAudits = Arrays.asList(fetchInfoAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            BillingAudit audit = audits.get(0);
            assertEquals(TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName(), audit.getTraceIdEvent());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
    }
    
    @Test
    void testSyncBillingAudit_TokenEventsMapping() throws CybersourceException {
        // Arrange
        TokenEvents tokenEvent = createTokenEvent("trace-4");
        List<TokenEvents> tokenEvents = Arrays.asList(tokenEvent);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(tokenEvents);
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            BillingAudit audit = audits.get(0);
            assertEquals("trace-4", audit.getTraceIdReference());
            assertEquals(TraceIdEventType.TOKEN_LIFECYCLE_MANAGEMENT.getDisplayName(), audit.getTraceIdEvent());
            assertEquals(EventType.SUCCESS.getDisplayName(), audit.getEventType());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
    }
    
    @Test
    void testSyncBillingAudit_FailedRequest_MapsToFailure() throws CybersourceException {
        // Arrange
        TokenAudit failedAudit = createTokenAudit("trace-5", false, "API Error");
        List<TokenAudit> tokenAudits = Arrays.asList(failedAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(tokenAudits);
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            BillingAudit audit = audits.get(0);
            assertEquals(EventType.FAILURE.getDisplayName(), audit.getEventType());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
    }
    
    @Test
    void testSyncBillingAudit_OptimisticLockingFailure() {
        // Arrange
        when(syncInformationRepository.findSyncInfo())
            .thenThrow(new OptimisticLockingFailureException("Concurrent modification"));
        
        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class, () -> {
            billingAuditSyncService.syncBillingAudit(null);
        });
        
        assertTrue(exception.getMessage().contains("Another sync operation is in progress"));
    }
    
    @Test
    void testSyncBillingAudit_EmptyDataSets() throws CybersourceException {
        // Arrange
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(0, response.getTotalRecordsCreated());
        verify(billingAuditRepository, never()).saveAll(anyList());
    }
    
    @Test
    void testSyncBillingAudit_UnexpectedError() {
        // Arrange
        when(syncInformationRepository.findSyncInfo())
            .thenThrow(new RuntimeException("Database connection error"));
        
        // Act & Assert
        CybersourceException exception = assertThrows(CybersourceException.class, () -> {
            billingAuditSyncService.syncBillingAudit(null);
        });
        
        assertTrue(exception.getMessage().contains("Billing audit sync failed"));
    }
    
    // Helper methods
    
    private BillingAuditSyncInformation createSyncInfo(LocalDateTime lastSync) {
        return BillingAuditSyncInformation.builder()
                .id("sync-info-1")
                .lastSyncTimestamp(lastSync)
                .lastSyncStatus("SUCCESS")
                .version(1L)
                .build();
    }
    
    private List<TokenAudit> createTokenAudits(int count) {
        List<TokenAudit> audits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            audits.add(createTokenAudit("trace-ta-" + i, true, null));
        }
        return audits;
    }
    
    private TokenAudit createTokenAudit(String traceId, boolean isComplete, String failureReason) {
        return TokenAudit.builder()
                .id("audit-" + traceId)
                .traceId(traceId)
                .paymentTokenId("payment-token-1")
                .merchantTokenRegistrationId("merchant-1")
                .instrumentIdentifierId("instrument-1")
                .isRequestComplete(isComplete)
                .failureReason(failureReason)
                .isDuplicate(false)
                .timestamp(LocalDateTime.now())
                .externalReference("ext-ref-1")
                .build();
    }
    
    private List<FetchInformationAudit> createFetchInfoAudits(int count) {
        List<FetchInformationAudit> audits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            audits.add(createFetchInfoAudit("trace-fia-" + i, "FetchCryptogram", true));
        }
        return audits;
    }
    
    private FetchInformationAudit createFetchInfoAudit(String traceId, String eventReference, boolean isComplete) {
        return FetchInformationAudit.builder()
                .id("fetch-audit-" + traceId)
                .traceId(traceId)
                .paymentTokenId("payment-token-1")
                .informationType("Cryptogram")
                .eventReference(eventReference)
                .isRequestComplete(isComplete)
                .timestamp(LocalDateTime.now())
                .externalReference("ext-ref-1")
                .build();
    }
    
    private List<TokenEvents> createTokenEvents(int count) {
        List<TokenEvents> events = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            events.add(createTokenEvent("trace-te-" + i));
        }
        return events;
    }
    
    private TokenEvents createTokenEvent(String traceId) {
        return TokenEvents.builder()
                .id("event-" + traceId)
                .traceId(traceId)
                .paymentTokenId("payment-token-1")
                .lifecycleEventName("TOKEN_UPDATED")
                .tokenStatus("ACTIVE")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
