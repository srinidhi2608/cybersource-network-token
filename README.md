# Cybersource Network Token Management System (TMS)

A comprehensive Token Management System for securely managing network tokens and cryptograms through the Cybersource API.

## Features

- **Network Token Management**: Create and manage network tokens with comprehensive audit logging
- **Duplicate Detection**: Automatic detection and handling of duplicate token requests
- **Cryptogram Generation**: Generate fresh cryptograms for existing tokens
- **Audit Trail**: Complete audit logging for all operations including duplicates
- **Merchant Enrollment**: Support for multiple enrolled merchants
- **Base64 Encryption**: Secure storage of network tokens
- **RESTful APIs**: Clean REST endpoints for token and cryptogram operations
- **Token Lifecycle Webhooks**: Listener endpoint for Cybersource token lifecycle updates with AWS SQS integration
- **Webhook Security**: HMAC-SHA256 signature validation for webhook endpoints
- **Spring Security Integration**: Stateless security with custom filters
- **Comprehensive Testing**: Unit tests, integration tests, and Cucumber BDD tests

## Architecture

### MongoDB Collections

#### 1. MerchantEnrollResponse
Stores manually enrolled merchants required for createInstrumentIdentifier calls.

```json
{
  "merchantTokenRegistrationId": "unique-merchant-id",
  "transactingOrgId": "org-id-for-cybersource",
  "merchantName": "Merchant Name",
  "status": "ACTIVE|INACTIVE|SUSPENDED",
  "enrolledAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:00"
}
```

#### 2. TokenTransactions
Contains the LATEST state of tokens. Duplicate calls do NOT create new records here.

```json
{
  "paymentTokenId": "uuid",
  "merchantTokenRegistrationId": "merchant-id",
  "networkToken": "base64-encrypted-token",
  "instrumentIdentifierId": "cybersource-instrument-id",
  "par": "payment-account-reference",
  "tokenExpiryMonth": "MM",
  "tokenExpiryYear": "YYYY",
  "tokenStatus": "ACTIVE",
  "timestamp": "2025-01-01T00:00:00"
}
```

#### 3. TokenAudits
FIRST table to save data. Every request including duplicates creates an audit.

```json
{
  "paymentTokenId": "uuid",
  "merchantTokenRegistrationId": "merchant-id",
  "instrumentIdentifierId": "cybersource-instrument-id",
  "cardExpiryMonth": "MM",
  "cardExpiryYear": "YYYY",
  "failureReason": "error-message-if-failed",
  "isDuplicate": false,
  "timestamp": "2025-01-01T00:00:00",
  "isRequestComplete": true,
  "externalReference": "transaction-ref",
  "traceId": "uuid-for-identifying-record"
}
```

**Key Rules:**
- PaymentTokenId and InstrumentIdentifier are 1:1 mapping
- Only ONE record per instrumentIdentifierId should have `isDuplicate=false`
- All subsequent requests with same instrumentIdentifierId marked as `isDuplicate=true`
- `traceId` is a unique UUID generated at insert time for record identification

#### 4. FetchInformationAudits
Records every cryptogram fetch operation and information retrieval.

```json
{
  "paymentTokenId": "uuid",
  "informationType": "InstrumentIdentifier|Cryptogram|NetworkToken",
  "eventReference": "CreateToken|FetchCryptogram|LCM",
  "timestamp": "2025-01-01T00:00:00",
  "isRequestComplete": true,
  "externalReference": "transaction-ref",
  "traceId": "uuid-for-identifying-record"
}
```

#### 5. TokenEvents
Captures lifecycle events received from Cybersource webhook notifications.

