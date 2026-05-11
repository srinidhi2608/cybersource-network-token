package com.example.cybersource.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility that reads Cucumber test scenarios from the Excel workbook
 * {@code src/test/resources/test-data/network-token-test-scenarios.xlsx}.
 *
 * <p>The workbook contains three sheets:
 * <ol>
 *   <li><b>NetworkToken</b> – scenarios for token creation / duplicate detection</li>
 *   <li><b>Cryptogram</b>  – scenarios for cryptogram generation</li>
 *   <li><b>BillingAudit</b> – scenarios for billing-audit sync operations</li>
 * </ol>
 *
 * <p>All scenario objects are keyed by their {@code ScenarioId} column value so
 * that step definitions can look them up in O(1) time.
 */
public class ExcelTestDataReader {

    private static final Logger log = LoggerFactory.getLogger(ExcelTestDataReader.class);

    private static final String EXCEL_PATH = "test-data/network-token-test-scenarios.xlsx";

    // -----------------------------------------------------------------------
    // Public scenario POJOs
    // -----------------------------------------------------------------------

    /** Data for a single network-token test scenario. */
    public static class NetworkTokenScenario {
        public final String scenarioId;
        public final String scenarioName;
        public final String description;
        public final String tags;
        public final String merchantId;
        public final String merchantStatus;   // ACTIVE | INACTIVE | NOT_ENROLLED
        public final String cardNumber;
        public final String expiryMonth;
        public final String expiryYear;
        public final String externalReference;
        public final String expectedOutcome;  // SUCCESS | FAILURE
        public final String expectedError;
        public final String expectedErrorType;
        public final boolean isDuplicate;

        public NetworkTokenScenario(String scenarioId, String scenarioName, String description,
                                    String tags, String merchantId, String merchantStatus,
                                    String cardNumber, String expiryMonth, String expiryYear,
                                    String externalReference, String expectedOutcome,
                                    String expectedError, String expectedErrorType,
                                    boolean isDuplicate) {
            this.scenarioId        = scenarioId;
            this.scenarioName      = scenarioName;
            this.description       = description;
            this.tags              = tags;
            this.merchantId        = merchantId;
            this.merchantStatus    = merchantStatus;
            this.cardNumber        = cardNumber;
            this.expiryMonth       = expiryMonth;
            this.expiryYear        = expiryYear;
            this.externalReference = externalReference;
            this.expectedOutcome   = expectedOutcome;
            this.expectedError     = expectedError;
            this.expectedErrorType = expectedErrorType;
            this.isDuplicate       = isDuplicate;
        }

        public boolean isSuccessExpected() {
            return "SUCCESS".equalsIgnoreCase(expectedOutcome);
        }
    }

    /** Data for a single cryptogram test scenario. */
    public static class CryptogramScenario {
        public final String scenarioId;
        public final String scenarioName;
        public final String description;
        public final String tags;
        public final String paymentTokenId;
        public final boolean tokenExists;
        public final String externalReference;
        public final String expectedOutcome;  // SUCCESS | FAILURE
        public final String expectedError;

        public CryptogramScenario(String scenarioId, String scenarioName, String description,
                                  String tags, String paymentTokenId, boolean tokenExists,
                                  String externalReference, String expectedOutcome,
                                  String expectedError) {
            this.scenarioId        = scenarioId;
            this.scenarioName      = scenarioName;
            this.description       = description;
            this.tags              = tags;
            this.paymentTokenId    = paymentTokenId;
            this.tokenExists       = tokenExists;
            this.externalReference = externalReference;
            this.expectedOutcome   = expectedOutcome;
            this.expectedError     = expectedError;
        }

        public boolean isSuccessExpected() {
            return "SUCCESS".equalsIgnoreCase(expectedOutcome);
        }
    }

    /** Data for a single billing-audit test scenario. */
    public static class BillingAuditScenario {
        public final String scenarioId;
        public final String scenarioName;
        public final String description;
        public final String tags;
        public final String startTime;
        public final String endTime;
        public final boolean forceSync;
        public final int    batchSize;
        public final boolean expectedSuccess;
        public final int    expectedStatus;
        public final long   expectedTotalRecords;
        public final boolean hasError;
        public final String errorMessage;

