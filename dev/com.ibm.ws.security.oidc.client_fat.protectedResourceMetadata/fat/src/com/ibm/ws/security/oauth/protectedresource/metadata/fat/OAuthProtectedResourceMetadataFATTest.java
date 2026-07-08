/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.oauth.protectedresource.metadata.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.PublicKey;
import java.util.Base64;
import java.util.List;

import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;

/**
 * FAT tests for OAuth 2.0 Protected Resource Metadata endpoint (RFC 9728).
 *
 * <p>
 * These tests verify that the metadata endpoint correctly serves metadata
 * for protected resources, including the resource URL and authorization
 * server identifiers.
 * </p>
 */
@RunWith(FATRunner.class)
public class OAuthProtectedResourceMetadataFATTest extends CommonTest {

    private static final Class<?> thisClass = OAuthProtectedResourceMetadataFATTest.class;

    private static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";
    private static final String RSServerName = "com.ibm.ws.security.oauth.oidc_fat.common.metadataServer";

    private static String serverHttpString;
    private static String serverHttpsString;

    private static final String PROTECTED_RESOURCE_PATH = "/myApp/protectedExact";
    private static final String PROTECTED_RESOURCE_SUBPATH = "/myApp/protected/subPath";
    private static final String METADATA_DISABLED_PATH = "/myApp/metadataDisabled";
    private static final String WITH_SCOPES_PATH = "/myApp/withScopes";
    private static final String SIGNED_PATH = "/myApp/withSigning";
    private static final String SIGNED_RS512_JWK_PATH = "/myApp/withSigningRs512Jwk";
    private static final String SIGNED_ES256_JWK_PATH = "/myApp/withSigningEs256Jwk";
    private static final String SIGNED_RS384_JWK_ROTATION_PATH = "/myApp/withSigningRs384JwkRotation";
    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        Log.info(thisClass, methodName, "Starting server: " + RSServerName);
        testSettings = new TestSettings();
        genericTestServer = commonSetUp(RSServerName, "server.xml", Constants.GENERIC_SERVER, null, Constants.DO_NOT_USE_DERBY, null, null, null);
        serverHttpString = genericTestServer.getHttpString();
        serverHttpsString = genericTestServer.getHttpsString();
        Log.info(thisClass, methodName, "Server started successfully. HTTP: " + serverHttpString + "  HTTPS: " + serverHttpsString);
    }

    // -------------------------------------------------------------------------
    // HTTP tests
    // -------------------------------------------------------------------------

    /**
     * Test that the metadata endpoint is accessible over HTTP and returns valid JSON
     * with the expected structure (resource and authorization_servers fields).
     */
    @Test
    public void testMetadataEndpointIsAccessible() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_PATH, json.get("resource"));
        assertNotNull("Response did not contain 'authorization_servers' field", json.get("authorization_servers"));
        assertTrue("'authorization_servers' should be a non-empty array",
                ((JSONArray) json.get("authorization_servers")).size() > 0);
    }

    /**
     * Test that the HTTP metadata response contains the correct resource URL
     * as an absolute URL including protocol, host, port, and path.
     */
    @Test
    public void testMetadataContainsCorrectResourceUrl() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_PATH, json.get("resource"));
    }

    /**
     * Test that the HTTP metadata response contains the expected authorization
     * server identifier in the authorization_servers array.
     */
    @Test
    public void testMetadataContainsAuthorizationServerIdentifier() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, PROTECTED_RESOURCE_PATH);
        JSONArray authorizationServers = (JSONArray) json.get("authorization_servers");
        String expectedIssuer = buildExpectedHttpIssuer();

        assertNotNull("Response did not contain 'authorization_servers' field", authorizationServers);
        assertEquals("Expected exactly one entry in 'authorization_servers'", 1, authorizationServers.size());
        assertTrue("Expected 'authorization_servers' to contain '" + expectedIssuer + "' but was: " + authorizationServers,
                authorizationServers.contains(expectedIssuer));
    }

    /**
     * Test that requesting metadata for an unknown/unconfigured resource
     * path over HTTP returns HTTP 404 Not Found.
     */
    @Test
    public void testUnknownResourceReturns404() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpString, "/nonexistent/path");
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that requesting the base metadata endpoint without a resource
     * path over HTTP returns HTTP 404 Not Found.
     */
    @Test
    public void testBasePathWithoutResourceReturns404() throws Exception {
        String metadataUrl = serverHttpString + PROTECTED_RESOURCE_METADATA_PATH;
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that a sub-path URL that is contained within a "contains" auth filter
     * pattern also returns metadata over HTTP.
     */
    @Test
    public void testSubPathUnderContainsFilterReturnsMetadata() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, PROTECTED_RESOURCE_SUBPATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_SUBPATH, json.get("resource"));
    }

    /**
     * Test that the HTTP metadata endpoint returns the correct Content-Type
     * header (application/json).
     */
    @Test
    public void testMetadataReturnsJsonContentType() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpString, PROTECTED_RESOURCE_PATH);
        WebResponse response = getSuccessResponse(metadataUrl);

        assertTrue("Expected Content-Type to contain application/json but was: " + response.getContentType(),
                response.getContentType().contains("application/json"));
    }

    /**
     * Test that requesting metadata for a resource whose OIDC client config does not have
     * serveProtectedResourceMetadata enabled returns 404, even though the auth filter matches.
     */
    @Test
    public void testMetadataDisabledReturns404() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpString, METADATA_DISABLED_PATH);
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that the metadata response includes scopes_supported matching the advertisedScopes
     * configured in the protectedResourceMetadata sub-element.
     */
    @Test
    public void testMetadataContainsConfiguredScopes() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, WITH_SCOPES_PATH);

        JSONArray scopesSupported = (JSONArray) json.get("scopes_supported");
        assertNotNull("Response did not contain 'scopes_supported' field", scopesSupported);
        assertTrue("Expected 'scopes_supported' to contain 'openid'", scopesSupported.contains("openid"));
        assertTrue("Expected 'scopes_supported' to contain 'profile'", scopesSupported.contains("profile"));
        assertTrue("Expected 'scopes_supported' to contain 'email'", scopesSupported.contains("email"));
        assertEquals("Expected exactly 3 scopes in 'scopes_supported'", 3, scopesSupported.size());
    }

    // -------------------------------------------------------------------------
    // HTTPS tests
    // -------------------------------------------------------------------------

    /**
     * Test that the metadata endpoint is accessible over HTTPS and returns valid JSON
     * with the expected structure (resource and authorization_servers fields).
     */
    @Test
    public void testMetadataEndpointIsAccessibleHttps() throws Exception {
        JSONObject json = getMetadataJson(serverHttpsString, PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpsString + PROTECTED_RESOURCE_PATH, json.get("resource"));
        assertNotNull("Response did not contain 'authorization_servers' field", json.get("authorization_servers"));
        assertTrue("'authorization_servers' should be a non-empty array",
                ((JSONArray) json.get("authorization_servers")).size() > 0);
    }

    /**
     * Test that the HTTPS metadata response contains the correct resource URL
     * as an absolute URL including protocol, host, port, and path.
     */
    @Test
    public void testMetadataContainsCorrectResourceUrlHttps() throws Exception {
        JSONObject json = getMetadataJson(serverHttpsString, PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpsString + PROTECTED_RESOURCE_PATH, json.get("resource"));
    }

    /**
     * Test that the HTTPS metadata response contains the expected authorization
     * server identifier in the authorization_servers array.
     */
    @Test
    public void testMetadataContainsAuthorizationServerIdentifierHttps() throws Exception {
        JSONObject json = getMetadataJson(serverHttpsString, PROTECTED_RESOURCE_PATH);
        JSONArray authorizationServers = (JSONArray) json.get("authorization_servers");
        String expectedIssuer = buildExpectedHttpsIssuer();

        assertNotNull("Response did not contain 'authorization_servers' field", authorizationServers);
        assertEquals("Expected exactly one entry in 'authorization_servers'", 1, authorizationServers.size());
        assertTrue("Expected 'authorization_servers' to contain '" + expectedIssuer + "' but was: " + authorizationServers,
                authorizationServers.contains(expectedIssuer));
    }

    /**
     * Test that requesting metadata for an unknown/unconfigured resource
     * path over HTTPS returns HTTP 404 Not Found.
     */
    @Test
    public void testUnknownResourceReturns404Https() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpsString, "/nonexistent/path");
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that requesting the base metadata endpoint without a resource
     * path over HTTPS returns HTTP 404 Not Found.
     */
    @Test
    public void testBasePathWithoutResourceReturns404Https() throws Exception {
        String metadataUrl = serverHttpsString + PROTECTED_RESOURCE_METADATA_PATH;
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that a sub-path URL that is contained within a "contains" auth filter
     * pattern also returns metadata over HTTPS.
     */
    @Test
    public void testSubPathUnderContainsFilterReturnsMetadataHttps() throws Exception {
        JSONObject json = getMetadataJson(serverHttpsString, PROTECTED_RESOURCE_SUBPATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpsString + PROTECTED_RESOURCE_SUBPATH, json.get("resource"));
    }

    /**
     * Test that the HTTPS metadata endpoint returns the correct Content-Type
     * header (application/json).
     */
    @Test
    public void testMetadataReturnsJsonContentTypeHttps() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpsString, PROTECTED_RESOURCE_PATH);
        WebResponse response = getSuccessResponse(metadataUrl);

        assertTrue("Expected Content-Type to contain application/json but was: " + response.getContentType(),
                response.getContentType().contains("application/json"));
    }

    /**
     * Test that requesting metadata over HTTPS for a resource whose OIDC client config does not
     * have serveProtectedResourceMetadata enabled returns 404, even though the auth filter matches.
     */
    @Test
    public void testMetadataDisabledReturns404Https() throws Exception {
        String metadataUrl = buildMetadataUrl(serverHttpsString, METADATA_DISABLED_PATH);
        getErrorResponse(metadataUrl);
    }

    /**
     * Test that the HTTPS metadata response includes scopes_supported matching the advertisedScopes
     * configured in the protectedResourceMetadata sub-element.
     */
    @Test
    public void testMetadataContainsConfiguredScopesHttps() throws Exception {
        JSONObject json = getMetadataJson(serverHttpsString, WITH_SCOPES_PATH);

        JSONArray scopesSupported = (JSONArray) json.get("scopes_supported");
        assertNotNull("Response did not contain 'scopes_supported' field", scopesSupported);
        assertTrue("Expected 'scopes_supported' to contain 'openid'", scopesSupported.contains("openid"));
        assertTrue("Expected 'scopes_supported' to contain 'profile'", scopesSupported.contains("profile"));
        assertTrue("Expected 'scopes_supported' to contain 'email'", scopesSupported.contains("email"));
        assertEquals("Expected exactly 3 scopes in 'scopes_supported'", 3, scopesSupported.size());
    }

    /**
     * Test that when jwtBuilderRef is configured, the metadata response includes
     * signed_metadata (a compact JWS), jwks_uri, and bearer_methods_supported.
     * Decodes the JWT payload and validates resource, iss, and jwks_uri claims.
     */
    @Test
    public void testMetadataContainsSignedMetadataJwt() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_PATH);

        // Top-level jwks_uri must be present and point to the JWT builder's JWK endpoint
        String topLevelJwksUri = (String) json.get("jwks_uri");
        assertNotNull("Response did not contain top-level 'jwks_uri' field", topLevelJwksUri);
        assertTrue("'jwks_uri' should contain '/jwt/ibm/api/metadataJwtBuilder/jwk' but was: " + topLevelJwksUri,
                topLevelJwksUri.contains("/jwt/ibm/api/metadataJwtBuilder/jwk"));

        // Top-level bearer_methods_supported must be ["header"]
        JSONArray bearerMethods = (JSONArray) json.get("bearer_methods_supported");
        assertNotNull("Response did not contain 'bearer_methods_supported' field", bearerMethods);
        assertTrue("'bearer_methods_supported' should contain 'header'", bearerMethods.contains("header"));

        // signed_metadata must be present and be a 3-part compact JWS
        String signedMetadata = (String) json.get("signed_metadata");
        assertNotNull("Response did not contain 'signed_metadata' field", signedMetadata);

        String[] parts = signedMetadata.split("\\.");
        assertEquals("signed_metadata should be a 3-part compact JWS (header.payload.signature)", 3, parts.length);

        // Decode the JWT payload (base64url, possibly unpadded)
        String payloadB64 = parts[1];
        int padNeeded = (4 - payloadB64.length() % 4) % 4;
        for (int i = 0; i < padNeeded; i++) {
            payloadB64 += "=";
        }
        byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadB64);
        JSONObject payload = JSONObject.parse(new String(payloadBytes, "UTF-8"));

        String expectedResource = serverHttpString + SIGNED_PATH;

        // JWT resource claim must equal the protected resource URL
        assertEquals("JWT payload 'resource' claim mismatch", expectedResource, payload.get("resource"));

        // RFC 9728 §4: iss MUST equal resource
        assertEquals("JWT payload 'iss' claim must equal 'resource' (RFC 9728 §4)",
                payload.get("resource"), payload.get("iss"));

        // JWT jwks_uri claim must match the top-level jwks_uri
        assertEquals("JWT payload 'jwks_uri' claim should match top-level 'jwks_uri'",
                topLevelJwksUri, payload.get("jwks_uri"));
    }

    /**
     * Test that a jwtBuilder with RS512 / 4096-bit ephemeral JWK produces valid
     * signed_metadata and a matching, verifiable JWK Set entry.
     * <p>
     * Confirms: {@code alg=RS512} in the JWT header, {@code kty=RSA} with a 4096-bit
     * modulus in the JWK, and that the signature verifies against the published key.
     * </p>
     */
    @Test
    public void testSignedMetadataRs512JwkSignatureVerifies() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_RS512_JWK_PATH);

        String jwksUri = (String) json.get("jwks_uri");
        assertNotNull("Response must contain 'jwks_uri'", jwksUri);
        assertTrue("jwks_uri must reference 'metadataJwtBuilderRs512Jwk'",
                jwksUri.contains("/jwt/ibm/api/metadataJwtBuilderRs512Jwk/jwk"));

        String signedMetadata = (String) json.get("signed_metadata");
        assertNotNull("Response must contain 'signed_metadata'", signedMetadata);

        // Verify the JWT header declares RS512
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("JWT header 'alg' must be RS512", "RS512", header.get("alg"));

        // Verify the JWK Set and the cryptographic signature
        JSONObject jwkSet = getJsonFromUrl(jwksUri);
        JSONArray keys = (JSONArray) jwkSet.get("keys");
        assertNotNull("JWK Set must contain a 'keys' array", keys);
        JSONObject key = (JSONObject) keys.iterator().next();
        assertEquals("JWK key type must be RSA", "RSA", key.get("kty"));

        // 4096-bit RSA modulus encodes to 512 bytes → 683 base64url chars (unpadded)
        String n = (String) key.get("n");
        assertNotNull("RSA JWK must contain modulus 'n'", n);
        assertTrue("4096-bit RSA modulus must be at least 600 base64url chars, was: " + n.length(),
                n.length() >= 600);

        verifySignatureAgainstJwk(signedMetadata, jwkSet);
    }

    /**
     * Test that a jwtBuilder with ES256 (ECDSA / P-256) produces valid
     * signed_metadata and a matching, verifiable JWK Set entry.
     * <p>
     * Confirms: {@code alg=ES256} in the JWT header, {@code kty=EC} and
     * {@code crv=P-256} in the JWK — proving the EC code path is exercised end-to-end.
     * </p>
     */
    @Test
    public void testSignedMetadataEs256JwkSignatureVerifies() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_ES256_JWK_PATH);

        String jwksUri = (String) json.get("jwks_uri");
        assertNotNull("Response must contain 'jwks_uri'", jwksUri);
        assertTrue("jwks_uri must reference 'metadataJwtBuilderEs256Jwk'",
                jwksUri.contains("/jwt/ibm/api/metadataJwtBuilderEs256Jwk/jwk"));

        String signedMetadata = (String) json.get("signed_metadata");
        assertNotNull("Response must contain 'signed_metadata'", signedMetadata);

        // Verify the JWT header declares ES256
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("JWT header 'alg' must be ES256", "ES256", header.get("alg"));

        // Verify the JWK entry is EC / P-256
        JSONObject jwkSet = getJsonFromUrl(jwksUri);
        JSONArray keys = (JSONArray) jwkSet.get("keys");
        assertNotNull("JWK Set must contain a 'keys' array", keys);
        JSONObject key = (JSONObject) keys.iterator().next();
        assertEquals("JWK key type must be EC", "EC", key.get("kty"));
        assertEquals("JWK curve must be P-256", "P-256", key.get("crv"));

        verifySignatureAgainstJwk(signedMetadata, jwkSet);
    }

    /**
     * Test that a jwtBuilder with RS384 / 2048-bit key and a 60-minute JWK rotation
     * window produces valid signed_metadata and a matching, verifiable JWK Set entry.
     * <p>
     * Confirms: {@code alg=RS384} in the JWT header and that the signature verifies,
     * exercising the third RSA digest variant and the short-rotation code path.
     * </p>
     */
    @Test
    public void testSignedMetadataRs384JwkRotationSignatureVerifies() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_RS384_JWK_ROTATION_PATH);

        String jwksUri = (String) json.get("jwks_uri");
        assertNotNull("Response must contain 'jwks_uri'", jwksUri);
        assertTrue("jwks_uri must reference 'metadataJwtBuilderRs384JwkRotation'",
                jwksUri.contains("/jwt/ibm/api/metadataJwtBuilderRs384JwkRotation/jwk"));

        String signedMetadata = (String) json.get("signed_metadata");
        assertNotNull("Response must contain 'signed_metadata'", signedMetadata);

        // Verify the JWT header declares RS384
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("JWT header 'alg' must be RS384", "RS384", header.get("alg"));

        verifySignatureAgainstJwk(signedMetadata, getJsonFromUrl(jwksUri));
    }

    /**
     * Test that when no {@code jwtBuilderRef} is configured, the metadata response does
     * not include {@code signed_metadata}, {@code jwks_uri}, or {@code bearer_methods_supported},
     * but does include the base fields {@code resource} and {@code authorization_servers}.
     * Uses the {@code /myApp/withScopes} path whose OIDC client has
     * {@code <protectedResourceMetadata advertisedScopes="openid,profile,email"/>} but no
     * {@code jwtBuilderRef}.
     */
    @Test
    public void testNoSigningFieldsWhenJwtBuilderRefIsAbsent() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, WITH_SCOPES_PATH);

        assertNotNull("Response must contain 'resource' field", json.get("resource"));
        assertNotNull("Response must contain 'authorization_servers' field", json.get("authorization_servers"));
        assertNotNull("Response must contain 'scopes_supported' field", json.get("scopes_supported"));

        assertFalse("'signed_metadata' must be absent when jwtBuilderRef is not configured",
                json.containsKey("signed_metadata"));
        assertFalse("'jwks_uri' must be absent when jwtBuilderRef is not configured",
                json.containsKey("jwks_uri"));
        assertFalse("'bearer_methods_supported' must be absent when jwtBuilderRef is not configured",
                json.containsKey("bearer_methods_supported"));
    }

    /**
     * Test that the signed_metadata JWT signature can be cryptographically verified
     * against the public key published at the jwks_uri endpoint.
     * <p>
     * Fetches the JWK Set from the top-level {@code jwks_uri}, finds the key whose
     * {@code kid} matches the JWT header, reconstructs the public key, and confirms
     * the RS256 signature is valid.
     * </p>
     */
    @Test
    public void testSignedMetadataJwtSignatureVerifiesAgainstJwksUri() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_PATH);

        String jwksUri = (String) json.get("jwks_uri");
        assertNotNull("Response did not contain 'jwks_uri' field", jwksUri);

        String signedMetadata = (String) json.get("signed_metadata");
        assertNotNull("Response did not contain 'signed_metadata' field", signedMetadata);

        JSONObject jwkSet = getJsonFromUrl(jwksUri);
        verifySignatureAgainstJwk(signedMetadata, jwkSet);
    }

    /**
     * Finds the JWK whose {@code kid} matches the JWT header, reconstructs the public key,
     * and asserts that the RS256 signature on the compact JWS is valid.
     */
    private static void verifySignatureAgainstJwk(String compactJws, JSONObject jwkSet) throws Exception {
        JSONObject header = decodeJwtHeader(compactJws);

        String kid = (String) header.get("kid");
        assertNotNull("JWT header must contain a 'kid' claim", kid);

        JSONArray keys = (JSONArray) jwkSet.get("keys");
        assertNotNull("JWK Set must contain a 'keys' array", keys);
        assertTrue("JWK Set must contain at least one key", keys.size() > 0);

        // Find the JWK entry whose kid matches the JWT header.
        JSONObject matchedKey = null;
        for (Object entry : keys) {
            JSONObject key = (JSONObject) entry;
            if (kid.equals(key.get("kid"))) {
                matchedKey = key;
                break;
            }
        }
        assertNotNull("JWT kid '" + kid + "' must match a key in the JWK Set", matchedKey);
        Log.info(thisClass, "verifySignatureAgainstJwk",
                "Matched JWK key: kid=" + matchedKey.get("kid") + ", kty=" + matchedKey.get("kty"));

        // Reconstruct the public key from the JWK and verify the JWT signature.
        PublicJsonWebKey publicJwk = PublicJsonWebKey.Factory.newPublicJwk(matchedKey.toString());
        PublicKey publicKey = publicJwk.getPublicKey();

        JsonWebSignature jws = new JsonWebSignature();
        jws.setCompactSerialization(compactJws);
        jws.setKey(publicKey);
        assertTrue("JWT signature must be valid against the published JWK", jws.verifySignature());

        Log.info(thisClass, "verifySignatureAgainstJwk",
                "JWT signature verified successfully against kid=" + kid);
    }

    /**
     * Fetches and parses a JSON object from the given URL, asserting HTTP 200.
     */
    private JSONObject getJsonFromUrl(String url) throws Exception {
        Log.info(thisClass, "getJsonFromUrl", "GET " + url);
        WebResponse response = getSuccessResponse(url);
        assertEquals("Expected 200 from " + url, 200, response.getResponseCode());
        return JSONObject.parse(response.getText());
    }

    /**
     * Decodes the JWT header (first part of the compact JWS) and returns it as a JSONObject.
     */
    private static JSONObject decodeJwtHeader(String compactJws) throws Exception {
        String headerB64 = compactJws.split("\\.")[0];
        int padNeeded = (4 - headerB64.length() % 4) % 4;
        for (int i = 0; i < padNeeded; i++) {
            headerB64 += "=";
        }
        byte[] headerBytes = Base64.getUrlDecoder().decode(headerB64);
        return JSONObject.parse(new String(headerBytes, "UTF-8"));
    }

    /**
     * GETs the metadata endpoint for the given base URL and protected resource path,
     * asserts HTTP 200, and returns the parsed JSON response body.
     */
    private JSONObject getMetadataJson(String baseUrl, String protectedResourcePath) throws Exception {
        String metadataUrl = buildMetadataUrl(baseUrl, protectedResourcePath);
        Log.info(thisClass, "getMetadataJson", "GET " + metadataUrl);

        WebResponse response = getSuccessResponse(metadataUrl);

        return JSONObject.parse(response.getText());
    }

    /**
     * Builds the full metadata endpoint URL for a given base URL and protected resource path.
     */
    private static String buildMetadataUrl(String baseUrl, String protectedResourcePath) {
        String normalized;
        if (protectedResourcePath == null || protectedResourcePath.isEmpty() || "/".equals(protectedResourcePath)) {
            normalized = "/";
        } else if (protectedResourcePath.startsWith("/")) {
            normalized = protectedResourcePath;
        } else {
            normalized = "/" + protectedResourcePath;
        }
        return baseUrl + PROTECTED_RESOURCE_METADATA_PATH + normalized;
    }

    /**
     * Builds the expected HTTP issuer URL from the running server's hostname and port.
     */
    private static String buildExpectedHttpIssuer() {
        return "http://localhost:" + genericTestServer.getServer().getHttpDefaultPort()
                + "/oidc/endpoint/OidcConfigSample";
    }

    /**
     * Builds the expected HTTPS issuer URL from the running server's secure port.
     */
    private static String buildExpectedHttpsIssuer() {
        return "https://localhost:" + genericTestServer.getServer().getHttpDefaultSecurePort()
                + "/oidc/endpoint/OidcConfigSampleHttps";
    }

    private WebResponse getSuccessResponse(String url) throws Exception {
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResourceMetadataUrl(url);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT,
                "Did not find protected resource metadata in the response.", "\"resource\":");

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getProtectedResourceMetadataUrl(),
                Constants.GETMETHOD, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT, null, null, expectations);

        return response;
    }

    private WebResponse getErrorResponse(String url) throws Exception {
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setProtectedResourceMetadataUrl(url);

        List<validationData> expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT, Constants.NOT_FOUND_STATUS);

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getProtectedResourceMetadataUrl(),
                Constants.GETMETHOD, Constants.INVOKE_PROTECTED_RESOURCE_METADATA_ENDPOINT, null, null, expectations);

        return response;
    }
}
