Feature: Cryptogram Management
  As a merchant
  I want to generate cryptograms for existing network tokens
  So that I can authorise transactions securely

  # -------------------------------------------------------------------------
  # Test data is maintained in the Excel workbook:
  #   src/test/resources/test-data/network-token-test-scenarios.xlsx
  #   Sheet: "Cryptogram"
  #
  # To add a new test case:
  #   1. Add a row to the Cryptogram sheet in the Excel file.
  #   2. Add a matching row to the Examples table below.
  # -------------------------------------------------------------------------

  @ExcelDriven
  Scenario Outline: <description>
    Given I load cryptogram scenario "<scenarioId>" from test data
    When I execute the cryptogram operation
    Then the cryptogram response should match the expected outcome

    Examples: Cryptogram Scenarios
      | scenarioId | description                               |
      | CR_TC_001  | Generate Cryptogram - Happy Path          |
      | CR_TC_002  | Cryptogram for Non-Existent Token         |
      | CR_TC_003  | Multiple Cryptograms for Same Token       |
      | CR_TC_004  | Cryptogram Response Fields                |