```json
{
  "traceId": "uuid",
  "paymentTokenId": "uuid",
  "tokenExpiryMonth": "MM",
  "tokenExpiryYear": "YYYY",
  "cardSuffix": "1234",
  "cardExpiryMonth": "MM",
  "cardExpiryYear": "YYYY",
  "tokenStatus": "ACTIVE",
  "lifecycleEventName": "TOKEN_UPDATED",
  "timestamp": "2025-01-01T00:00:00",
  "merchantUpdateTimestamp": "2025-01-01T00:00:00"
}
```

#### 6. BillingAudit
Consolidated billing records aggregated from TokenAudits, FetchInformationAudits, and TokenEvents.

```json
{
  "traceIdReference": "trace-id-from-source",
  "traceIdEvent": "NetworkTokenProcessing|Request Cryptogram|TokenLifeCycleManagement",
  "eventTimeStamp": "2025-01-01T00:00:00",
  "eventType": "Success|Failure",
  "merchantTokenRegistrationId": "merchant-id",
  "billingReferenceNumber": "BILL-1704153600-00000001",
  "billingBatchNumber": "uuid-for-batch",
  "timestamp": "2025-01-01T00:00:00",
  "paymentTokenId": "uuid",
  "externalReference": "transaction-ref",
  "notes": "additional-context"
}
```

#### 7. BillingAuditSyncInformation
Tracks the last successful sync timestamp for incremental billing audits.

```json
{
  "_id": "singleton-record",
  "lastSyncTimestamp": "2025-01-01T00:00:00",
  "lastSyncStartedAt": "2025-01-01T00:00:00",
  "lastSyncCompletedAt": "2025-01-01T00:00:15",
  "lastSyncRecordCount": 250,
  "lastSyncStatus": "SUCCESS|FAILURE|IN_PROGRESS",
  "version": 1,
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-01T00:00:15"
}
```

## API Documentation

### 1. Create Network Token

Creates a network token with comprehensive audit logging and duplicate detection.

**Endpoint:** `POST /api/v1/network-tokens`

**Request Body:**
```json
{
  "merchantTokenRegistrationId": "merchant-123",
  "cardNumber": "4111111111111111",
  "cardExpiryMonth": "12",
  "cardExpiryYear": "2025",
  "externalReference": "transaction-ref-001"
}
```

**Response (201 Created):**
```json
{
  "paymentTokenId": "550e8400-e29b-41d4-a716-446655440000",
  "networkToken": "4111000011110000",
  "cryptogram": "ABC123DEF456GHI789",
  "par": "PAR123456789",
  "tokenExpiryMonth": "12",
  "tokenExpiryYear": "2025",
  "tokenStatus": "ACTIVE",
  "instrumentIdentifierId": "instr-id-123"
}
```

**Flow:**
1. Validates merchant exists and is active
2. Generates UUID as paymentTokenId
3. Creates TokenAudit entry (ALWAYS, even for duplicates)
4. Calls Cybersource createInstrumentIdentifier
5. Checks if instrumentIdentifierId exists (duplicate detection)
6. If duplicate: returns existing token info, marks audit as `isDuplicate=true`
7. If new: fetches network token and cryptogram from Cybersource
8. Encrypts networkToken using Base64
9. Saves to TokenTransactions
10. Records in FetchInformationAudit
11. Updates TokenAudit as complete
12. Returns network token, cryptogram, and PAR

### 2. Create Cryptogram

Generates a fresh cryptogram for an existing payment token.

**Endpoint:** `POST /api/v1/network-tokens/cryptogram`

**Request Body:**
```json
{
  "paymentTokenId": "550e8400-e29b-41d4-a716-446655440000",
  "externalReference": "transaction-ref-002"
}
```

**Response (200 OK):**
```json
{
  "cryptogram": "XYZ789ABC123DEF456",
  "paymentTokenId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Flow:**
1. Lookups TokenTransaction by paymentTokenId
2. Gets merchant info from merchantTokenRegistrationId
3. Calls Cybersource to fetch fresh cryptogram
4. Records in FetchInformationAudit with `informationType="Cryptogram"`
5. Returns ONLY cryptogram

### 3. Update TokenAudit External Reference

Updates the external reference for a TokenAudit record identified by traceId.

**Endpoint:** `PUT /api/v1/audits/token-audits/external-reference`

**Request Body:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "externalReference": "new-transaction-ref"
}
```

