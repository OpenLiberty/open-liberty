# OAuth 2.0 Protected Resource Metadata FAT Tests

This FAT (Feature Acceptance Test) bundle contains tests for the OAuth 2.0 Protected Resource Metadata endpoint as defined in [RFC 8707](https://www.rfc-editor.org/rfc/rfc8707.html).

## Overview

The Protected Resource Metadata endpoint allows OAuth 2.0 protected resources to publish metadata about themselves, including:
- The resource identifier (URL)
- Associated authorization server identifiers

The endpoint follows the pattern: `/.well-known/oauth-protected-resource{resource_path}`

## Test Structure

### Test Classes

- **`OAuthProtectedResourceMetadataFATTest`**: Main FAT test class containing test methods
- **`OAuthProtectedResourceMetadataTests`**: Helper class with reusable test logic
- **`FATSuite`**: Test suite that runs all tests in this bundle

### Test Coverage

The tests verify:
1. Metadata endpoint accessibility and JSON response format
2. Correct resource URL in metadata response
3. Authorization server identifiers in metadata
4. 404 responses for unknown/unconfigured resources
5. Correct Content-Type headers (application/json)

## Server Configuration

The test server (`com.ibm.ws.security.oauth.oidc_fat.common.metadataServer`) is configured with:
- OpenID Connect Client feature for metadata endpoint support
- Form-based authentication test application
- SSL/HTTPS configuration
- Basic user registry for test authentication

## Running the Tests

To run all tests in this bundle:
```bash
./gradlew :com.ibm.ws.security.oauth.protectedresource.metadata_fat:buildandrun
```

To run a specific test:
```bash
./gradlew :com.ibm.ws.security.oauth.protectedresource.metadata_fat:test --tests OAuthProtectedResourceMetadataFATTest.testMetadataEndpointIsAccessible
```

## Dependencies

This FAT bundle depends on:
- `com.ibm.ws.security.oauth.oidc_fat.common` - Common test utilities and test applications
- `com.ibm.ws.security.fat.common` - General security FAT utilities
- HTTPUnit and HTMLUnit for HTTP testing

## Related Documentation

- [OAuth_Protected_Resource_Metadata_Documentation.md](../../OAuth_Protected_Resource_Metadata_Documentation.md) - Detailed implementation documentation
- [RFC 8707](https://www.rfc-editor.org/rfc/rfc8707.html) - OAuth 2.0 Resource Indicators specification