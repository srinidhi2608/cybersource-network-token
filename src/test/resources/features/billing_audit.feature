Feature: Billing Audit Management
  As a finance team member
  I want to synchronise billing audit records
  So that I can track token and cryptogram usage for billing purposes

  # -------------------------------------------------------------------------
  # Test data is maintained in the Excel workbook:
  #   src/test/resources/test-data/network-token-test-scenarios.xlsx
  #   Sheet: "BillingAudit"
  #
  # To add a new test case:
  #   1. Add a row to the BillingAudit sheet in the Excel file.
  #   2. Add a matching row to the Examples table below.
  # -------------------------------------------------------------------------

  @ExcelDriven
  Scenario Outline: <description>
    Given I load billing audit scenario "<scenarioId>" from test data
    When I execute the billing audit sync operation
    Then the billing audit response should match the expected outcome

    Examples: Billing Audit Scenarios
      | scenarioId | description                        |
      | BA_TC_001  | Sync With Default Parameters       |
      | BA_TC_002  | Sync With Custom Start Time        |
      | BA_TC_003  | Sync With Both Times               |
      | BA_TC_004  | Force Sync Enabled                 |
      | BA_TC_005  | Custom Batch Size                  |
      | BA_TC_006  | Full Request                       |
      | BA_TC_007  | Cybersource Exception              |
      | BA_TC_008  | Generic Exception                  |
      | BA_TC_009  | Empty Time Window                  |
      | BA_TC_010  | Future Time Window                 |
      | BA_TC_011  | Health Check Endpoint              |
