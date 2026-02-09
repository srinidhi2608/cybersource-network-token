# Webhook Security Implementation Guide

## Overview

The Token Lifecycle webhook endpoint (`/api/v1/webhooks/token-lifecycle`) is secured using HMAC-SHA256 signature validation. This ensures that requests are authentic and haven't been tampered with during transmission.

## How It Works

### 1. Signature Generation (Client Side)

When Cybersource sends a webhook request, they generate a signature:

```
1. Take the complete request body (JSON string)
2. Generate HMAC-SHA256 hash using the shared secret key
3. Base64 encode the hash
4. Include the signature in the "v-c-signature" header
```

### 2. Signature Validation (Server Side)

Our filter validates incoming requests:

```
1. Extract the "v-c-signature" header
2. Read the request body
3. Generate expected signature using our secret key
4. Compare signatures using constant-time comparison
5. Allow/reject request based on match
```

## Configuration

### Application Properties

```properties
# Webhook Security Configuration
webhook.signature.secret=YOUR_WEBHOOK_SECRET_KEY
```

⚠️ **Important**: Keep this secret secure and never commit it to version control!

### Environment-Specific Configuration

**Development:**
```properties
webhook.signature.secret=dev-secret-key-change-in-production
```

**Production:**
```properties
webhook.signature.secret=${WEBHOOK_SECRET_ENV_VAR}
```

Use environment variables or secret management systems (AWS Secrets Manager, HashiCorp Vault, etc.) for production.

## Client Implementation Examples

### Java

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WebhookSignature {
    
    public static String generateSignature(String requestBody, String secret) 
            throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(
            secret.getBytes(StandardCharsets.UTF_8), 
            "HmacSHA256"
        );
        mac.init(secretKeySpec);
        
        byte[] hash = mac.doFinal(requestBody.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hash);
    }
    
    public static void main(String[] args) throws Exception {
        String body = "{\"eventType\":\"TOKEN_UPDATED\",\"tokenId\":\"123\"}";
        String secret = "your-secret-key";
        String signature = generateSignature(body, secret);
        
        // Include signature in request header: v-c-signature
        System.out.println("Signature: " + signature);
    }
}
```

### Python

```python
import hmac
import hashlib
import base64
import json

def generate_signature(request_body, secret):
    """Generate HMAC-SHA256 signature for webhook request"""
    message = request_body.encode('utf-8')
    secret_bytes = secret.encode('utf-8')
    
    signature = hmac.new(secret_bytes, message, hashlib.sha256).digest()
    return base64.b64encode(signature).decode('utf-8')

# Example usage
body = json.dumps({
    "eventType": "TOKEN_UPDATED",
    "tokenId": "123"
})
secret = "your-secret-key"
signature = generate_signature(body, secret)

# Make request
import requests
response = requests.post(
    'http://localhost:8080/api/v1/webhooks/token-lifecycle',
    headers={
        'Content-Type': 'application/json',
        'v-c-signature': signature
    },
    data=body
)
```

### Node.js

```javascript
const crypto = require('crypto');

function generateSignature(requestBody, secret) {
    const hmac = crypto.createHmac('sha256', secret);
    hmac.update(requestBody);
    return hmac.digest('base64');
}

// Example usage
const body = JSON.stringify({
    eventType: 'TOKEN_UPDATED',
    tokenId: '123'
});

const secret = 'your-secret-key';
const signature = generateSignature(body, secret);

// Make request using fetch or axios
fetch('http://localhost:8080/api/v1/webhooks/token-lifecycle', {
    method: 'POST',
    headers: {
        'Content-Type': 'application/json',
        'v-c-signature': signature
    },
    body: body
});
```

### Bash/cURL

```bash
#!/bin/bash

SECRET="your-secret-key"
BODY='{"eventType":"TOKEN_UPDATED","tokenId":"123"}'

# Generate signature
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)

# Make request
curl -X POST http://localhost:8080/api/v1/webhooks/token-lifecycle \
  -H "Content-Type: application/json" \
  -H "v-c-signature: $SIGNATURE" \
  -d "$BODY"
