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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

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
 * FAT tests verifying that the OAuth 2.0 Protected Resource Metadata endpoint
 * is <strong>not</strong> served when the server is not in beta mode.
 * <p>
 * Even when {@code protectedResourceMetadata} is fully configured in the OIDC client,
 * the endpoint must return HTTP 404 when {@code com.ibm.ws.beta.edition=false}.
 * </p>
 */
@RunWith(FATRunner.class)
public class OAuthProtectedResourceMetadataNonBetaFATTest extends CommonTest {

    private static final Class<?> thisClass = OAuthProtectedResourceMetadataNonBetaFATTest.class;

    private static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";

    @Server("com.ibm.ws.security.oauth.oidc_fat.common.metadataServerNonBeta")
    public static LibertyServer testServer;

    private static String serverHttpString;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";
        Log.info(thisClass, methodName, "Starting non-beta server: " + testServer.getServerName());

        testServer.startServer();

        serverHttpString = "http://" + testServer.getHostname() + ":" + testServer.getHttpDefaultPort();

        Log.info(thisClass, methodName, "Non-beta server started at: " + serverHttpString);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testServer != null && testServer.isStarted()) {
            testServer.stopServer();
        }
    }

    /**
     * Test that when the server is not in beta mode, the protected resource metadata endpoint
     * returns 404 even when the OIDC client configuration has a {@code protectedResourceMetadata}
     * sub-element with {@code advertisedScopes} configured.
     */
    @Test
    public void testMetadataNotServedInNonBetaMode() throws Exception {
        String metadataUrl = serverHttpString + PROTECTED_RESOURCE_METADATA_PATH + "/myApp/protected";
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new GetMethodWebRequest(metadataUrl);
        WebResponse response = wc.getResponse(request);

        assertEquals("Expected 404 when server is not in beta mode, even with protectedResourceMetadata configured",
                404, response.getResponseCode());
    }
}