**Response (200 OK):**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "externalReference": "new-transaction-ref",
  "auditType": "TokenAudit",
  "success": true,
  "message": "External reference updated successfully"
}
```

### 4. Update FetchInformationAudit External Reference

Updates the external reference for a FetchInformationAudit record identified by traceId.

**Endpoint:** `PUT /api/v1/audits/fetch-information-audits/external-reference`

**Request Body:**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "externalReference": "new-transaction-ref"
}
```

**Response (200 OK):**
```json
{
  "traceId": "550e8400-e29b-41d4-a716-446655440000",
  "externalReference": "new-transaction-ref",
  "auditType": "FetchInformationAudit",
  "success": true,
  "message": "External reference updated successfully"
}
```

### Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2025-01-01T00:00:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid request parameters",
  "details": {
    "fieldName": "error message"
  }
}
```

**Error Types:**
- `400 Bad Request` - Validation errors
- `502 Bad Gateway` - Cybersource API errors
- `503 Service Unavailable` - Network errors
- `500 Internal Server Error` - Other errors

### 5. Token Lifecycle Webhook

Receives token lifecycle update messages from Cybersource Token Management Service and forwards to AWS SQS.

**Endpoint:** `POST /api/v1/webhooks/token-lifecycle`

**Request Body:**
```json
{
  "eventType": "TOKEN_UPDATED",
  "eventTimestamp": "2024-01-15T10:30:00Z",
  "tokenInformation": {
    "tokenId": "token-123",
    "tokenStatus": "ACTIVE",
    "expirationMonth": "12",
    "expirationYear": "2025",
    "paymentAccountReference": "PAR-ABC-123"
  },
  "instrumentIdentifier": {
    "id": "instr-456",
    "state": "ACTIVE"
  },
  "organizationInformation": {
    "organizationId": "org-789",
    "merchantName": "Test Merchant"
  }
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "messageId": "sqs-message-id-123",
  "message": "Token lifecycle update processed successfully"
}
```

**SQS Queue Message (extracted fields):**
```json
{
  "tokenId": "token-123",
  "instrumentIdentifierId": "instr-456",
  "organizationId": "org-789",
  "tokenStatus": "ACTIVE",
  "eventType": "TOKEN_UPDATED",
  "eventTimestamp": "2024-01-15T10:30:00Z",
  "paymentAccountReference": "PAR-ABC-123"
}
```

**Failure Handling:**
- On processing failure, the original message is sent to the Dead Letter Queue (DLQ)
- DLQ message includes original payload, error details, and timestamp

### 6. Billing Audit Sync API

Synchronizes billing audit data from various source collections (TokenAudits, FetchInformationAudits, TokenEvents) into a consolidated BillingAudit collection for billing and reporting purposes.

**Endpoint:** `POST /api/v1/billing/audits/sync`

**Request Body (Optional):**
```json
{
  "startTime": "2025-01-01T00:00:00",
  "endTime": "2025-01-02T00:00:00",
  "forceSync": false,
  "batchSize": 1000
}
```

All fields are optional. If not provided:
- `startTime`: Uses `lastSyncTimestamp` from BillingAuditSyncInformation
- `endTime`: Current time minus 5 minutes (configurable via `billing.sync.offset-minutes`)
- `forceSync`: false (prevents concurrent syncs)
- `batchSize`: Uses configured default (1000)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Billing audit sync completed successfully",
  "billingBatchNumber": "b8e25224-cbab-483e-ba71-7ec446e59001",
  "syncStartTime": "2025-01-01T00:00:00",
  "syncEndTime": "2025-01-01T23:55:00",
  "operationStartedAt": "2025-01-02T00:00:00",
  "operationCompletedAt": "2025-01-02T00:00:15",
  "tokenAuditCount": 150,
  "fetchInfoAuditCount": 75,
  "tokenEventsCount": 25,
  "totalRecordsCreated": 250,
  "sampleBillingReferenceNumbers": [
    "BILL-1704153600-00000001",
    "BILL-1704153600-00000002",
    "BILL-1704153600-00000003"
  ]
}
```

