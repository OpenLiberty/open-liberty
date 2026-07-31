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
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jose4j.jwk.PublicJsonWebKey;
import org.jose4j.jws.JsonWebSignature;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ApacheJsonUtils;
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
    private static final String SIGNED_JWK_DISABLED_PATH = "/myApp/withSigningJwkDisabled";
    private static final String SIGNED_JWK_INVALID_PATH = "/myApp/withInvalidJwtBuilder";

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        Log.info(thisClass, methodName, "Starting server: " + RSServerName);
        testSettings = new TestSettings();
        genericTestServer = commonSetUp(RSServerName, "server.xml", Constants.GENERIC_SERVER, null, Constants.DO_NOT_USE_DERBY, null, null, null);
        genericTestServer.addIgnoredServerException("CWWKG0033W"); // reference not found in configuration
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
     * signed_metadata (a compact JWS).
     * Decodes the JWT payload and validates resource and iss claims.
     */
    @Test
    public void testMetadataContainsSignedMetadataJwt() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_PATH);

        // Checks signature and consistency with unsigned metadata
        verifySignedMetadata(json, "metadataJwtBuilder");

        // issuer must be set to the value used in the config
        JSONObject signedPayload = decodeJwtPayload((String) json.get("signed_metadata"));
        String issuer = (String) signedPayload.get("iss");
        assertNotNull("Signed payload did not contain issuer claim", issuer);
        assertEquals("Signed payload 'iss' claim not correct", "testIssuer", issuer);
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

        // Checks signature and consistency with unsigned metadata
        verifySignedMetadata(json, "metadataJwtBuilderRs512Jwk");

        String signedMetadata = (String) json.get("signed_metadata");

        // Verify the JWT header declares RS512
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("JWT header 'alg' must be RS512", "RS512", header.get("alg"));
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

        // Checks signature and consistency with unsigned metadata
        verifySignedMetadata(json, "metadataJwtBuilderEs256Jwk");

        String signedMetadata = (String) json.get("signed_metadata");

        // Verify the JWT header declares ES256
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("JWT header 'alg' must be ES256", "ES256", header.get("alg"));
    }

    @Test
    public void testSignedMetadataWithJwkDisabled() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_JWK_DISABLED_PATH);

        // Checks signature and consistency with unsigned metadata
        // With jwkEnabled = false, we still get the jwkBuilder and the key is still published
        // at the JWK endpoint.
        verifySignedMetadata(json, "metadataJwtBuilderJwkDisabled");
    }

    /**
     * Test that when no {@code jwtBuilderRef} is configured, the metadata response does
     * not include {@code signed_metadata}
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
    }

    @Test
    public void testNoSigningFieldsWhenJwtBuilderRefIsInvalid() throws Exception {
        JSONObject json = getMetadataJson(serverHttpString, SIGNED_JWK_INVALID_PATH);

        assertNotNull("Response must contain 'resource' field", json.get("resource"));
        assertNotNull("Response must contain 'authorization_servers' field", json.get("authorization_servers"));
        assertNotNull("Response must contain 'scopes_supported' field", json.get("scopes_supported"));

        assertFalse("'signed_metadata' must be absent when jwtBuilderRef is invalid",
                json.containsKey("signed_metadata"));
    }

    /**
     * Validates that the metadata is signed and internally consistent.
     * <p>
     * Checks the metadata includes signed_metadata, that the signature is valid and that the claims match the metadata
     * @param metadata the protected resource metadata to verify
     * @param jwtBuilderId the jwtBuilder ID, used to find the key to verify the signature
     */
    private void verifySignedMetadata(JSONObject metadata, String jwtBuilderId) throws Exception {

        // Check signed metadata present
        String signedMetadata = (String) metadata.get("signed_metadata");
        assertNotNull("Response did not contain 'signed_metadata' field", signedMetadata);

        // Parse signed_metadata as JWT
        JSONObject payload = decodeJwtPayload(signedMetadata);

        // Check JWT includes all metadata entries as claims, except signed_metadata
        for (Entry<String, Object> entry : (Set<Entry<String, Object>>) metadata.entrySet()) {
            String key = entry.getKey();
            if (key.equals("signed_metadata")) {
                continue;
            }
            assertTrue("Signed metadata does not contain " + key, payload.containsKey(key));
            assertEquals("Signed metadata has a different value for " + key, entry.getValue(), payload.get(key));
        }

        // Check required claims
        assertTrue("Signed metadata must have an 'iss' (issuer) claim", payload.containsKey("iss"));

        // Check typ header
        JSONObject header = decodeJwtHeader(signedMetadata);
        assertEquals("Header 'typ' is incorrect", "JWT", header.get("typ"));

        // Check JWT signature
        verifySignatureAgainstJwk(signedMetadata, jwtBuilderId);
    }

    /**
     * Finds the JWK whose {@code kid} matches the JWT header, reconstructs the public key,
     * and asserts that the RS256 signature on the compact JWS is valid.
     */
    private void verifySignatureAgainstJwk(String compactJws, String jwtBuilderId) throws Exception {
        JSONObject header = decodeJwtHeader(compactJws);
        JSONObject jwkSet = getJwkFromJwtBuilderId(jwtBuilderId);

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
    private JSONObject getJwkFromJwtBuilderId(String builderId) throws Exception {

        String url = serverHttpsString + "/jwt/ibm/api/" + builderId + "/jwk";
        Log.info(thisClass, "getJwkFromUrl", "GET " + url);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(new String[] { Constants.INVOKE_JWK_ENDPOINT });
        WebConversation wc = new WebConversation();
        WebResponse response = genericInvokeEndpoint(_testName, wc, null, url, Constants.GETMETHOD, Constants.INVOKE_JWK_ENDPOINT, null, null, expectations);

        return JSONObject.parse(response.getText());
    }

    /**
     * Decodes the header part of a JWT and returns it as a JSONObject
     */
    private static JSONObject decodeJwtHeader(String compactJws) throws Exception {
        return decodeJwtPart(compactJws, 0);
    }

    /**
     * Decodes the payload part of a JWT and returns it as a JSONObject
     */
    private static JSONObject decodeJwtPayload(String compactJws) throws Exception {
        return decodeJwtPart(compactJws, 1);
    }

    /**
     * Decodes a JWT part and returns it as a JSONObject.
     */
    private static JSONObject decodeJwtPart(String compactJws, int part) throws Exception {
        String[] parts = compactJws.split("\\.");
        assertEquals("JWT has wrong number of parts", 3, parts.length);
        String segmentB64 = parts[part];
        String segment = ApacheJsonUtils.fromBase64StringToJsonString(segmentB64);
        return JSONObject.parse(segment);
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
