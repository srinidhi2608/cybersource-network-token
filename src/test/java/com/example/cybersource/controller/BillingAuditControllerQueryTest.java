package com.example.cybersource.controller;

import com.example.cybersource.dto.TokenInfo;
import com.example.cybersource.service.BillingAuditQueryService;
import com.example.cybersource.service.BillingAuditSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BillingAuditController query endpoints.
 * Tests getMerchantListByMonthAndYear and getTokenEventsByMerchant endpoints.
 */
@ExtendWith(MockitoExtension.class)
class BillingAuditControllerQueryTest {
    
    @Mock
    private BillingAuditSyncService billingAuditSyncService;
    
    @Mock
    private BillingAuditQueryService billingAuditQueryService;
    
    @InjectMocks
    private BillingAuditController billingAuditController;
    
    private List<String> mockMerchantList;
    private List<TokenInfo> mockTokenInfoList;
    
    @BeforeEach
    void setUp() {
        mockMerchantList = Arrays.asList("merchant-001", "merchant-002", "merchant-003");
        
        mockTokenInfoList = Arrays.asList(
                TokenInfo.builder()
                        .traceId("trace-001")
                        .transactionType("NetworkTokenProcessing")
                        .timestamp(LocalDateTime.of(2025, 1, 15, 10, 30))
                        .eventStatus(Boolean.TRUE)
                        .build(),
                TokenInfo.builder()
                        .traceId("trace-002")
                        .transactionType("Request Cryptogram")
                        .timestamp(LocalDateTime.of(2025, 1, 20, 14, 45))
                        .eventStatus(Boolean.FALSE)
                        .build()
        );
    }
    
    // ==================== getMerchantListByMonthAndYear Tests ====================
    
    @Test
    void testGetMerchantListByMonthAndYear_Success_ReturnsOkWithMerchantList() {
        // Arrange
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, year))
                .thenReturn(mockMerchantList);
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertEquals(mockMerchantList, response.getBody());
        
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, year);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_EmptyResult_ReturnsOkWithEmptyList() {
        // Arrange
        int month = 2;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, year))
                .thenReturn(Collections.emptyList());
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, year);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_InvalidMonth_ReturnsBadRequest() {
        // Arrange
        int invalidMonth = 13;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(invalidMonth, year))
                .thenThrow(new IllegalArgumentException("Month must be between 1 and 12"));
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(invalidMonth, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(invalidMonth, year);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_InvalidYear_ReturnsBadRequest() {
        // Arrange
        int month = 1;
        int invalidYear = 2019;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, invalidYear))
                .thenThrow(new IllegalArgumentException("Year must be between 2020 and 2100"));
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, invalidYear);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, invalidYear);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_UnexpectedError_ReturnsInternalServerError() {
        // Arrange
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, year))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, year);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_BoundaryMonth_January() {
        // Arrange
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, year))
                .thenReturn(mockMerchantList);
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, year);
    }
    
    @Test
    void testGetMerchantListByMonthAndYear_BoundaryMonth_December() {
        // Arrange
        int month = 12;
        int year = 2025;
        when(billingAuditQueryService.getMerchantListByMonthAndYear(month, year))
                .thenReturn(mockMerchantList);
        
        // Act
        ResponseEntity<List<String>> response = billingAuditController.getMerchantListByMonthAndYear(month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getMerchantListByMonthAndYear(month, year);
    }
    
    // ==================== getTokenEventsByMerchant Tests ====================
    
    @Test
    void testGetTokenEventsByMerchant_Success_ReturnsOkWithTokenInfoList() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(mockTokenInfoList, response.getBody());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_EmptyResult_ReturnsOkWithEmptyList() {
        // Arrange
        String merchantId = "merchant-999";
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(Collections.emptyList());
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_InvalidMerchantId_ReturnsBadRequest() {
        // Arrange
        String merchantId = null;
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenThrow(new IllegalArgumentException("Merchant token registration ID cannot be null or empty"));
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_InvalidMonth_ReturnsBadRequest() {
        // Arrange
        String merchantId = "merchant-001";
        int invalidMonth = 0;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, invalidMonth, year))
                .thenThrow(new IllegalArgumentException("Month must be between 1 and 12"));
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, invalidMonth, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, invalidMonth, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_InvalidYear_ReturnsBadRequest() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int invalidYear = 2101;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, invalidYear))
                .thenThrow(new IllegalArgumentException("Year must be between 2020 and 2100"));
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, invalidYear);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, invalidYear);
    }
    
    @Test
    void testGetTokenEventsByMerchant_UnexpectedError_ReturnsInternalServerError() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenThrow(new RuntimeException("Database connection failed"));
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_WithSpecialCharactersInMerchantId_Success() {
        // Arrange
        String merchantId = "merchant-001-special-!@#";
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_BoundaryValues_MinMonth() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_BoundaryValues_MaxMonth() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 12;
        int year = 2025;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_BoundaryValues_MinYear() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int year = 2020;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
    
    @Test
    void testGetTokenEventsByMerchant_BoundaryValues_MaxYear() {
        // Arrange
        String merchantId = "merchant-001";
        int month = 1;
        int year = 2100;
        when(billingAuditQueryService.getTokenEventsByMerchant(merchantId, month, year))
                .thenReturn(mockTokenInfoList);
        
        // Act
        ResponseEntity<List<TokenInfo>> response = billingAuditController.getTokenEventsByMerchant(merchantId, month, year);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(billingAuditQueryService, times(1)).getTokenEventsByMerchant(merchantId, month, year);
    }
}
