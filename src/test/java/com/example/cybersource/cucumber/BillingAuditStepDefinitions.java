package com.example.cybersource.cucumber;

import com.example.cybersource.dto.BillingAuditSyncRequest;
import com.example.cybersource.dto.BillingAuditSyncResponse;
import com.example.cybersource.entity.BillingAuditSyncInformation;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.*;
import com.example.cybersource.service.BillingAuditSyncService;
import com.example.cybersource.util.BillingReferenceNumberGenerator;
import com.example.cybersource.util.ExcelTestDataReader;
import com.example.cybersource.util.ExcelTestDataReader.BillingAuditScenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cucumber step definitions for Billing Audit feature.
 *
 * <p>All test data is loaded from the Excel workbook:<br>
 * {@code src/test/resources/test-data/network-token-test-scenarios.xlsx}
 * Sheet: {@code BillingAudit}
 */
public class BillingAuditStepDefinitions {

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // -----------------------------------------------------------------------
    // Collaborators (mocked per-scenario)
    // -----------------------------------------------------------------------
    private BillingAuditSyncService billingAuditSyncService;
    private TokenAuditRepository tokenAuditRepository;
    private FetchInformationAuditRepository fetchInformationAuditRepository;
    private TokenEventsRepository tokenEventsRepository;
    private TokenTransactionRepository tokenTransactionRepository;
    private BillingAuditRepository billingAuditRepository;
    private BillingAuditSyncInformationRepository syncInformationRepository;
    private BillingReferenceNumberGenerator referenceNumberGenerator;

    // -----------------------------------------------------------------------
    // Per-scenario state
    // -----------------------------------------------------------------------
    private BillingAuditScenario scenario;
    private BillingAuditSyncResponse response;
    private Exception thrownException;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @Before
    public void setUpBillingAudit() {
        tokenAuditRepository            = Mockito.mock(TokenAuditRepository.class);
        fetchInformationAuditRepository = Mockito.mock(FetchInformationAuditRepository.class);
        tokenEventsRepository           = Mockito.mock(TokenEventsRepository.class);
        tokenTransactionRepository      = Mockito.mock(TokenTransactionRepository.class);
        billingAuditRepository          = Mockito.mock(BillingAuditRepository.class);
        syncInformationRepository       = Mockito.mock(BillingAuditSyncInformationRepository.class);
        referenceNumberGenerator        = Mockito.mock(BillingReferenceNumberGenerator.class);

        billingAuditSyncService = new BillingAuditSyncService(
                tokenAuditRepository,
                fetchInformationAuditRepository,
                tokenEventsRepository,
                tokenTransactionRepository,
                billingAuditRepository,
                syncInformationRepository,
                referenceNumberGenerator);

        thrownException = null;
        response        = null;
        scenario        = null;
    }

    // -----------------------------------------------------------------------
    // Steps
    // -----------------------------------------------------------------------

    @Given("I load billing audit scenario {string} from test data")
    public void iLoadBillingAuditScenario(String scenarioId) {
        scenario = ExcelTestDataReader.getBillingAuditScenario(scenarioId);
        setupMocks();
    }

    @When("I execute the billing audit sync operation")
    public void iExecuteTheBillingAuditSyncOperation() {
        BillingAuditSyncRequest request = buildRequest();
        try {
            response = billingAuditSyncService.syncBillingAudit(request);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @Then("the billing audit response should match the expected outcome")
    public void theBillingAuditResponseShouldMatchExpectedOutcome() {
        if (scenario.expectedSuccess) {
            assertNull(thrownException,
                    "Expected success but got exception: "
                            + (thrownException != null ? thrownException.getMessage() : ""));
            assertNotNull(response, "Response must not be null");
            assertTrue(response.isSuccess(),
                    "Expected success=true in response but was false");
        } else {
            // Errors manifest either as exception or as error response
            boolean hasException = thrownException != null;
            boolean hasErrorResponse = response != null && !response.isSuccess();

            assertTrue(hasException || hasErrorResponse,
                    "Expected failure but neither an exception nor an error response was produced");

            if (scenario.errorMessage != null && !scenario.errorMessage.isBlank()) {
                if (hasException) {
                    assertTrue(
                            thrownException.getMessage() != null
                                    && thrownException.getMessage().contains(scenario.errorMessage),
                            "Expected error message to contain '" + scenario.errorMessage
                                    + "' but was: " + thrownException.getMessage());
                } else {
                    assertTrue(
                            response.getErrorMessage() != null
                                    && response.getErrorMessage().contains(scenario.errorMessage),
                            "Expected errorMessage to contain '" + scenario.errorMessage
                                    + "' but was: " + response.getErrorMessage());
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private BillingAuditSyncRequest buildRequest() {
        if (scenario.startTime.isBlank() && scenario.endTime.isBlank()
                && !scenario.forceSync && scenario.batchSize == 0) {
            return null; // null body simulates "default parameters" scenario
        }
        return BillingAuditSyncRequest.builder()
                .startTime(parseDateTime(scenario.startTime))
                .endTime(parseDateTime(scenario.endTime))
                .forceSync(scenario.forceSync)
                .batchSize(scenario.batchSize > 0 ? scenario.batchSize : null)
                .build();
    }

    private void setupMocks() {
        // Stub syncInformationRepository: return an existing sync info record
        BillingAuditSyncInformation syncInfo = BillingAuditSyncInformation.builder()
                .id("sync-info-1")
                .lastSyncTimestamp(LocalDateTime.now().minusDays(1))
                .build();
        // findSyncInfo() is a default method that delegates to findAll()
        when(syncInformationRepository.findAll())
                .thenReturn(Collections.singletonList(syncInfo));
        when(syncInformationRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Stub source-data queries (return empty lists — no billing records to create)
        when(tokenAuditRepository.findByTimestampBetween(any(), any()))
                .thenReturn(Collections.emptyList());
        when(fetchInformationAuditRepository.findByTimestampBetween(any(), any()))
                .thenReturn(Collections.emptyList());
        when(tokenEventsRepository.findByTimestampBetween(any(), any()))
                .thenReturn(Collections.emptyList());

        if (scenario.hasError && scenario.errorMessage.contains("Optimistic locking")) {
            // OptimisticLockingFailureException during save → service throws CybersourceException
            when(syncInformationRepository.save(any()))
                    .thenThrow(new org.springframework.dao.OptimisticLockingFailureException(
                            "Optimistic locking failure"));
        } else if (scenario.hasError) {
            when(syncInformationRepository.save(any()))
                    .thenThrow(new RuntimeException(scenario.errorMessage));
        } else {
            when(billingAuditRepository.saveAll(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
            when(referenceNumberGenerator.generateUniqueReferenceNumber())
                    .thenReturn("BILLING-REF-001");
        }
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDateTime.parse(value, DT_FMT);
        } catch (Exception e) {
            return null;
        }
    }
}