```

## API Response Codes

### Success
- **200 OK**: Signature valid, request processed successfully

### Client Errors
- **400 Bad Request**: Empty request body
- **401 Unauthorized**: Missing or invalid signature

### Server Errors
- **500 Internal Server Error**: Processing error (after successful validation)

## Error Responses

### Missing Signature
```json
{
  "error": "Missing signature header"
}
```

### Invalid Signature
```json
{
  "error": "Invalid signature"
}
```

### Empty Body
```json
{
  "error": "Empty request body"
}
```

## Security Considerations

### 1. Secret Key Management
- ✅ Use strong, random secrets (at least 32 characters)
- ✅ Rotate secrets periodically
- ✅ Store secrets securely (environment variables, secret managers)
- ❌ Never commit secrets to version control
- ❌ Never log the secret key

### 2. HTTPS in Production
```properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_PASSWORD}
server.ssl.key-store-type=PKCS12
```

Always use HTTPS in production to prevent:
- Man-in-the-middle attacks
- Signature replay attacks
- Request tampering

### 3. Signature Replay Prevention

Consider implementing additional measures:
- **Timestamp validation**: Include timestamp in body, validate freshness
- **Nonce**: Use one-time tokens
- **Request ID tracking**: Prevent duplicate processing

Example with timestamp:
```json
{
  "eventType": "TOKEN_UPDATED",
  "tokenId": "123",
  "timestamp": "2024-02-09T23:30:00Z"
}
```

Then validate:
```java
long requestTime = parseTimestamp(body.getTimestamp());
long currentTime = System.currentTimeMillis();
if (Math.abs(currentTime - requestTime) > 300000) { // 5 minutes
    throw new SecurityException("Request too old");
}
```

### 4. Rate Limiting

Consider adding rate limiting to prevent abuse:

```java
@Bean
public FilterRegistrationBean<RateLimitFilter> rateLimitFilter() {
    FilterRegistrationBean<RateLimitFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new RateLimitFilter());
    registrationBean.addUrlPatterns("/api/v1/webhooks/*");
    return registrationBean;
}
```

## Testing

### Unit Tests

Run signature validation tests:
```bash
mvn test -Dtest=SignatureValidatorTest
```

### Integration Tests

Run filter integration tests:
```bash
mvn test -Dtest=WebhookSignatureValidationFilterTest
```

### Manual Testing

1. Start the application:
```bash
mvn spring-boot:run
```

2. Generate a test signature:
```bash
SECRET="YOUR_WEBHOOK_SECRET_KEY"
BODY='{"eventType":"TOKEN_UPDATED","eventTimestamp":"2024-02-09T23:30:00Z","tokenInformation":{"tokenId":"token-123"}}'
SIGNATURE=$(echo -n "$BODY" | openssl dgst -sha256 -hmac "$SECRET" -binary | base64)
echo "Signature: $SIGNATURE"
```

3. Make a test request:
```bash
curl -v -X POST http://localhost:8080/api/v1/webhooks/token-lifecycle \
  -H "Content-Type: application/json" \
  -H "v-c-signature: $SIGNATURE" \
  -d "$BODY"
```

4. Test with invalid signature:
```bash
curl -v -X POST http://localhost:8080/api/v1/webhooks/token-lifecycle \
  -H "Content-Type: application/json" \
  -H "v-c-signature: invalid-signature" \
  -d "$BODY"
```

Expected: 401 Unauthorized

## Troubleshooting

### Issue: "Invalid signature" error with correct secret

**Possible causes:**
1. **Whitespace differences**: Ensure no trailing spaces in body
2. **Character encoding**: Use UTF-8 consistently
3. **JSON formatting**: Body must match exactly (no pretty-printing)
4. **Line endings**: Use consistent line endings (LF vs CRLF)

**Debug steps:**
```java
// Log the exact body being validated
logger.debug("Request body for validation: [{}]", requestBody);
logger.debug("Request body length: {}", requestBody.length());
logger.debug("Provided signature: {}", providedSignature);
```

### Issue: "Empty request body" error

**Possible causes:**
1. Request body not being sent
2. Content-Type header missing or incorrect
3. Body consumed before reaching filter

**Solution:**
Ensure Content-Type is set to `application/json` and body is included in request.

### Issue: Performance impact of body caching

**Solution:**
The `CachedBodyHttpServletRequest` caches the body in memory. For large payloads:

```properties
# Limit request body size
server.tomcat.max-http-post-size=1MB
```

## Monitoring and Logging

### Key Metrics to Track
- Signature validation success rate
- Signature validation failure rate
- Average validation time
- Invalid signature attempts (potential attacks)

### Logging Configuration

```properties
# Enable debug logging for signature validation
logging.level.com.example.cybersource.filter=DEBUG
logging.level.com.example.cybersource.util=DEBUG
```

### Example Log Output

Success:
```
INFO  WebhookSignatureValidationFilter - Webhook signature validated successfully
INFO  TokenLifecycleController - Received token lifecycle update webhook for event type: TOKEN_UPDATED
```

Failure:
```
ERROR WebhookSignatureValidationFilter - Invalid signature for webhook request
WARN  SignatureValidator - Signature validation failed: provided=abc123..., expected=xyz789...
```

## Production Checklist

- [ ] Secret key configured via environment variable
- [ ] HTTPS enabled
- [ ] Rate limiting configured
- [ ] Monitoring and alerting set up
- [ ] Log aggregation configured
- [ ] Secret rotation procedure documented
- [ ] Incident response plan prepared
- [ ] Load testing completed
- [ ] Security audit completed

## Support

For issues or questions:
1. Check the troubleshooting section above
2. Review application logs
3. Verify configuration
4. Contact the development team

