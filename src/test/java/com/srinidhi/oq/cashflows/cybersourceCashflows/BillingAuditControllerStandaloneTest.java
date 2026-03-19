package com.srinidhi.oq.cashflows.cybersourceCashflows;

import com.example.cybersource.controller.BillingAuditController;
import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.service.BillingAuditSyncService;
import com.example.cybersource.util.CSVTestDataReader;
import com.example.cybersource.util.CSVTestDataReader.TestScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * Comprehensive standalone controller tests for BillingAuditController using CSV test data.
 * Tests cover all methods, lines, and branches for 100% coverage.
 * 
 * Uses pure Mockito without Spring Boot test framework for simplicity.
 * Test data is read from CSV files in src/test/resources/test-data/:
 * - billing-audit-sync-requests.csv: Test input parameters
 * - billing-audit-sync-responses.csv: Expected response data
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BillingAuditController Standalone CSV-based Tests")
class BillingAuditControllerStandaloneTest {
    
    @Mock
    private BillingAuditSyncService billingAuditSyncService;
    
    @InjectMocks
    private BillingAuditController billingAuditController;
    
    private static final String REQUEST_CSV = "test-data/billing-audit-sync-requests.csv";
    private static final String RESPONSE_CSV = "test-data/billing-audit-sync-responses.csv";
    
    private List<TestScenario> testScenarios;
    
    @BeforeEach
    void setUp() {
        testScenarios = CSVTestDataReader.readTestScenarios(REQUEST_CSV, RESPONSE_CSV);
        assertFalse(testScenarios.isEmpty(), "Test scenarios should be loaded from CSV");
    }
    
    /**
     * Provides test scenarios for parameterized tests.
     */
    static Stream<TestScenario> provideTestScenarios() {
        return CSVTestDataReader.readTestScenarios(REQUEST_CSV, RESPONSE_CSV).stream();
    }
    
    /**
     * Tests the syncBillingAudit endpoint with various scenarios from CSV data.
     * Covers: success cases, error cases, edge cases, null handling.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTestScenarios")
    @DisplayName("Test syncBillingAudit with CSV scenarios")
    void testSyncBillingAudit_FromCSV(TestScenario scenario) throws CybersourceException {
        // Arrange
        BillingAuditSyncRequest request = scenario.getRequest();
        boolean isSuccess = scenario.isSuccess();
        boolean hasError = scenario.hasError();
        
        BillingAuditSyncResponse mockResponse = createMockResponse(scenario);
        
        // Configure mock based on scenario
        if (scenario.getScenarioName().contains("cybersource_exception")) {
            when(billingAuditSyncService.syncBillingAudit(any()))
                .thenThrow(new CybersourceException("Optimistic locking failure"));
        } else if (scenario.getScenarioName().contains("generic_exception")) {
            when(billingAuditSyncService.syncBillingAudit(any()))
                .thenThrow(new RuntimeException("Unexpected error"));
        } else {
            when(billingAuditSyncService.syncBillingAudit(any()))
                .thenReturn(mockResponse);
        }
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> responseEntity;
        if (scenario.getScenarioName().equals("success_null_request")) {
            responseEntity = billingAuditController.syncBillingAudit(null);
            verify(billingAuditSyncService).syncBillingAudit(isNull());
        } else {
            responseEntity = billingAuditController.syncBillingAudit(request);
            verify(billingAuditSyncService).syncBillingAudit(any());
        }
        
        // Assert
        assertNotNull(responseEntity);
        assertEquals(scenario.getExpectedStatus(), responseEntity.getStatusCodeValue(),
            "HTTP status mismatch for scenario: " + scenario.getScenarioName());
        
        BillingAuditSyncResponse response = responseEntity.getBody();
        assertNotNull(response);
        
        assertEquals(isSuccess, response.isSuccess(),
            "Success flag mismatch for scenario: " + scenario.getScenarioName());
        
        if (hasError) {
            assertNotNull(response.getErrorMessage(),
                "Error message should be present for error scenarios");
            assertFalse(response.getErrorMessage().isEmpty());
        }
        
        if (isSuccess && !hasError) {
            assertEquals(scenario.getTotalRecordsCreated(), response.getTotalRecordsCreated(),
                "Total records created mismatch for scenario: " + scenario.getScenarioName());
            assertNotNull(response.getBillingBatchNumber(),
                "Billing batch number should be present");
        }
    }
    
    /**
     * Tests syncBillingAudit with explicit null request.
     * Branch coverage: null request path
     */
    @Test
    @DisplayName("Test syncBillingAudit with null request")
    void testSyncBillingAudit_NullRequest_Success() throws CybersourceException {
        // Arrange
        BillingAuditSyncResponse mockResponse = BillingAuditSyncResponse.builder()
            .success(true)
            .message("Sync completed successfully")
            .billingBatchNumber(UUID.randomUUID().toString())
            .totalRecordsCreated(10)
            .tokenAuditCount(5)
            .fetchInfoAuditCount(3)
            .tokenEventsCount(2)
            .sampleBillingReferenceNumbers(Arrays.asList("BILL-001", "BILL-002"))
            .build();
        
        when(billingAuditSyncService.syncBillingAudit(isNull()))
            .thenReturn(mockResponse);
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(null);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(10, response.getBody().getTotalRecordsCreated());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(isNull());
    }
    
