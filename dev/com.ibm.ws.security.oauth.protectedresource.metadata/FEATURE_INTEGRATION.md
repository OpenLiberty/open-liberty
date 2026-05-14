# Feature Integration for OAuth Protected Resource Metadata

## Overview
This bundle provides the OAuth 2.0 Protected Resource Metadata endpoint as defined in RFC 9728.

## Feature Integration Requirement

This bundle **MUST** be added to the `openidConnectClient` feature so that enabling the feature automatically makes the `/.well-known/oauth-protected-resource` endpoint available.

## Integration Steps

1. Locate the `openidConnectClient` feature definition in the Liberty feature repository
2. Add this bundle to the feature's bundle list:
   ```
   com.ibm.ws.security.oauth.protectedresource.metadata
   ```

3. The bundle should be included in all versions of the openidConnectClient feature (e.g., openidConnectClient-1.0)

## Bundle Details

- **Bundle Symbolic Name**: `com.ibm.ws.security.oauth.protectedresource.metadata`
- **Web Context Path**: `/.well-known/oauth-protected-resource`
- **Dependencies**:
  - `com.ibm.ws.security.authentication.filter` (for IAuthenticationFilterInternal)
  - `com.ibm.ws.security.openidconnect.clients.common` (for OidcClientConfig, ConvergedClientConfig)
  - Standard OSGi and servlet APIs

## Verification

After integration, verify that:
1. Enabling `<feature>openidConnectClient-1.0</feature>` in server.xml makes the endpoint available
2. The endpoint responds at `https://server:port/.well-known/oauth-protected-resource/<path>`
3. The endpoint returns RFC 9728 compliant JSON metadata for protected resources

## Example Configuration

```xml
<server>
    <featureManager>
        <feature>openidConnectClient-1.0</feature>
    </featureManager>
    
    <authFilter id="myAppFilter">
        <requestUrl urlPattern="/myApp/protected" matchType="contains"/>
    </authFilter>
    
    <openidConnectClient 
        id="myOidcClient"
        authFilterRef="myAppFilter"
        issuerIdentifier="https://auth.example.com/oidc"
        clientId="myClientId"
        clientSecret="myClientSecret"/>
</server>
```

With this configuration, the endpoint will be available at:
```
GET /.well-known/oauth-protected-resource/myApp/protected
```

And will return:
```json
{
  "resource": "https://localhost:9443/myApp/protected",
  "authorization_servers": [
    "https://auth.example.com/oidc"
  ]
}