**Key Features:**
- **Unique Billing Reference Numbers**: Generated using MongoDB atomic counter (format: `BILL-{timestamp}-{sequence}`)
- **Batch Processing**: All records in one sync share the same `billingBatchNumber` (UUID)
- **Event Type Mapping**:
  - TokenAudit → `traceIdEvent = "NetworkTokenProcessing"`
  - FetchInformationAudit (eventReference="FetchCryptogram") → `traceIdEvent = "Request Cryptogram"`
  - FetchInformationAudit (eventReference="LCM") → `traceIdEvent = "Request Cryptogram"`
  - TokenEvents → `traceIdEvent = "TokenLifeCycleManagement"`
- **Event Status**: Determined from `isRequestComplete` and `failureReason` fields (Success/Failure)
- **Incremental Sync**: Only fetches data since last successful sync
- **Concurrency Safe**: Uses optimistic locking to prevent concurrent syncs

**Health Check Endpoint:**
```bash
curl http://localhost:8080/api/v1/billing/audits/health
```

**BillingAudit Collection Schema:**
```json
{
  "_id": "ObjectId",
  "traceIdReference": "trace-id-from-source-collection",
  "traceIdEvent": "NetworkTokenProcessing|Request Cryptogram|TokenLifeCycleManagement",
  "eventTimeStamp": "2025-01-01T00:00:00",
  "eventType": "Success|Failure",
  "merchantTokenRegistrationId": "merchant-123",
  "billingReferenceNumber": "BILL-1704153600-00000001",
  "billingBatchNumber": "uuid-for-this-sync-operation",
  "timestamp": "2025-01-02T00:00:00",
  "paymentTokenId": "optional-payment-token-id",
  "externalReference": "optional-external-ref",
  "notes": "additional-context"
}
```

**BillingAuditSyncInformation Collection:**
```json
{
  "_id": "singleton-record",
  "lastSyncTimestamp": "2025-01-01T23:55:00",
  "lastSyncStartedAt": "2025-01-02T00:00:00",
  "lastSyncCompletedAt": "2025-01-02T00:00:15",
  "lastSyncRecordCount": 250,
  "lastSyncStatus": "SUCCESS|FAILURE|IN_PROGRESS",
  "version": 1,
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": "2025-01-02T00:00:15"
}
```

**Example cURL:**
```bash
# Sync with default parameters (since last sync to now - 5 minutes)
curl -X POST http://localhost:8080/api/v1/billing/audits/sync \
  -H "Content-Type: application/json"

# Sync with custom time window
curl -X POST http://localhost:8080/api/v1/billing/audits/sync \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2025-01-01T00:00:00",
    "endTime": "2025-01-02T00:00:00"
  }'
```

### 7. Get Merchant List by Month and Year

Retrieves a list of unique merchant token registration IDs from the BillingAudit collection for a specific month and year.

**Endpoint:** `GET /api/v1/billing/merchants/{month}/{year}`

**Path Parameters:**
- `month`: Month (1-12)
- `year`: Year (2020-2100)

**Response (200 OK):**
```json
[
  "merchant-001",
  "merchant-002",
  "merchant-003"
]
```

**Empty Result (200 OK):**
```json
[]
```

**Validation Error (400 Bad Request):**
- Invalid month (not between 1-12)
- Invalid year (not between 2020-2100)

