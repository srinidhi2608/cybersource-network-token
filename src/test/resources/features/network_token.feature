Feature: Network Token Management
  As a merchant
  I want to create and manage network tokens
  So that I can securely process card payments

  # -------------------------------------------------------------------------
  # Test data is maintained in the Excel workbook:
  #   src/test/resources/test-data/network-token-test-scenarios.xlsx
  #   Sheet: "NetworkToken"
  #
  # To add a new test case:
  #   1. Add a row to the NetworkToken sheet in the Excel file.
  #   2. Add a matching row to the Examples table below.
  # -------------------------------------------------------------------------

  @ExcelDriven
  Scenario Outline: <description>
    Given I load network token scenario "<scenarioId>" from test data
    When I execute the network token operation
    Then the network token response should match the expected outcome

    Examples: Network Token Scenarios
      | scenarioId | description                          |
      | NT_TC_001  | Create Network Token - Happy Path    |
      | NT_TC_002  | Duplicate Token Detection            |
      | NT_TC_003  | Invalid Merchant - Not Enrolled      |
      | NT_TC_004  | Inactive Merchant                    |
      | NT_TC_005  | Missing Card Number                  |
      | NT_TC_006  | Invalid Expiry Month                 |
      | NT_TC_007  | Visa Card Token Creation             |
      | NT_TC_008  | Mastercard Token Creation            |
      | NT_TC_009  | Amex Card Token Creation             |
      | NT_TC_010  | Token Reuse After Duplicate          |