        public BillingAuditScenario(String scenarioId, String scenarioName, String description,
                                    String tags, String startTime, String endTime,
                                    boolean forceSync, int batchSize,
                                    boolean expectedSuccess, int expectedStatus,
                                    long expectedTotalRecords, boolean hasError,
                                    String errorMessage) {
            this.scenarioId           = scenarioId;
            this.scenarioName         = scenarioName;
            this.description          = description;
            this.tags                 = tags;
            this.startTime            = startTime;
            this.endTime              = endTime;
            this.forceSync            = forceSync;
            this.batchSize            = batchSize;
            this.expectedSuccess      = expectedSuccess;
            this.expectedStatus       = expectedStatus;
            this.expectedTotalRecords = expectedTotalRecords;
            this.hasError             = hasError;
            this.errorMessage         = errorMessage;
        }
    }

    // -----------------------------------------------------------------------
    // Public loaders (singleton-cached per JVM run)
    // -----------------------------------------------------------------------

    private static Map<String, NetworkTokenScenario> networkTokenCache;
    private static Map<String, CryptogramScenario>   cryptogramCache;
    private static Map<String, BillingAuditScenario> billingAuditCache;

    public static synchronized Map<String, NetworkTokenScenario> loadNetworkTokenScenarios() {
        if (networkTokenCache == null) {
            networkTokenCache = readNetworkTokenSheet();
        }
        return networkTokenCache;
    }

    public static synchronized Map<String, CryptogramScenario> loadCryptogramScenarios() {
        if (cryptogramCache == null) {
            cryptogramCache = readCryptogramSheet();
        }
        return cryptogramCache;
    }

    public static synchronized Map<String, BillingAuditScenario> loadBillingAuditScenarios() {
        if (billingAuditCache == null) {
            billingAuditCache = readBillingAuditSheet();
        }
        return billingAuditCache;
    }

    public static NetworkTokenScenario getNetworkTokenScenario(String scenarioId) {
        NetworkTokenScenario s = loadNetworkTokenScenarios().get(scenarioId);
        if (s == null) {
            throw new IllegalArgumentException(
                    "No NetworkToken scenario found in Excel for id: " + scenarioId);
        }
        return s;
    }

    public static CryptogramScenario getCryptogramScenario(String scenarioId) {
        CryptogramScenario s = loadCryptogramScenarios().get(scenarioId);
        if (s == null) {
            throw new IllegalArgumentException(
                    "No Cryptogram scenario found in Excel for id: " + scenarioId);
        }
        return s;
    }

    public static BillingAuditScenario getBillingAuditScenario(String scenarioId) {
        BillingAuditScenario s = loadBillingAuditScenarios().get(scenarioId);
        if (s == null) {
            throw new IllegalArgumentException(
                    "No BillingAudit scenario found in Excel for id: " + scenarioId);
        }
        return s;
    }

    // -----------------------------------------------------------------------
    // Sheet readers
    // -----------------------------------------------------------------------