**Example cURL:**
```bash
# Get merchants for January 2025
curl -X GET http://localhost:8080/api/v1/billing/merchants/1/2025

# Get merchants for December 2025
curl -X GET http://localhost:8080/api/v1/billing/merchants/12/2025
```

**Key Features:**
- Returns distinct and sorted merchant IDs
- Filters by `eventTimeStamp` month and year
- Efficient MongoDB query with projection
- Null and blank merchant IDs are filtered out

### 8. Get Token Events by Merchant

Retrieves token event information for a specific merchant and time period from the BillingAudit collection.

**Endpoint:** `GET /api/v1/billing/token-events/{merchantTokenRegistrationId}/{month}/{year}`

**Path Parameters:**
- `merchantTokenRegistrationId`: Merchant token registration ID
- `month`: Month (1-12)
- `year`: Year (2020-2100)

**Response (200 OK):**
```json
[
  {
    "traceId": "trace-001",
    "transactionType": "NetworkTokenProcessing",
    "timestamp": "2025-01-15T10:30:00",
    "eventStatus": true
  },
  {
    "traceId": "trace-002",
    "transactionType": "Request Cryptogram",
    "timestamp": "2025-01-20T14:45:00",
    "eventStatus": false
  },
  {
    "traceId": "trace-003",
    "transactionType": "TokenLifeCycleManagement",
    "timestamp": "2025-01-25T16:00:00",
    "eventStatus": true
  }
]
```

**TokenInfo Fields:**
- `traceId`: Trace ID reference from the billing audit record
- `transactionType`: Event type (NetworkTokenProcessing, Request Cryptogram, TokenLifeCycleManagement)
- `timestamp`: Event timestamp
- `eventStatus`: Boolean indicating success (true) or failure (false)

**Empty Result (200 OK):**
```json
[]
```

**Validation Errors (400 Bad Request):**
- Null or empty merchant token registration ID
- Invalid month (not between 1-12)
- Invalid year (not between 2020-2100)

**Example cURL:**
```bash
# Get token events for merchant-001 in January 2025
curl -X GET http://localhost:8080/api/v1/billing/token-events/merchant-001/1/2025

# Get token events for merchant-002 in December 2025
curl -X GET http://localhost:8080/api/v1/billing/token-events/merchant-002/12/2025
```

**Key Features:**
- Returns all token events for the specified merchant and time period
- Maps `eventType` to boolean `eventStatus` (Success → true, Failure → false)
- Filters by `eventTimeStamp` month and year
- Handles leap years and month boundaries correctly

# Sync with custom time window
curl -X POST http://localhost:8080/api/v1/billing/audits/sync \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2025-01-01T00:00:00",
    "endTime": "2025-01-02T00:00:00"
  }'
```

**Error Response (500):**
```json
{
  "success": false,
  "message": "Billing audit sync failed",
  "errorMessage": "Another sync operation is in progress. Please try again later."
}
```

## Security

### Webhook Signature Validation

The Token Lifecycle webhook endpoint is secured using HMAC-SHA256 signature validation to ensure authenticity and prevent tampering.

**How it works:**
1. Client generates HMAC-SHA256 hash of request body using shared secret
2. Client sends signature in `v-c-signature` header
3. Server validates signature before processing request
4. Invalid or missing signatures return 401 Unauthorized

**Example request:**
```bash
SECRET="your-webhook-secret"
BODY='{"eventType":"TOKEN_UPDATED","tokenId":"123"}'
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

curl -X POST http://localhost:8080/api/v1/webhooks/token-lifecycle \
  -H "Content-Type: application/json" \
  -H "v-c-signature: $SIGNATURE" \
  -d "$BODY"
```

📖 **For detailed security documentation, see [WEBHOOK_SECURITY_GUIDE.md](WEBHOOK_SECURITY_GUIDE.md)**

Includes:
- Client implementation examples (Java, Python, Node.js, Bash)
- Security best practices
- Troubleshooting guide
- Production checklist

## Setup Instructions

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+
- Cybersource API credentials

### Configuration

1. Update `src/main/resources/application.properties`:

```properties
# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=cybersource_tms

