package com.example.cybersource.cucumber;

import com.example.cybersource.dto.CreateNetworkTokenRequest;
import com.example.cybersource.dto.CreateNetworkTokenResponse;
import com.example.cybersource.dto.CreateCryptogramRequest;
import com.example.cybersource.dto.CreateCryptogramResponse;
import com.example.cybersource.entity.MerchantEnrollResponse;
import com.example.cybersource.entity.TokenAudit;
import com.example.cybersource.entity.TokenTransaction;
import com.example.cybersource.exception.CybersourceException;
import com.example.cybersource.repository.FetchInformationAuditRepository;
import com.example.cybersource.repository.MerchantEnrollResponseRepository;
import com.example.cybersource.repository.TokenAuditRepository;
import com.example.cybersource.repository.TokenTransactionRepository;
import com.example.cybersource.service.*;
import com.example.cybersource.util.ExcelTestDataReader;
import com.example.cybersource.util.ExcelTestDataReader.NetworkTokenScenario;
import com.example.cybersource.util.ExcelTestDataReader.CryptogramScenario;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Cucumber step definitions for Network Token and Cryptogram features.
 *
 * <p>All test data (inputs and expected outcomes) is loaded from the Excel workbook:<br>
 * {@code src/test/resources/test-data/network-token-test-scenarios.xlsx}
 *
 * <p>Step flow for both features:
 * <ol>
 *   <li>Load the scenario by ID from Excel (Given)</li>
 *   <li>Execute the service operation (When)</li>
 *   <li>Assert the response matches the expected outcome (Then)</li>
 * </ol>
 */
