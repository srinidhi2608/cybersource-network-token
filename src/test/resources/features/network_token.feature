Feature: Network Token Management
  As a merchant
  I want to create and manage network tokens
  So that I can securely process card payments

  Background:
    Given the merchant "merchant-123" is enrolled and active

  Scenario: Successfully create a network token
    Given I have a valid card number "4111111111111111"
    And the card expires on "12/2025"
    When I request to create a network token with external reference "ref-001"
    Then the network token should be created successfully
    And the response should contain a payment token ID
    And the response should contain a network token
    And the response should contain a cryptogram
    And the response should contain a PAR
    And an audit record should be created with isDuplicate set to false

  Scenario: Handle duplicate network token creation
    Given I have a valid card number "4111111111111111"
    And the card expires on "12/2025"
    And a network token already exists for this card
    When I request to create a network token with external reference "ref-002"
    Then the existing network token should be returned
    And the response should contain the original payment token ID
    And a fresh cryptogram should be generated
    And an audit record should be created with isDuplicate set to true

  Scenario: Create cryptogram for existing token
    Given a network token exists with payment token ID "token-123"
    When I request to create a cryptogram with external reference "ref-003"
    Then a new cryptogram should be generated successfully
    And the response should only contain the cryptogram and payment token ID
    And a fetch information audit should be created with type "Cryptogram"

  Scenario: Fail to create network token with invalid merchant
    Given the merchant "invalid-merchant" is not enrolled
    And I have a valid card number "4111111111111111"
    And the card expires on "12/2025"
    When I request to create a network token with external reference "ref-004"
    Then the request should fail with error "Merchant not found"

  Scenario: Fail to create network token with inactive merchant
    Given the merchant "merchant-inactive" is enrolled but inactive
    And I have a valid card number "4111111111111111"
    And the card expires on "12/2025"
    When I request to create a network token with external reference "ref-005"
    Then the request should fail with error "Merchant is not active"

  Scenario: Fail to create cryptogram for non-existent token
    Given no network token exists with payment token ID "non-existent-token"
    When I request to create a cryptogram with external reference "ref-006"
    Then the request should fail with error "Token not found"

  Scenario: Validation error with missing card number
    Given I have an empty card number
    And the card expires on "12/2025"
    When I request to create a network token with external reference "ref-007"
    Then the request should fail with validation error "cardNumber is required"

  Scenario: Validation error with invalid expiry month
    Given I have a valid card number "4111111111111111"
    And the card expires on "13/2025"
    When I request to create a network token with external reference "ref-008"
    Then the request should fail with validation error "cardExpiryMonth must be in MM format"