# Cybersource API Configuration
cybersource.key-id=your-key-id
cybersource.api-key=your-api-key
cybersource.secret-key=your-secret-key
cybersource.merchant-id=your-merchant-id
cybersource.base-url=https://apitest.cybersource.com

# AWS SQS Configuration
aws.sqs.region=us-east-1
aws.sqs.queue-url=https://sqs.us-east-1.amazonaws.com/your-account/token-lifecycle-queue
aws.sqs.dlq-url=https://sqs.us-east-1.amazonaws.com/your-account/token-lifecycle-dlq

# Webhook Security Configuration
webhook.signature.secret=YOUR_WEBHOOK_SECRET_KEY

# Billing Audit Sync Configuration
billing.sync.offset-minutes=5
billing.sync.batch-size=1000
billing.sequence.collection=BillingSequence
```

**Important**: Never commit secrets to version control! Use environment variables in production:
```properties
webhook.signature.secret=${WEBHOOK_SECRET_ENV_VAR}
```

2. Ensure MongoDB is running:
```bash
# Using Docker
docker run -d -p 27017:27017 --name mongodb mongo:latest

# Or using local installation
mongod --dbpath /path/to/data
```

### Building the Application

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Run Cucumber BDD tests
mvn test -Dtest=CucumberTestRunner

# Package
mvn clean package
```

### Running the Application

```bash
# Run Spring Boot application
mvn spring-boot:run

# Or run the JAR
java -jar target/cybersourceCashflows-0.0.1-SNAPSHOT.jar
```

The application will start on `http://localhost:8080`

### Testing the APIs

#### Using cURL

**Create Network Token:**
```bash
curl -X POST http://localhost:8080/api/v1/network-tokens \
  -H "Content-Type: application/json" \
  -d '{
    "merchantTokenRegistrationId": "merchant-123",
    "cardNumber": "4111111111111111",
    "cardExpiryMonth": "12",
    "cardExpiryYear": "2025",
    "externalReference": "ref-001"
  }'
```

**Create Cryptogram:**
```bash
curl -X POST http://localhost:8080/api/v1/network-tokens/cryptogram \
  -H "Content-Type: application/json" \
  -d '{
    "paymentTokenId": "550e8400-e29b-41d4-a716-446655440000",
    "externalReference": "ref-002"
  }'
```

## Testing

### Unit Tests

The project includes comprehensive unit tests with >80% code coverage:

```bash
mvn test
```

**Test Classes:**
- `EncryptionServiceTest` - Tests Base64 encryption/decryption (9 tests)
- `NetworkTokenServiceTest` - Tests network token service logic (7 tests)
- `AuditServiceTest` - Tests audit update operations (7 tests)
- `TokenLifecycleServiceTest` - Tests webhook processing and SQS integration (8 tests)
- `BillingReferenceNumberGeneratorTest` - Tests unique reference number generation (5 tests)
- `BillingAuditSyncServiceTest` - Tests billing audit sync logic (11 tests)

**Total:** 47+ unit tests with >80% code coverage

### Cucumber BDD Tests

Behavioral tests using Cucumber:

```bash
mvn test -Dtest=CucumberTestRunner
```

**Feature File:** `src/test/resources/features/network_token.feature`

**Scenarios:**
1. Successfully create a network token
2. Handle duplicate network token creation
3. Create cryptogram for existing token
4. Validation and error scenarios

### Integration Tests

Integration tests with Testcontainers for MongoDB:

