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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import java.util.List;

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
