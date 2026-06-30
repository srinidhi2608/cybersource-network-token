package com.example.cybersource.service;

import com.example.cybersource.dto.TokenInfo;
import com.example.cybersource.entity.BillingAudit;
import com.example.cybersource.repository.BillingAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingAuditQueryService.
 * Tests all service methods for merchant list and token events retrieval.
 */
@ExtendWith(MockitoExtension.class)
class BillingAuditQueryServiceTest {
    
    @Mock
    private BillingAuditRepository billingAuditRepository;
    
    @InjectMocks
    private BillingAuditQueryService billingAuditQueryService;
    
    private BillingAudit testBillingAudit1;
    private BillingAudit testBillingAudit2;
    private BillingAudit testBillingAudit3;
    
    @BeforeEach
    void setUp() {
        testBillingAudit1 = BillingAudit.builder()
                .id("1")
                .traceIdReference("trace-001")
                .traceIdEvent("NetworkTokenProcessing")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 15, 10, 30))
                .eventType("Success")
                .merchantTokenRegistrationId("merchant-001")
                .billingReferenceNumber("BILL-001")
                .billingBatchNumber("batch-001")
                .build();
        
        testBillingAudit2 = BillingAudit.builder()
                .id("2")
                .traceIdReference("trace-002")
                .traceIdEvent("Request Cryptogram")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 20, 14, 45))
                .eventType("Failure")
                .merchantTokenRegistrationId("merchant-002")
                .billingReferenceNumber("BILL-002")
                .billingBatchNumber("batch-001")
                .build();
        
        testBillingAudit3 = BillingAudit.builder()
                .id("3")
                .traceIdReference("trace-003")
                .traceIdEvent("TokenLifeCycleManagement")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 25, 16, 0))
                .eventType("Success")
                .merchantTokenRegistrationId("merchant-001")
                .billingReferenceNumber("BILL-003")
                .billingBatchNumber("batch-002")
                .build();
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_WithData_ReturnsDistinctMerchants() {
        // Arrange
        List<BillingAudit> mockAudits = Arrays.asList(
                testBillingAudit1,
                testBillingAudit2,
                testBillingAudit3
        );
        
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size()); // Should have 2 distinct merchants
        assertTrue(result.contains("merchant-001"));
        assertTrue(result.contains("merchant-002"));
        
        verify(billingAuditRepository, times(1))
                .findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_WithNoData_ReturnsEmptyList() {
        // Arrange
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(new ArrayList<>());
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(2, 2025);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(billingAuditRepository, times(1))
                .findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_WithNullMerchantIds_FiltersOut() {
        // Arrange
        BillingAudit auditWithNullMerchant = BillingAudit.builder()
                .id("4")
                .merchantTokenRegistrationId(null)
                .eventTimeStamp(LocalDateTime.of(2025, 1, 10, 10, 0))
                .build();
        
        BillingAudit auditWithBlankMerchant = BillingAudit.builder()
                .id("5")
                .merchantTokenRegistrationId("  ")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 11, 11, 0))
                .build();
        
        List<BillingAudit> mockAudits = Arrays.asList(
                testBillingAudit1,
                auditWithNullMerchant,
                auditWithBlankMerchant
        );
        
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("merchant-001", result.get(0));
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_InvalidMonth_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getMerchantListByMonthAndYear(0, 2025);
        });
        
        assertEquals("Month must be between 1 and 12", exception.getMessage());
        
        // Test upper bound
        exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getMerchantListByMonthAndYear(13, 2025);
        });
        
        assertEquals("Month must be between 1 and 12", exception.getMessage());
        
        verify(billingAuditRepository, never()).findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_InvalidYear_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getMerchantListByMonthAndYear(1, 2019);
        });
        
        assertEquals("Year must be between 2020 and 2100", exception.getMessage());
        
        // Test upper bound
        exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getMerchantListByMonthAndYear(1, 2101);
        });
        
        assertEquals("Year must be between 2020 and 2100", exception.getMessage());
        
        verify(billingAuditRepository, never()).findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetTokenEventsByMerchant_WithData_ReturnsTokenInfoList() {
        // Arrange
        String merchantId = "merchant-001";
        List<BillingAudit> mockAudits = Arrays.asList(testBillingAudit1, testBillingAudit3);
        
        when(billingAuditRepository.findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
                eq(merchantId), any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<TokenInfo> result = billingAuditQueryService.getTokenEventsByMerchant(merchantId, 1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        
        TokenInfo tokenInfo1 = result.get(0);
        assertEquals("trace-001", tokenInfo1.getTraceId());
        assertEquals("NetworkTokenProcessing", tokenInfo1.getTransactionType());
        assertEquals(LocalDateTime.of(2025, 1, 15, 10, 30), tokenInfo1.getTimestamp());
        assertEquals(Boolean.TRUE, tokenInfo1.getEventStatus());
        
        TokenInfo tokenInfo2 = result.get(1);
        assertEquals("trace-003", tokenInfo2.getTraceId());
        assertEquals("TokenLifeCycleManagement", tokenInfo2.getTransactionType());
        assertEquals(Boolean.TRUE, tokenInfo2.getEventStatus());
        
        verify(billingAuditRepository, times(1))
                .findByMerchantTokenRegistrationIdAndEventTimeStampBetween(eq(merchantId), any(), any());
    }
    
    @Test
    void testGetTokenEventsByMerchant_WithNoData_ReturnsEmptyList() {
        // Arrange
        String merchantId = "merchant-999";
        when(billingAuditRepository.findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
                eq(merchantId), any(), any()))
                .thenReturn(new ArrayList<>());
        
        // Act
        List<TokenInfo> result = billingAuditQueryService.getTokenEventsByMerchant(merchantId, 1, 2025);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(billingAuditRepository, times(1))
                .findByMerchantTokenRegistrationIdAndEventTimeStampBetween(eq(merchantId), any(), any());
    }
    
    @Test
    void testGetTokenEventsByMerchant_WithFailureEvent_ReturnsFalseStatus() {
        // Arrange
        String merchantId = "merchant-002";
        List<BillingAudit> mockAudits = Arrays.asList(testBillingAudit2);
        
        when(billingAuditRepository.findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
                eq(merchantId), any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<TokenInfo> result = billingAuditQueryService.getTokenEventsByMerchant(merchantId, 1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        
        TokenInfo tokenInfo = result.get(0);
        assertEquals("trace-002", tokenInfo.getTraceId());
        assertEquals("Request Cryptogram", tokenInfo.getTransactionType());
        assertEquals(Boolean.FALSE, tokenInfo.getEventStatus());
    }
    
    @Test
    void testGetTokenEventsByMerchant_WithNullEventType_ReturnsNullStatus() {
        // Arrange
        String merchantId = "merchant-003";
        BillingAudit auditWithNullEventType = BillingAudit.builder()
                .id("4")
                .traceIdReference("trace-004")
                .traceIdEvent("NetworkTokenProcessing")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 10, 10, 0))
                .eventType(null)
                .merchantTokenRegistrationId(merchantId)
                .build();
        
        List<BillingAudit> mockAudits = Arrays.asList(auditWithNullEventType);
        
        when(billingAuditRepository.findByMerchantTokenRegistrationIdAndEventTimeStampBetween(
                eq(merchantId), any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<TokenInfo> result = billingAuditQueryService.getTokenEventsByMerchant(merchantId, 1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertNull(result.get(0).getEventStatus());
    }
    
    @Test
    void testGetTokenEventsByMerchant_NullMerchantId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getTokenEventsByMerchant(null, 1, 2025);
        });
        
        assertEquals("Merchant token registration ID cannot be null or empty", exception.getMessage());
        
        verify(billingAuditRepository, never())
                .findByMerchantTokenRegistrationIdAndEventTimeStampBetween(any(), any(), any());
    }
    
    @Test
    void testGetTokenEventsByMerchant_BlankMerchantId_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getTokenEventsByMerchant("  ", 1, 2025);
        });
        
        assertEquals("Merchant token registration ID cannot be null or empty", exception.getMessage());
        
        verify(billingAuditRepository, never())
                .findByMerchantTokenRegistrationIdAndEventTimeStampBetween(any(), any(), any());
    }
    
    @Test
    void testGetTokenEventsByMerchant_InvalidMonth_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getTokenEventsByMerchant("merchant-001", 0, 2025);
        });
        
        assertEquals("Month must be between 1 and 12", exception.getMessage());
    }
    
    @Test
    void testGetTokenEventsByMerchant_InvalidYear_ThrowsException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            billingAuditQueryService.getTokenEventsByMerchant("merchant-001", 1, 2019);
        });
        
        assertEquals("Year must be between 2020 and 2100", exception.getMessage());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_February_LeapYear() {
        // Arrange
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(Arrays.asList(testBillingAudit1));
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(2, 2024); // Leap year
        
        // Assert
        assertNotNull(result);
        verify(billingAuditRepository, times(1))
                .findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_December_EndOfYear() {
        // Arrange
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(Arrays.asList(testBillingAudit1));
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(12, 2025);
        
        // Assert
        assertNotNull(result);
        verify(billingAuditRepository, times(1))
                .findByEventTimeStampBetweenWithMerchantIdOnly(any(), any());
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_WithDuplicateMerchants_ReturnsSortedUnique() {
        // Arrange
        BillingAudit audit4 = BillingAudit.builder()
                .id("4")
                .merchantTokenRegistrationId("merchant-002")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 5, 10, 0))
                .build();
        
        BillingAudit audit5 = BillingAudit.builder()
                .id("5")
                .merchantTokenRegistrationId("merchant-001")
                .eventTimeStamp(LocalDateTime.of(2025, 1, 6, 11, 0))
                .build();
        
        List<BillingAudit> mockAudits = Arrays.asList(
                testBillingAudit2, // merchant-002
                testBillingAudit1, // merchant-001
                audit4,            // merchant-002 (duplicate)
                audit5             // merchant-001 (duplicate)
        );
        
        when(billingAuditRepository.findByEventTimeStampBetweenWithMerchantIdOnly(any(), any()))
                .thenReturn(mockAudits);
        
        // Act
        List<String> result = billingAuditQueryService.getMerchantListByMonthAndYear(1, 2025);
        
        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("merchant-001", result.get(0)); // Should be sorted
        assertEquals("merchant-002", result.get(1));
    }
}