    private static Map<String, NetworkTokenScenario> readNetworkTokenSheet() {
        Map<String, NetworkTokenScenario> map = new LinkedHashMap<>();
        try (InputStream is = openExcel(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("NetworkToken");
            if (sheet == null) {
                log.error("Sheet 'NetworkToken' not found in {}", EXCEL_PATH);
                return map;
            }
            Map<String, Integer> idx = buildHeaderIndex(sheet.getRow(0));
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row, idx.get("ScenarioId"))) continue;

                String id = str(row, idx.get("ScenarioId"));
                map.put(id, new NetworkTokenScenario(
                        id,
                        str(row, idx.get("ScenarioName")),
                        str(row, idx.get("Description")),
                        str(row, idx.get("Tags")),
                        str(row, idx.get("MerchantId")),
                        str(row, idx.get("MerchantStatus")),
                        str(row, idx.get("CardNumber")),
                        str(row, idx.get("ExpiryMonth")),
                        str(row, idx.get("ExpiryYear")),
                        str(row, idx.get("ExternalReference")),
                        str(row, idx.get("ExpectedOutcome")),
                        str(row, idx.get("ExpectedError")),
                        str(row, idx.get("ExpectedErrorType")),
                        bool(row, idx.get("IsDuplicate"))
                ));
            }
        } catch (IOException e) {
            log.error("Failed to read NetworkToken sheet from {}", EXCEL_PATH, e);
        }
        log.info("Loaded {} NetworkToken scenarios from Excel", map.size());
        return map;
    }

    private static Map<String, CryptogramScenario> readCryptogramSheet() {
        Map<String, CryptogramScenario> map = new LinkedHashMap<>();
        try (InputStream is = openExcel(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("Cryptogram");
            if (sheet == null) {
                log.error("Sheet 'Cryptogram' not found in {}", EXCEL_PATH);
                return map;
            }
            Map<String, Integer> idx = buildHeaderIndex(sheet.getRow(0));
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row, idx.get("ScenarioId"))) continue;

                String id = str(row, idx.get("ScenarioId"));
                map.put(id, new CryptogramScenario(
                        id,
                        str(row, idx.get("ScenarioName")),
                        str(row, idx.get("Description")),
                        str(row, idx.get("Tags")),
                        str(row, idx.get("PaymentTokenId")),
                        bool(row, idx.get("TokenExists")),
                        str(row, idx.get("ExternalReference")),
                        str(row, idx.get("ExpectedOutcome")),
                        str(row, idx.get("ExpectedError"))
                ));
            }
        } catch (IOException e) {
            log.error("Failed to read Cryptogram sheet from {}", EXCEL_PATH, e);
        }
        log.info("Loaded {} Cryptogram scenarios from Excel", map.size());
        return map;
    }

    private static Map<String, BillingAuditScenario> readBillingAuditSheet() {
        Map<String, BillingAuditScenario> map = new LinkedHashMap<>();
        try (InputStream is = openExcel(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheet("BillingAudit");
            if (sheet == null) {
                log.error("Sheet 'BillingAudit' not found in {}", EXCEL_PATH);
                return map;
            }
            Map<String, Integer> idx = buildHeaderIndex(sheet.getRow(0));
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row, idx.get("ScenarioId"))) continue;

                String id = str(row, idx.get("ScenarioId"));
                map.put(id, new BillingAuditScenario(
                        id,
                        str(row, idx.get("ScenarioName")),
                        str(row, idx.get("Description")),
                        str(row, idx.get("Tags")),
                        str(row, idx.get("StartTime")),
                        str(row, idx.get("EndTime")),
                        bool(row, idx.get("ForceSync")),
                        intVal(row, idx.get("BatchSize")),
                        bool(row, idx.get("ExpectedSuccess")),
                        intVal(row, idx.get("ExpectedStatus")),
                        longVal(row, idx.get("ExpectedTotalRecords")),
                        bool(row, idx.get("HasError")),
                        str(row, idx.get("ErrorMessage"))
                ));
            }
        } catch (IOException e) {
            log.error("Failed to read BillingAudit sheet from {}", EXCEL_PATH, e);
        }
        log.info("Loaded {} BillingAudit scenarios from Excel", map.size());
        return map;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static InputStream openExcel() {
        InputStream is = ExcelTestDataReader.class.getClassLoader()
                .getResourceAsStream(EXCEL_PATH);
        if (is == null) {
            throw new IllegalStateException(
                    "Excel test data not found on classpath: " + EXCEL_PATH
                    + ". Run ExcelDataGenerator#main() to create it.");
        }
        return is;
    }

    private static Map<String, Integer> buildHeaderIndex(Row headerRow) {
        Map<String, Integer> idx = new LinkedHashMap<>();
        if (headerRow == null) return idx;
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            Cell cell = headerRow.getCell(c);
            if (cell != null) {
                idx.put(cell.getStringCellValue().trim(), c);
            }
        }
        return idx;
    }

    private static boolean isBlank(Row row, Integer colIdx) {
        if (colIdx == null) return true;
        Cell cell = row.getCell(colIdx);
        return cell == null || cell.getCellType() == CellType.BLANK
                || cell.toString().isBlank();
    }

    private static String str(Row row, Integer colIdx) {
        if (colIdx == null) return "";
        Cell cell = row.getCell(colIdx);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                yield (d == Math.floor(d)) ? String.valueOf((long) d) : String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> cell.toString().trim();
        };
    }

    private static boolean bool(Row row, Integer colIdx) {
        String v = str(row, colIdx);
        return "true".equalsIgnoreCase(v) || "yes".equalsIgnoreCase(v) || "1".equals(v);
    }

    private static int intVal(Row row, Integer colIdx) {
        if (colIdx == null) return 0;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return 0;
        if (cell.getCellType() == CellType.NUMERIC) return (int) cell.getNumericCellValue();
        try { return Integer.parseInt(str(row, colIdx)); } catch (NumberFormatException e) { return 0; }
    }

    private static long longVal(Row row, Integer colIdx) {
        if (colIdx == null) return 0L;
        Cell cell = row.getCell(colIdx);
        if (cell == null) return 0L;
        if (cell.getCellType() == CellType.NUMERIC) return (long) cell.getNumericCellValue();
        try { return Long.parseLong(str(row, colIdx)); } catch (NumberFormatException e) { return 0L; }
    }
}
