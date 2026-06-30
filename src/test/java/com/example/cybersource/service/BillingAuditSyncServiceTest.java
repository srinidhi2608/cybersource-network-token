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
    private TokenTransactionRepository tokenTransactionRepository;
    
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
    
    // ========== NEW TESTS FOR MERCHANT ID LOOKUP ==========
    
    @Test
    void testSyncBillingAudit_FetchInfoAudit_WithMerchantIdLookup() throws CybersourceException {
        // Arrange - FetchInformationAudit needs merchantTokenRegistrationId from TokenTransaction
        FetchInformationAudit fetchInfoAudit = createFetchInfoAudit("trace-fetch-1", "FetchCryptogram", true);
        fetchInfoAudit.setPaymentTokenId("payment-token-100");
        List<FetchInformationAudit> fetchInfoAudits = Arrays.asList(fetchInfoAudit);
        
        // Create matching TokenTransaction with merchantTokenRegistrationId
        TokenTransaction tokenTransaction = TokenTransaction.builder()
                .id("tt-1")
                .paymentTokenId("payment-token-100")
                .merchantTokenRegistrationId("merchant-ABC")
                .instrumentIdentifierId("instrument-100")
                .build();
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenTransactionRepository.findByPaymentTokenIdIn(anyList())).thenReturn(Arrays.asList(tokenTransaction));
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000001");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            assertEquals("merchant-ABC", audit.getMerchantTokenRegistrationId());
            assertEquals("payment-token-100", audit.getPaymentTokenId());
            assertEquals(TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName(), audit.getTraceIdEvent());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalRecordsCreated());
        verify(tokenTransactionRepository).findByPaymentTokenIdIn(anyList());
    }
    
    @Test
    void testSyncBillingAudit_TokenEvent_WithMerchantIdLookup() throws CybersourceException {
        // Arrange - TokenEvent needs merchantTokenRegistrationId from TokenTransaction
        TokenEvents tokenEvent = createTokenEvent("trace-event-1");
        tokenEvent.setPaymentTokenId("payment-token-200");
        List<TokenEvents> tokenEvents = Arrays.asList(tokenEvent);
        
        // Create matching TokenTransaction with merchantTokenRegistrationId
        TokenTransaction tokenTransaction = TokenTransaction.builder()
                .id("tt-2")
                .paymentTokenId("payment-token-200")
                .merchantTokenRegistrationId("merchant-XYZ")
                .instrumentIdentifierId("instrument-200")
                .build();
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(tokenEvents);
        when(tokenTransactionRepository.findByPaymentTokenIdIn(anyList())).thenReturn(Arrays.asList(tokenTransaction));
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000002");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            assertEquals("merchant-XYZ", audit.getMerchantTokenRegistrationId());
            assertEquals("payment-token-200", audit.getPaymentTokenId());
            assertEquals(TraceIdEventType.TOKEN_LIFECYCLE_MANAGEMENT.getDisplayName(), audit.getTraceIdEvent());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalRecordsCreated());
        verify(tokenTransactionRepository).findByPaymentTokenIdIn(anyList());
    }
    
    @Test
    void testSyncBillingAudit_NoMatchingTokenTransaction_LogsWarning() throws CybersourceException {
        // Arrange - FetchInformationAudit without matching TokenTransaction
        FetchInformationAudit fetchInfoAudit = createFetchInfoAudit("trace-fetch-orphan", "FetchCryptogram", true);
        fetchInfoAudit.setPaymentTokenId("payment-token-orphan");
        List<FetchInformationAudit> fetchInfoAudits = Arrays.asList(fetchInfoAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenTransactionRepository.findByPaymentTokenIdIn(anyList())).thenReturn(new ArrayList<>()); // No match
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000003");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            // MerchantTokenRegistrationId should be null when no match found
            assertNull(audit.getMerchantTokenRegistrationId());
            assertEquals("payment-token-orphan", audit.getPaymentTokenId());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalRecordsCreated());
        verify(tokenTransactionRepository).findByPaymentTokenIdIn(anyList());
    }
    
    @Test
    void testSyncBillingAudit_BatchLookup_MultipleRecords() throws CybersourceException {
        // Arrange - Multiple FetchInfoAudits and TokenEvents requiring batch lookup
        FetchInformationAudit fetchInfo1 = createFetchInfoAudit("trace-f1", "FetchCryptogram", true);
        fetchInfo1.setPaymentTokenId("payment-token-301");
        FetchInformationAudit fetchInfo2 = createFetchInfoAudit("trace-f2", "LCM", true);
        fetchInfo2.setPaymentTokenId("payment-token-302");
        List<FetchInformationAudit> fetchInfoAudits = Arrays.asList(fetchInfo1, fetchInfo2);
        
        TokenEvents event1 = createTokenEvent("trace-e1");
        event1.setPaymentTokenId("payment-token-303");
        TokenEvents event2 = createTokenEvent("trace-e2");
        event2.setPaymentTokenId("payment-token-304");
        List<TokenEvents> tokenEvents = Arrays.asList(event1, event2);
        
        // Create matching TokenTransactions
        List<TokenTransaction> tokenTransactions = Arrays.asList(
                TokenTransaction.builder()
                        .paymentTokenId("payment-token-301")
                        .merchantTokenRegistrationId("merchant-M1")
                        .build(),
                TokenTransaction.builder()
                        .paymentTokenId("payment-token-302")
                        .merchantTokenRegistrationId("merchant-M2")
                        .build(),
                TokenTransaction.builder()
                        .paymentTokenId("payment-token-303")
                        .merchantTokenRegistrationId("merchant-M3")
                        .build(),
                TokenTransaction.builder()
                        .paymentTokenId("payment-token-304")
                        .merchantTokenRegistrationId("merchant-M4")
                        .build()
        );
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(fetchInfoAudits);
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(tokenEvents);
        when(tokenTransactionRepository.findByPaymentTokenIdIn(anyList())).thenReturn(tokenTransactions);
        when(referenceNumberGenerator.generateUniqueReferenceNumber())
                .thenReturn("BILL-1", "BILL-2", "BILL-3", "BILL-4");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(4, audits.size());
            // Verify all have merchantTokenRegistrationId populated
            assertTrue(audits.stream().allMatch(a -> a.getMerchantTokenRegistrationId() != null));
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(4, response.getTotalRecordsCreated());
        // Verify only ONE batch query was made (not 4 separate queries)
        verify(tokenTransactionRepository, times(1)).findByPaymentTokenIdIn(anyList());
    }
    
    @Test
    void testSyncBillingAudit_TokenAudit_DoesNotRequireLookup() throws CybersourceException {
        // Arrange - TokenAudit already has merchantTokenRegistrationId
        TokenAudit tokenAudit = createTokenAudit("trace-ta-1", true, null);
        tokenAudit.setMerchantTokenRegistrationId("merchant-direct");
        List<TokenAudit> tokenAudits = Arrays.asList(tokenAudit);
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(tokenAudits);
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        // No batch lookup needed when only TokenAudits (they already have merchantTokenRegistrationId)
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-00000005");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            assertEquals("merchant-direct", audit.getMerchantTokenRegistrationId());
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalRecordsCreated());
        // Verify no batch lookup was needed (no FetchInfoAudits or TokenEvents)
        verify(tokenTransactionRepository, never()).findByPaymentTokenIdIn(anyList());
    }
    
    @Test
    void testSyncBillingAudit_AllFieldsPopulated_FetchInfoAudit() throws CybersourceException {
        // Arrange - Verify all fields are populated in BillingAudit
        FetchInformationAudit fetchInfoAudit = FetchInformationAudit.builder()
                .id("fetch-1")
                .traceId("trace-complete")
                .paymentTokenId("payment-token-500")
                .informationType("Cryptogram")
                .eventReference("FetchCryptogram")
                .isRequestComplete(true)
                .timestamp(LocalDateTime.of(2025, 1, 15, 10, 30))
                .externalReference("ext-ref-complete")
                .build();
        
        TokenTransaction tokenTransaction = TokenTransaction.builder()
                .paymentTokenId("payment-token-500")
                .merchantTokenRegistrationId("merchant-complete")
                .build();
        
        when(syncInformationRepository.findSyncInfo()).thenReturn(createSyncInfo(LocalDateTime.now().minusHours(1)));
        when(tokenAuditRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any()))
                .thenReturn(Arrays.asList(fetchInfoAudit));
        when(tokenEventsRepository.findByTimestampBetween(any(), any())).thenReturn(new ArrayList<>());
        when(tokenTransactionRepository.findByPaymentTokenIdIn(anyList()))
                .thenReturn(Arrays.asList(tokenTransaction));
        when(referenceNumberGenerator.generateUniqueReferenceNumber()).thenReturn("BILL-1234567890-99999");
        when(billingAuditRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<BillingAudit> audits = invocation.getArgument(0);
            assertEquals(1, audits.size());
            BillingAudit audit = audits.get(0);
            
            // Verify all fields are populated
            assertNotNull(audit.getTraceIdReference(), "traceIdReference should be populated");
            assertEquals("trace-complete", audit.getTraceIdReference());
            
            assertNotNull(audit.getTraceIdEvent(), "traceIdEvent should be populated");
            assertEquals(TraceIdEventType.REQUEST_CRYPTOGRAM.getDisplayName(), audit.getTraceIdEvent());
            
            assertNotNull(audit.getEventTimeStamp(), "eventTimeStamp should be populated");
            assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), audit.getEventTimeStamp());
            
            assertNotNull(audit.getEventType(), "eventType should be populated");
            assertEquals(EventType.SUCCESS.getDisplayName(), audit.getEventType());
            
            assertNotNull(audit.getMerchantTokenRegistrationId(), "merchantTokenRegistrationId should be populated");
            assertEquals("merchant-complete", audit.getMerchantTokenRegistrationId());
            
            assertNotNull(audit.getBillingReferenceNumber(), "billingReferenceNumber should be populated");
            assertEquals("BILL-1234567890-99999", audit.getBillingReferenceNumber());
            
            assertNotNull(audit.getBillingBatchNumber(), "billingBatchNumber should be populated");
            
            assertNotNull(audit.getTimestamp(), "timestamp should be populated");
            
            assertNotNull(audit.getPaymentTokenId(), "paymentTokenId should be populated");
            assertEquals("payment-token-500", audit.getPaymentTokenId());
            
            assertNotNull(audit.getExternalReference(), "externalReference should be populated");
            assertEquals("ext-ref-complete", audit.getExternalReference());
            
            assertNotNull(audit.getNotes(), "notes should be populated");
            assertTrue(audit.getNotes().contains("Cryptogram"));
            assertTrue(audit.getNotes().contains("FetchCryptogram"));
            
            return audits;
        });
        when(syncInformationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        BillingAuditSyncResponse response = billingAuditSyncService.syncBillingAudit(null);
        
        // Assert
        assertTrue(response.isSuccess());
        assertEquals(1, response.getTotalRecordsCreated());
    }
}