@CucumberContextConfiguration
@SpringBootTest(classes = CucumberTestConfig.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class NetworkTokenStepDefinitions {

    // -----------------------------------------------------------------------
    // Collaborators (mocked per-scenario in @Before)
    // -----------------------------------------------------------------------
    private NetworkTokenService networkTokenService;
    private MerchantEnrollResponseRepository merchantRepository;
    private TokenTransactionRepository tokenTransactionRepository;
    private TokenAuditRepository tokenAuditRepository;
    private FetchInformationAuditRepository fetchInformationAuditRepository;
    private InstrumentIdentifierService instrumentIdentifierService;
    private CybersourceRestClient cybersourceRestClient;
    private EncryptionService encryptionService;

    // -----------------------------------------------------------------------
    // Per-scenario state
    // -----------------------------------------------------------------------
    private NetworkTokenScenario ntScenario;
    private CryptogramScenario   crScenario;

    private CreateNetworkTokenResponse networkTokenResponse;
    private CreateCryptogramResponse   cryptogramResponse;
    private Exception thrownException;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @Before
    public void setUp() {
        merchantRepository            = Mockito.mock(MerchantEnrollResponseRepository.class);
        tokenTransactionRepository    = Mockito.mock(TokenTransactionRepository.class);
        tokenAuditRepository          = Mockito.mock(TokenAuditRepository.class);
        fetchInformationAuditRepository = Mockito.mock(FetchInformationAuditRepository.class);
        instrumentIdentifierService   = Mockito.mock(InstrumentIdentifierService.class);
        cybersourceRestClient         = Mockito.mock(CybersourceRestClient.class);
        encryptionService             = new EncryptionService();

        networkTokenService = new NetworkTokenService(
                merchantRepository,
                tokenTransactionRepository,
                tokenAuditRepository,
                fetchInformationAuditRepository,
                instrumentIdentifierService,
                cybersourceRestClient,
                encryptionService);

        thrownException      = null;
        networkTokenResponse = null;
        cryptogramResponse   = null;
        ntScenario           = null;
        crScenario           = null;
    }

    // -----------------------------------------------------------------------
    // Network-token Given / When / Then
    // -----------------------------------------------------------------------

    @Given("I load network token scenario {string} from test data")
    public void iLoadNetworkTokenScenario(String scenarioId) {
        ntScenario = ExcelTestDataReader.getNetworkTokenScenario(scenarioId);
        setupMerchantMock(ntScenario.merchantId, ntScenario.merchantStatus);
    }

    @When("I execute the network token operation")
    public void iExecuteTheNetworkTokenOperation() throws Exception {
        // Stub common repository calls for success scenarios
        when(tokenAuditRepository.save(any(TokenAudit.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(fetchInformationAuditRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        if (ntScenario.isDuplicate) {
            // Simulate existing token for duplicate scenario
            TokenTransaction existing = TokenTransaction.builder()
                    .paymentTokenId("existing-token-id")
                    .merchantTokenRegistrationId(ntScenario.merchantId)
                    .instrumentIdentifierId("instr-existing")
                    .networkToken("base64token")
                    .par("PAR_EXISTING")
                    .tokenExpiryMonth("12")
                    .tokenExpiryYear("2025")
                    .tokenStatus("ACTIVE")
                    .build();
            Mockito.doReturn("{\"id\":\"instr-existing\"}")
                    .when(instrumentIdentifierService)
                    .createInstrumentIdentifier(anyString(), anyString());
            when(tokenTransactionRepository.findByInstrumentIdentifierId("instr-existing"))
                    .thenReturn(Optional.of(existing));
            Mockito.doReturn("{\"networkToken\":{\"number\":\"4111000011110000\","
                            + "\"par\":\"PAR_EXISTING\",\"expirationMonth\":\"12\","
                            + "\"expirationYear\":\"2025\",\"status\":\"ACTIVE\","
                            + "\"cryptogram\":\"FRESH_CRYPTOGRAM\"}}")
                    .when(cybersourceRestClient).get(anyString(), anyString());
            when(tokenTransactionRepository.save(any(TokenTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        } else {
            Mockito.doReturn("{\"id\":\"instr-123\"}")
                    .when(instrumentIdentifierService)
                    .createInstrumentIdentifier(anyString(), anyString());
            when(tokenTransactionRepository.findByInstrumentIdentifierId(anyString()))
                    .thenReturn(Optional.empty());
            Mockito.doReturn("{\"networkToken\":{\"number\":\"4111000011110000\","
                            + "\"par\":\"PAR123\",\"expirationMonth\":\"12\","
                            + "\"expirationYear\":\"2025\",\"status\":\"ACTIVE\","
                            + "\"cryptogram\":\"ABC123\"}}")
                    .when(cybersourceRestClient).get(anyString(), anyString());
            when(tokenTransactionRepository.save(any(TokenTransaction.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        CreateNetworkTokenRequest request = CreateNetworkTokenRequest.builder()
                .merchantTokenRegistrationId(ntScenario.merchantId)
                .cardNumber(ntScenario.cardNumber)
                .cardExpiryMonth(ntScenario.expiryMonth)
                .cardExpiryYear(ntScenario.expiryYear)
                .externalReference(ntScenario.externalReference)
                .build();

        try {
            networkTokenResponse = networkTokenService.createNetworkToken(request);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @Then("the network token response should match the expected outcome")
    public void theNetworkTokenResponseShouldMatchExpectedOutcome() {
        if (ntScenario.isSuccessExpected()) {
            assertNull(thrownException,
                    "Expected success but got exception: " + (thrownException != null ? thrownException.getMessage() : ""));
            assertNotNull(networkTokenResponse, "Response must not be null");
            assertNotNull(networkTokenResponse.getPaymentTokenId(), "paymentTokenId must be present");
            assertNotNull(networkTokenResponse.getNetworkToken(), "networkToken must be present");
            assertNotNull(networkTokenResponse.getCryptogram(), "cryptogram must be present");
            assertNotNull(networkTokenResponse.getPar(), "PAR must be present");
        } else {
            assertNotNull(thrownException,
                    "Expected failure with '" + ntScenario.expectedError + "' but no exception was thrown");
            if (ntScenario.expectedError != null && !ntScenario.expectedError.isBlank()) {
                assertTrue(
                        thrownException.getMessage() != null
                                && thrownException.getMessage().contains(ntScenario.expectedError),
                        "Expected error message to contain '" + ntScenario.expectedError
                                + "' but was: " + thrownException.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Cryptogram Given / When / Then
    // -----------------------------------------------------------------------

    @Given("I load cryptogram scenario {string} from test data")
    public void iLoadCryptogramScenario(String scenarioId) {
        crScenario = ExcelTestDataReader.getCryptogramScenario(scenarioId);
    }

    @When("I execute the cryptogram operation")
    public void iExecuteTheCryptogramOperation() throws Exception {
        if (crScenario.tokenExists) {
            TokenTransaction token = TokenTransaction.builder()
                    .paymentTokenId(crScenario.paymentTokenId)
                    .merchantTokenRegistrationId("merchant-123")
                    .instrumentIdentifierId("instr-123")
                    .build();
            when(tokenTransactionRepository.findByPaymentTokenId(crScenario.paymentTokenId))
                    .thenReturn(Optional.of(token));
            // Cryptogram service also validates the merchant
            MerchantEnrollResponse merchant = MerchantEnrollResponse.builder()
                    .merchantTokenRegistrationId("merchant-123")
                    .transactingOrgId("org-merchant-123")
                    .status(MerchantEnrollResponse.MerchantStatus.ACTIVE)
                    .build();
            when(merchantRepository.findByMerchantTokenRegistrationId("merchant-123"))
                    .thenReturn(Optional.of(merchant));
            Mockito.doReturn("{\"networkToken\":{\"cryptogram\":\"XYZ789\"}}")
                    .when(cybersourceRestClient).get(anyString(), anyString());
            when(fetchInformationAuditRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));
        } else {
            when(tokenTransactionRepository.findByPaymentTokenId(crScenario.paymentTokenId))
                    .thenReturn(Optional.empty());
        }

        CreateCryptogramRequest request = CreateCryptogramRequest.builder()
                .paymentTokenId(crScenario.paymentTokenId)
                .externalReference(crScenario.externalReference)
                .build();

        try {
            cryptogramResponse = networkTokenService.createCryptogram(request);
        } catch (Exception e) {
            thrownException = e;
        }
    }

    @Then("the cryptogram response should match the expected outcome")
    public void theCryptogramResponseShouldMatchExpectedOutcome() {
        if (crScenario.isSuccessExpected()) {
            assertNull(thrownException,
                    "Expected success but got exception: " + (thrownException != null ? thrownException.getMessage() : ""));
            assertNotNull(cryptogramResponse, "Response must not be null");
            assertNotNull(cryptogramResponse.getCryptogram(), "cryptogram must be present");
            assertNotNull(cryptogramResponse.getPaymentTokenId(), "paymentTokenId must be present");
        } else {
            assertNotNull(thrownException,
                    "Expected failure with '" + crScenario.expectedError + "' but no exception was thrown");
            if (crScenario.expectedError != null && !crScenario.expectedError.isBlank()) {
                assertTrue(
                        thrownException.getMessage() != null
                                && thrownException.getMessage().contains(crScenario.expectedError),
                        "Expected error message to contain '" + crScenario.expectedError
                                + "' but was: " + thrownException.getMessage());
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private void setupMerchantMock(String merchantId, String merchantStatus) {
        switch (merchantStatus) {
            case "ACTIVE" -> {
                MerchantEnrollResponse merchant = MerchantEnrollResponse.builder()
                        .merchantTokenRegistrationId(merchantId)
                        .transactingOrgId("org-" + merchantId)
                        .merchantName("Test Merchant")
                        .status(MerchantEnrollResponse.MerchantStatus.ACTIVE)
                        .enrolledAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                when(merchantRepository.findByMerchantTokenRegistrationId(merchantId))
                        .thenReturn(Optional.of(merchant));
            }
            case "INACTIVE" -> {
                MerchantEnrollResponse merchant = MerchantEnrollResponse.builder()
                        .merchantTokenRegistrationId(merchantId)
                        .transactingOrgId("org-" + merchantId)
                        .merchantName("Inactive Merchant")
                        .status(MerchantEnrollResponse.MerchantStatus.INACTIVE)
                        .build();
                when(merchantRepository.findByMerchantTokenRegistrationId(merchantId))
                        .thenReturn(Optional.of(merchant));
            }
            case "NOT_ENROLLED" -> when(merchantRepository.findByMerchantTokenRegistrationId(merchantId))
                    .thenReturn(Optional.empty());
            default -> throw new IllegalArgumentException(
                    "Unknown MerchantStatus in Excel: " + merchantStatus);
        }
    }
}

