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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * FAT tests for OAuth 2.0 Protected Resource Metadata endpoint (RFC 8707).
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

    @Server("com.ibm.ws.security.oauth.oidc_fat.common.metadataServer")
    public static LibertyServer testServer;

    private static String serverHttpString;

    private static final String PROTECTED_RESOURCE_PATH = "/myApp/protected";
    private static final String PROTECTED_RESOURCE_SUBPATH = "/myApp/protected/subPath";

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        Log.info(thisClass, methodName, "Starting server: " + testServer.getServerName());

        testServer.startServer();

        serverHttpString = "http://" + testServer.getHostname() + ":" + testServer.getHttpDefaultPort();

        Log.info(thisClass, methodName, "Server started successfully at: " + serverHttpString);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testServer != null && testServer.isStarted()) {
            testServer.stopServer();
        }
    }

    /**
     * Test that the metadata endpoint is accessible and returns valid JSON
     * with the expected structure (resource and authorization_servers fields).
     */
    @Test
    public void testMetadataEndpointIsAccessible() throws Exception {
        JSONObject json = getMetadataJson(PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_PATH, json.get("resource"));
        assertNotNull("Response did not contain 'authorization_servers' field", json.get("authorization_servers"));
        assertTrue("'authorization_servers' should be a non-empty array",
                ((JSONArray) json.get("authorization_servers")).size() > 0);
    }

    /**
     * Test that the metadata response contains the correct resource URL
     * as an absolute URL including protocol, host, port, and path.
     */
    @Test
    public void testMetadataContainsCorrectResourceUrl() throws Exception {
        JSONObject json = getMetadataJson(PROTECTED_RESOURCE_PATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_PATH, json.get("resource"));
    }

    /**
     * Test that the metadata response contains the expected authorization
     * server identifier in the authorization_servers array.
     */
    @Test
    public void testMetadataContainsAuthorizationServerIdentifier() throws Exception {
        JSONObject json = getMetadataJson(PROTECTED_RESOURCE_PATH);
        JSONArray authorizationServers = (JSONArray) json.get("authorization_servers");
        String expectedIssuer = buildExpectedIssuer();

        assertNotNull("Response did not contain 'authorization_servers' field", authorizationServers);
        assertTrue("Expected 'authorization_servers' to contain '" + expectedIssuer + "' but was: " + authorizationServers,
                authorizationServers.contains(expectedIssuer));
    }

    /**
     * Test that requesting metadata for an unknown/unconfigured resource
     * path returns HTTP 404 Not Found.
     */
    @Test
    public void testUnknownResourceReturns404() throws Exception {
        String metadataUrl = buildMetadataUrl("/nonexistent/path");
        WebResponse response = getResponseAllowingErrorStatus(metadataUrl);

        assertEquals("Expected unknown protected resource metadata endpoint to return 404", 404,
                response.getResponseCode());
    }

    /**
     * Test that requesting the base metadata endpoint without a resource
     * path returns HTTP 404 Not Found.
     */
    @Test
    public void testBasePathWithoutResourceReturns404() throws Exception {
        String metadataUrl = serverHttpString + PROTECTED_RESOURCE_METADATA_PATH;
        WebResponse response = getResponseAllowingErrorStatus(metadataUrl);

        assertEquals("Expected base protected resource metadata endpoint to return 404", 404,
                response.getResponseCode());
    }

    /**
     * Test that a sub-path URL that is contained within a "contains" auth filter
     * pattern also returns metadata.
     */
    @Test
    public void testSubPathUnderContainsFilterReturnsMetadata() throws Exception {
        JSONObject json = getMetadataJson(PROTECTED_RESOURCE_SUBPATH);

        assertEquals("Unexpected value for 'resource' field",
                serverHttpString + PROTECTED_RESOURCE_SUBPATH, json.get("resource"));
    }

    /**
     * Test that the metadata endpoint returns the correct Content-Type
     * header (application/json).
     */
    @Test
    public void testMetadataReturnsJsonContentType() throws Exception {
        String metadataUrl = buildMetadataUrl(PROTECTED_RESOURCE_PATH);
        WebResponse response = getResponse(metadataUrl);

        assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());
        assertTrue("Expected Content-Type to contain application/json but was: " + response.getContentType(),
                response.getContentType().contains("application/json"));
    }

    /**
     * GETs the metadata endpoint for the given protected resource path, asserts HTTP 200,
     * and returns the parsed JSON response body.
     */
    private static JSONObject getMetadataJson(String protectedResourcePath) throws Exception {
        String metadataUrl = buildMetadataUrl(protectedResourcePath);
        Log.info(thisClass, "getMetadataJson", "GET " + metadataUrl);
        WebResponse response = getResponse(metadataUrl);
        assertEquals("Expected metadata endpoint to return 200", 200, response.getResponseCode());
        return JSONObject.parse(response.getText());
    }

    /**
     * Builds the full metadata endpoint URL for a given protected resource path.
     */
    private static String buildMetadataUrl(String protectedResourcePath) {
        String normalized;
        if (protectedResourcePath == null || protectedResourcePath.isEmpty() || "/".equals(protectedResourcePath)) {
            normalized = "/";
        } else if (protectedResourcePath.startsWith("/")) {
            normalized = protectedResourcePath;
        } else {
            normalized = "/" + protectedResourcePath;
        }
        return serverHttpString + PROTECTED_RESOURCE_METADATA_PATH + normalized;
    }

    /**
     * Builds the expected issuer URL from the running server's hostname and port.
     */
    private static String buildExpectedIssuer() {
        return "http://" + testServer.getHostname() + ":" + testServer.getHttpDefaultPort()
                + "/oidc/endpoint/OidcConfigSample";
    }

    private static WebResponse getResponse(String url) throws Exception {
        WebConversation wc = new WebConversation();
        WebRequest request = new GetMethodWebRequest(url);
        return wc.getResponse(request);
    }

    private static WebResponse getResponseAllowingErrorStatus(String url) throws Exception {
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new GetMethodWebRequest(url);
        return wc.getResponse(request);
    }
}