```bash
mvn verify
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/cybersource/
│   │       ├── config/          # Configuration classes
│   │       │   ├── RestClientConfig.java
│   │       │   ├── SecurityConfig.java
│   │       │   └── AwsSqsConfig.java
│   │       ├── constants/       # Constants
│   │       │   └── BillingConstants.java
│   │       ├── controller/      # REST endpoints
│   │       │   ├── NetworkTokenController.java
│   │       │   ├── AuditController.java
│   │       │   ├── TokenLifecycleController.java
│   │       │   └── BillingAuditController.java
│   │       ├── dto/             # Request/Response objects
│   │       │   ├── CreateNetworkTokenRequest.java
│   │       │   ├── CreateNetworkTokenResponse.java
│   │       │   ├── CreateCryptogramRequest.java
│   │       │   ├── CreateCryptogramResponse.java
│   │       │   ├── BillingAuditSyncRequest.java
│   │       │   └── BillingAuditSyncResponse.java
│   │       ├── entity/          # MongoDB documents
│   │       │   ├── MerchantEnrollResponse.java
│   │       │   ├── TokenTransaction.java
│   │       │   ├── TokenAudit.java
│   │       │   ├── FetchInformationAudit.java
│   │       │   ├── TokenEvents.java
│   │       │   ├── BillingAudit.java
│   │       │   └── BillingAuditSyncInformation.java
│   │       ├── enums/           # Enumerations
│   │       │   ├── TraceIdEventType.java
│   │       │   ├── EventType.java
│   │       │   └── EventReference.java
│   │       ├── filter/          # Security filters
│   │       │   ├── WebhookSignatureValidationFilter.java
│   │       │   └── CachedBodyHttpServletRequest.java
│   │       ├── repository/      # Data access
│   │       │   ├── TokenAuditRepository.java
│   │       │   ├── BillingAuditRepository.java
│   │       │   └── ... (other repositories)
│   │       ├── service/         # Business logic
│   │       │   ├── NetworkTokenService.java
│   │       │   ├── EncryptionService.java
│   │       │   ├── AuditService.java
│   │       │   ├── TokenLifecycleService.java
│   │       │   ├── BillingAuditSyncService.java
│   │       │   └── CybersourceRestClient.java
│   │       ├── exception/       # Custom exceptions
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── ErrorResponse.java
│   │       └── util/            # Utility classes
│   │           ├── SignatureValidator.java
│   │           └── BillingReferenceNumberGenerator.java
│   └── resources/
│       └── application.properties
└── test/
    ├── java/
    │   └── com/example/cybersource/
    │       ├── cucumber/        # BDD tests
    │       ├── filter/          # Filter tests
    │       ├── service/         # Unit tests
    │       └── util/            # Utility tests
    └── resources/
        └── features/            # Cucumber feature files
```

## Key Technologies

- **Spring Boot 3.5.3** - Application framework
- **Spring Data MongoDB** - Database integration
- **Spring RestClient** - HTTP client for Cybersource API
- **Jakarta Validation** - Request validation
- **Lombok** - Reduces boilerplate code
- **Cybersource REST SDK** - Cybersource integration
- **JUnit 5** - Unit testing
- **Mockito** - Mocking framework
- **Cucumber** - BDD testing
- **Testcontainers** - Integration testing

## Security Considerations

1. **Token Encryption**: Network tokens are encrypted using Base64 before storage
2. **JWT Authentication**: Cybersource API calls use JWT tokens
3. **Sensitive Data**: Never log full card numbers or tokens
4. **HTTPS**: Always use HTTPS in production
5. **Secrets Management**: Use environment variables for API credentials

## Monitoring and Logging

The application uses SLF4J for logging with the following levels:

- `INFO` - Normal operations, API calls
- `WARN` - Validation warnings
- `ERROR` - Failures, exceptions

Configure logging in `application.properties`:
```properties
logging.level.com.example.cybersource=INFO
logging.level.org.springframework.web=INFO
```

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License.

## Support

For issues and questions:
- Create an issue in the GitHub repository
- Contact the development team

## Acknowledgments

- Cybersource REST API documentation
- Spring Boot community
- MongoDB community
