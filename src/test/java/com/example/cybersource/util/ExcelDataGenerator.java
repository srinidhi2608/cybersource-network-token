package com.example.cybersource.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Standalone generator for the Excel test-data workbook used by Cucumber.
 *
 * <p><b>Usage:</b> Run {@link #main(String[])} once (or whenever you want to
 * reset the workbook to the baseline).  The generated file is placed at:<br>
 * {@code src/test/resources/test-data/network-token-test-scenarios.xlsx}
 *
 * <p>To add new test cases, either:
 * <ol>
 *   <li><b>Edit the Excel file directly</b> in Excel / LibreOffice and add new rows
 *       to the appropriate sheet, then update the matching
 *       {@code Examples:} table in the corresponding {@code .feature} file.</li>
 *   <li><b>Re-run this generator</b> after adding entries to the data arrays below.</li>
 * </ol>
 *
 * <p><b>Sheet → column layout</b>
 * <pre>
 * Sheet "NetworkToken":
 *   ScenarioId | ScenarioName | Description | Tags | MerchantId | MerchantStatus
 *   CardNumber | ExpiryMonth  | ExpiryYear  | ExternalReference
 *   ExpectedOutcome | ExpectedError | ExpectedErrorType | IsDuplicate
 *
 * Sheet "Cryptogram":
 *   ScenarioId | ScenarioName | Description | Tags | PaymentTokenId | TokenExists
 *   ExternalReference | ExpectedOutcome | ExpectedError
 *
 * Sheet "BillingAudit":
 *   ScenarioId | ScenarioName | Description | Tags | StartTime | EndTime
 *   ForceSync  | BatchSize | ExpectedSuccess | ExpectedStatus
 *   ExpectedTotalRecords | HasError | ErrorMessage
 * </pre>
 */
public class ExcelDataGenerator {

    /** Output path relative to the Maven project root. */
    private static final String OUTPUT_PATH =
            "src/test/resources/test-data/network-token-test-scenarios.xlsx";

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        Path out = Paths.get(OUTPUT_PATH);
        Files.createDirectories(out.getParent());

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            createNetworkTokenSheet(wb);
            createCryptogramSheet(wb);
            createBillingAuditSheet(wb);

