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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;

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
 *
 * <p>
 * The tests use helper methods from {@link OAuthProtectedResourceMetadataTests}
 * to perform HTTP requests and validate responses.
 * </p>
 */
@RunWith(FATRunner.class)
public class OAuthProtectedResourceMetadataFATTest extends OAuthProtectedResourceMetadataTests {

    private static final Class<?> thisClass = OAuthProtectedResourceMetadataFATTest.class;

    @Server("com.ibm.ws.security.oauth.oidc_fat.common.metadataServer")
    public static LibertyServer testServer;

    private static TestSettings testSettings;
    private static String serverHttpsString;

    private static final String PROTECTED_RESOURCE_PATH = "/formlogin/SimpleServlet";
    private static final String EXPECTED_ISSUER = "https://localhost:${bvt.prop.security_1_HTTP_default.secure}/oidc/endpoint/OidcConfigSample";

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        Log.info(thisClass, methodName, "Starting server: " + testServer.getServerName());

        testServer.startServer();

        // Wait for the OIDC client configuration to be processed
        // CWWKS1700I: OpenID Connect client {0} configuration successfully processed.
        testServer.waitForStringInLog("CWWKS1700I.*OidcConfigSample");

        // Construct HTTPS URL manually
        String hostname = "localhost";
        int httpsPort = testServer.getHttpDefaultSecurePort();
        serverHttpsString = "https://" + hostname + ":" + httpsPort;

        testSettings = new TestSettings();
        testSettings.setTestURL(serverHttpsString);

        Log.info(thisClass, methodName, "Server started successfully at: " + serverHttpsString);
    }

    /**
     * Test that the metadata endpoint is accessible and returns valid JSON
     * with the expected structure (resource and authorization_servers fields).
     */
    @Test
    public void testMetadataEndpointIsAccessible() throws Exception {
        testMetadataEndpointAccessible(
            serverHttpsString,
            PROTECTED_RESOURCE_PATH,
            testSettings
        );
    }

    /**
     * Test that the metadata response contains the correct resource URL
     * as an absolute URL including protocol, host, port, and path.
     */
    @Test
    public void testMetadataContainsCorrectResourceUrl() throws Exception {
        testMetadataContainsResource(
            serverHttpsString,
            PROTECTED_RESOURCE_PATH,
            testSettings
        );
    }

    /**
     * Test that the metadata response contains the expected authorization
     * server identifier in the authorization_servers array.
     */
    @Test
    public void testMetadataContainsAuthorizationServerIdentifier() throws Exception {
        String expectedIssuer = EXPECTED_ISSUER.replace(
            "${bvt.prop.security_1_HTTP_default.secure}",
            String.valueOf(testServer.getHttpDefaultSecurePort())
        );

        testMetadataContainsAuthorizationServer(
            serverHttpsString,
            PROTECTED_RESOURCE_PATH,
            expectedIssuer,
            testSettings
        );
    }

    /**
     * Test that requesting metadata for an unknown/unconfigured resource
     * path returns HTTP 404 Not Found.
     */
    @Test
    public void testUnknownResourceReturns404() throws Exception {
        testMetadataEndpointReturns404ForUnknownResource(
            serverHttpsString,
            "/nonexistent/path",
            testSettings
        );
    }

    /**
     * Test that requesting the base metadata endpoint without a resource
     * path returns HTTP 404 Not Found.
     */
    @Test
    public void testBasePathWithoutResourceReturns404() throws Exception {
        testMetadataEndpointBasePathReturns404(
            serverHttpsString,
            testSettings
        );
    }

    /**
     * Test that the metadata endpoint returns the correct Content-Type
     * header (application/json).
     */
    @Test
    public void testMetadataReturnsJsonContentType() throws Exception {
        testMetadataEndpointReturnsJsonContentType(
            serverHttpsString,
            PROTECTED_RESOURCE_PATH,
            testSettings
        );
    }
}

// Made with Bob