    /**
     * Tests syncBillingAudit when CybersourceException is thrown.
     * Branch coverage: CybersourceException catch block
     */
    @Test
    @DisplayName("Test syncBillingAudit with CybersourceException")
    void testSyncBillingAudit_CybersourceException() throws CybersourceException {
        // Arrange
        String errorMessage = "Optimistic locking failure";
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenThrow(new CybersourceException(errorMessage));
        
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
            .startTime(LocalDateTime.now().minusHours(1))
            .build();
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Billing audit sync failed", response.getBody().getMessage());
        assertEquals(errorMessage, response.getBody().getErrorMessage());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Tests syncBillingAudit when generic Exception is thrown.
     * Branch coverage: generic Exception catch block
     */
    @Test
    @DisplayName("Test syncBillingAudit with generic Exception")
    void testSyncBillingAudit_GenericException() throws CybersourceException {
        // Arrange
        String errorMessage = "Unexpected error";
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenThrow(new RuntimeException(errorMessage));
        
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
            .endTime(LocalDateTime.now())
            .build();
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Unexpected error during billing audit sync", response.getBody().getMessage());
        assertEquals(errorMessage, response.getBody().getErrorMessage());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Tests syncBillingAudit with all request parameters populated.
     * Line coverage: all request fields
     */
    @Test
    @DisplayName("Test syncBillingAudit with full request parameters")
    void testSyncBillingAudit_FullRequest_Success() throws CybersourceException {
        // Arrange
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2025, 1, 2, 0, 0);
        
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
            .startTime(startTime)
            .endTime(endTime)
            .forceSync(true)
            .batchSize(500)
            .build();
        
        BillingAuditSyncResponse mockResponse = BillingAuditSyncResponse.builder()
            .success(true)
            .message("Sync completed")
            .billingBatchNumber(UUID.randomUUID().toString())
            .syncStartTime(startTime)
            .syncEndTime(endTime)
            .totalRecordsCreated(25)
            .tokenAuditCount(12)
            .fetchInfoAuditCount(9)
            .tokenEventsCount(4)
            .build();
        
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenReturn(mockResponse);
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isSuccess());
        assertEquals(25, response.getBody().getTotalRecordsCreated());
        assertEquals(12, response.getBody().getTokenAuditCount());
        assertEquals(9, response.getBody().getFetchInfoAuditCount());
        assertEquals(4, response.getBody().getTokenEventsCount());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Tests the healthCheck endpoint.
     * Line coverage: healthCheck method
     */
    @Test
    @DisplayName("Test healthCheck endpoint")
    void testHealthCheck_Success() {
        // Act
        ResponseEntity<String> response = billingAuditController.healthCheck();
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Billing Audit Sync Service is running", response.getBody());
        
        // Verify no service method was called
        verifyNoInteractions(billingAuditSyncService);
    }
    
    /**
     * Tests syncBillingAudit with only startTime parameter.
     * Branch coverage: partial request parameters
     */
    @Test
    @DisplayName("Test syncBillingAudit with only startTime")
    void testSyncBillingAudit_OnlyStartTime() throws CybersourceException {
        // Arrange
        LocalDateTime startTime = LocalDateTime.of(2025, 1, 1, 0, 0);
        
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
            .startTime(startTime)
            .build();
        
        BillingAuditSyncResponse mockResponse = BillingAuditSyncResponse.builder()
            .success(true)
            .billingBatchNumber(UUID.randomUUID().toString())
            .totalRecordsCreated(15)
            .tokenAuditCount(8)
            .fetchInfoAuditCount(5)
            .tokenEventsCount(2)
            .build();
        
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenReturn(mockResponse);
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(15, response.getBody().getTotalRecordsCreated());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Tests syncBillingAudit with empty result (no records found).
     * Edge case: empty time window
     */
    @Test
    @DisplayName("Test syncBillingAudit with empty result")
    void testSyncBillingAudit_EmptyResult() throws CybersourceException {
        // Arrange
        BillingAuditSyncRequest request = BillingAuditSyncRequest.builder()
            .startTime(LocalDateTime.of(2025, 1, 1, 0, 0))
            .endTime(LocalDateTime.of(2025, 1, 1, 0, 0))
            .build();
        
        BillingAuditSyncResponse mockResponse = BillingAuditSyncResponse.builder()
            .success(true)
            .message("No records found in the specified time window")
            .billingBatchNumber(UUID.randomUUID().toString())
            .totalRecordsCreated(0)
            .tokenAuditCount(0)
            .fetchInfoAuditCount(0)
            .tokenEventsCount(0)
            .build();
        
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenReturn(mockResponse);
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(request);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertEquals(0, response.getBody().getTotalRecordsCreated());
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Tests that successful sync includes all response fields.
     * Line coverage: response field validation
     */
    @Test
    @DisplayName("Test successful sync includes all response fields")
    void testSyncBillingAudit_ValidateAllResponseFields() throws CybersourceException {
        // Arrange
        String batchNumber = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        
        BillingAuditSyncResponse mockResponse = BillingAuditSyncResponse.builder()
            .success(true)
            .message("Sync completed successfully")
            .billingBatchNumber(batchNumber)
            .syncStartTime(now.minusHours(1))
            .syncEndTime(now)
            .operationStartedAt(now.minusMinutes(5))
            .operationCompletedAt(now)
            .totalRecordsCreated(30)
            .tokenAuditCount(15)
            .fetchInfoAuditCount(10)
            .tokenEventsCount(5)
            .sampleBillingReferenceNumbers(Arrays.asList(
                "BILL-123-001", "BILL-123-002", "BILL-123-003"))
            .build();
        
        when(billingAuditSyncService.syncBillingAudit(any()))
            .thenReturn(mockResponse);
        
        // Act
        ResponseEntity<BillingAuditSyncResponse> response = billingAuditController.syncBillingAudit(null);
        
        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        BillingAuditSyncResponse body = response.getBody();
        assertNotNull(body);
        assertTrue(body.isSuccess());
        assertEquals("Sync completed successfully", body.getMessage());
        assertEquals(batchNumber, body.getBillingBatchNumber());
        assertEquals(30, body.getTotalRecordsCreated());
        assertEquals(15, body.getTokenAuditCount());
        assertEquals(10, body.getFetchInfoAuditCount());
        assertEquals(5, body.getTokenEventsCount());
        assertNotNull(body.getSampleBillingReferenceNumbers());
        assertEquals(3, body.getSampleBillingReferenceNumbers().size());
        assertEquals("BILL-123-001", body.getSampleBillingReferenceNumbers().get(0));
        
        verify(billingAuditSyncService, times(1)).syncBillingAudit(any());
    }
    
    /**
     * Helper method to create mock response based on test scenario.
     */
    private BillingAuditSyncResponse createMockResponse(TestScenario scenario) {
        if (scenario.hasError()) {
            return BillingAuditSyncResponse.builder()
                .success(false)
                .message("Billing audit sync failed")
                .errorMessage(scenario.getExpectedResponse().get("errorMessage"))
                .build();
        }
        
        long totalRecords = scenario.getTotalRecordsCreated();
        long tokenAuditCount = Long.parseLong(
            scenario.getExpectedResponse().getOrDefault("tokenAuditCount", "0"));
        long fetchInfoCount = Long.parseLong(
            scenario.getExpectedResponse().getOrDefault("fetchInfoAuditCount", "0"));
        long tokenEventsCount = Long.parseLong(
            scenario.getExpectedResponse().getOrDefault("tokenEventsCount", "0"));
        
        return BillingAuditSyncResponse.builder()
            .success(true)
            .message("Sync completed successfully")
            .billingBatchNumber(UUID.randomUUID().toString())
            .totalRecordsCreated(totalRecords)
            .tokenAuditCount(tokenAuditCount)
            .fetchInfoAuditCount(fetchInfoCount)
            .tokenEventsCount(tokenEventsCount)
            .sampleBillingReferenceNumbers(totalRecords > 0 ?
                Arrays.asList("BILL-001", "BILL-002") : null)
            .build();
    }
}