            try (FileOutputStream fos = new FileOutputStream(out.toFile())) {
                wb.write(fos);
            }
        }
        System.out.println("Excel test data created at: " + out.toAbsolutePath());
    }

    // -----------------------------------------------------------------------
    // Sheet builders
    // -----------------------------------------------------------------------

    private static void createNetworkTokenSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("NetworkToken");

        String[] headers = {
                "ScenarioId", "ScenarioName", "Description", "Tags",
                "MerchantId", "MerchantStatus",
                "CardNumber", "ExpiryMonth", "ExpiryYear", "ExternalReference",
                "ExpectedOutcome", "ExpectedError", "ExpectedErrorType", "IsDuplicate"
        };

        // ---- test rows: ScenarioId, ScenarioName, Description, Tags,
        //                  MerchantId, MerchantStatus,
        //                  CardNumber, ExpiryMonth, ExpiryYear, ExternalReference,
        //                  ExpectedOutcome, ExpectedError, ExpectedErrorType, IsDuplicate
        Object[][] data = {
                {"NT_TC_001", "Create Network Token - Happy Path",
                 "Successfully create a new network token for a valid card",
                 "@smoke @happy-path",
                 "merchant-123", "ACTIVE",
                 "4111111111111111", "12", "2025", "ref-001",
                 "SUCCESS", "", "", false},

                {"NT_TC_002", "Duplicate Token Detection",
                 "Return existing token when the same card is tokenised again",
                 "@regression @duplicate",
                 "merchant-123", "ACTIVE",
                 "4111111111111111", "12", "2025", "ref-002",
                 "SUCCESS", "", "", true},

                {"NT_TC_003", "Invalid Merchant - Not Enrolled",
                 "Fail gracefully when merchant is not enrolled",
                 "@negative @merchant",
                 "invalid-merchant", "NOT_ENROLLED",
                 "4111111111111111", "12", "2025", "ref-003",
                 "FAILURE", "Merchant not found", "CybersourceException", false},

                {"NT_TC_004", "Inactive Merchant",
                 "Fail gracefully when merchant account is inactive",
                 "@negative @merchant",
                 "merchant-inactive", "INACTIVE",
                 "4111111111111111", "12", "2025", "ref-004",
                 "FAILURE", "Merchant is not active", "CybersourceException", false},

                {"NT_TC_005", "Missing Card Number",
                 "Service accepts empty cardNumber (validation is enforced at controller level by @Valid)",
                 "@negative @validation",
                 "merchant-123", "ACTIVE",
                 "", "12", "2025", "ref-005",
                 "SUCCESS", "", "", false},

                {"NT_TC_006", "Invalid Expiry Month",
                 "Service accepts invalid expiryMonth (validation is enforced at controller level by @Valid)",
                 "@negative @validation",
                 "merchant-123", "ACTIVE",
                 "4111111111111111", "13", "2025", "ref-006",
                 "SUCCESS", "", "", false},

                {"NT_TC_007", "Visa Card Token Creation",
                 "Create a network token for a Visa card",
                 "@regression @visa",
                 "merchant-123", "ACTIVE",
                 "4000056655665556", "06", "2026", "ref-007",
                 "SUCCESS", "", "", false},

                {"NT_TC_008", "Mastercard Token Creation",
                 "Create a network token for a Mastercard",
                 "@regression @mastercard",
                 "merchant-123", "ACTIVE",
                 "5425233430109903", "08", "2027", "ref-008",
                 "SUCCESS", "", "", false},

                {"NT_TC_009", "Amex Card Token Creation",
                 "Create a network token for an American Express card",
                 "@regression @amex",
                 "merchant-123", "ACTIVE",
                 "3714496353984312", "03", "2026", "ref-009",
                 "SUCCESS", "", "", false},

                {"NT_TC_010", "Token Reuse After Duplicate",
                 "Third request for same card still returns existing token",
                 "@regression @duplicate",
                 "merchant-123", "ACTIVE",
                 "4111111111111111", "12", "2025", "ref-010",
                 "SUCCESS", "", "", true},
        };

        writeSheet(wb, sheet, headers, data);
    }

    private static void createCryptogramSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("Cryptogram");

        String[] headers = {
                "ScenarioId", "ScenarioName", "Description", "Tags",
                "PaymentTokenId", "TokenExists", "ExternalReference",
                "ExpectedOutcome", "ExpectedError"
        };

        Object[][] data = {
                {"CR_TC_001", "Generate Cryptogram - Happy Path",
                 "Generate a fresh cryptogram for an existing payment token",
                 "@smoke @happy-path",
                 "token-123", true, "ref-101",
                 "SUCCESS", ""},

                {"CR_TC_002", "Cryptogram for Non-Existent Token",
                 "Fail gracefully when the payment token does not exist",
                 "@negative",
                 "non-existent-token", false, "ref-102",
                 "FAILURE", "Token not found"},

                {"CR_TC_003", "Multiple Cryptograms for Same Token",
                 "Each call returns a fresh cryptogram (cryptograms are single-use)",
                 "@regression",
                 "token-456", true, "ref-103",
                 "SUCCESS", ""},

                {"CR_TC_004", "Cryptogram Response Fields",
                 "Response must include both cryptogram and paymentTokenId",
                 "@regression @schema",
                 "token-789", true, "ref-104",
                 "SUCCESS", ""},
        };

        writeSheet(wb, sheet, headers, data);
    }

    private static void createBillingAuditSheet(XSSFWorkbook wb) {
        XSSFSheet sheet = wb.createSheet("BillingAudit");

        String[] headers = {
                "ScenarioId", "ScenarioName", "Description", "Tags",
                "StartTime", "EndTime", "ForceSync", "BatchSize",
                "ExpectedSuccess", "ExpectedStatus", "ExpectedTotalRecords",
                "HasError", "ErrorMessage"
        };

        Object[][] data = {
                {"BA_TC_001", "Sync With Default Parameters",
                 "Sync billing audit with null / default request body",
                 "@smoke @happy-path",
                 "", "", false, 1000,
                 true, 200, 10, false, ""},

                {"BA_TC_002", "Sync With Custom Start Time",
                 "Sync with a custom startTime filter",
                 "@regression",
                 "2025-01-01T00:00:00", "", false, 1000,
                 true, 200, 15, false, ""},

                {"BA_TC_003", "Sync With Both Times",
                 "Sync within an explicit [startTime, endTime] window",
                 "@regression",
                 "2025-01-01T00:00:00", "2025-01-02T00:00:00", false, 1000,
                 true, 200, 20, false, ""},

                {"BA_TC_004", "Force Sync Enabled",
                 "Sync with forceSync=true overrides last-sync-timestamp check",
                 "@regression @forceSync",
                 "2025-01-01T00:00:00", "2025-01-02T00:00:00", true, 1000,
                 true, 200, 20, false, ""},

                {"BA_TC_005", "Custom Batch Size",
                 "Sync with a smaller batch size of 500",
                 "@regression",
                 "", "", false, 500,
                 true, 200, 8, false, ""},

                {"BA_TC_006", "Full Request",
                 "Sync with all parameters specified",
                 "@regression @full",
                 "2025-01-01T00:00:00", "2025-01-02T00:00:00", true, 500,
                 true, 200, 25, false, ""},

                {"BA_TC_007", "Cybersource Exception",
                 "Service throws OptimisticLockingFailureException → response must surface CybersourceException",
                 "@negative @error",
                 "2025-01-01T00:00:00", "2025-01-02T00:00:00", false, 1000,
                 false, 500, 0, true, "Another sync operation is in progress"},

                {"BA_TC_008", "Generic Exception",
                 "Service throws unexpected exception — response must be HTTP 500",
                 "@negative @error",
                 "2025-02-01T00:00:00", "2025-02-02T00:00:00", false, 1000,
                 false, 500, 0, true, "Unexpected error"},

                {"BA_TC_009", "Empty Time Window",
                 "Same startTime and endTime → zero records synced",
                 "@edge-case",
                 "2025-01-01T00:00:00", "2025-01-01T00:00:00", false, 1000,
                 true, 200, 0, false, ""},

                {"BA_TC_010", "Future Time Window",
                 "Future dates → zero records synced but no error",
                 "@edge-case",
                 "2025-12-31T00:00:00", "2025-12-31T23:59:59", false, 1000,
                 true, 200, 0, false, ""},

                {"BA_TC_011", "Health Check Endpoint",
                 "Billing audit health check returns 200 OK",
                 "@smoke @health",
                 "", "", false, 0,
                 true, 200, 0, false, ""},
        };

        writeSheet(wb, sheet, headers, data);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static void writeSheet(XSSFWorkbook wb, XSSFSheet sheet,
                                   String[] headers, Object[][] data) {
        CellStyle headerStyle  = buildHeaderStyle(wb);
        CellStyle dataStyle    = buildDataStyle(wb);
        CellStyle altDataStyle = buildAltDataStyle(wb);

        // Header row
        Row headerRow = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (int r = 0; r < data.length; r++) {
            Row row   = sheet.createRow(r + 1);
            CellStyle style = (r % 2 == 0) ? dataStyle : altDataStyle;
            for (int c = 0; c < data[r].length; c++) {
                Cell cell = row.createCell(c);
                Object val = data[r][c];
                if (val instanceof String)  cell.setCellValue((String)  val);
                else if (val instanceof Boolean) cell.setCellValue((Boolean) val);
                else if (val instanceof Integer) cell.setCellValue((Integer) val);
                else if (val instanceof Long)    cell.setCellValue((Long)    val);
                else if (val != null)       cell.setCellValue(val.toString());
                cell.setCellStyle(style);
            }
        }

        // Auto-size first four columns (id, name, description, tags)
        for (int c = 0; c < Math.min(headers.length, 4); c++) {
            sheet.autoSizeColumn(c);
        }

        // Freeze pane: keep header row visible while scrolling
        sheet.createFreezePane(0, 1);

        // Auto-filter on header row
        sheet.setAutoFilter(new CellRangeAddress(0, data.length, 0, headers.length - 1));
    }

    private static CellStyle buildHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle buildDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private static CellStyle buildAltDataStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }
}
