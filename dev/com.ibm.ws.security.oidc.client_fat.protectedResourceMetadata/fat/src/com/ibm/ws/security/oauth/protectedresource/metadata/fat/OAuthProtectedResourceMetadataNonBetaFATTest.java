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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth.protectedresource.metadata.fat.hello.app.HelloServlet;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpsRequest;

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

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RepeatOnServer("oidcClient", "com.ibm.ws.security.oauth.oidc_fat.common.metadataServerNonBeta"))
            .andWith(new RepeatOnServer("socialLogin", "com.ibm.ws.security.oauth.oidc_fat.common.sociallogin.metadataServerNonBeta"));

    private static LibertyServer testServer;

    private static String serverHttpsString;

    @BeforeClass
    public static void setUp() throws Exception {
        String methodName = "setUp";

        testServer = LibertyServerFactory.getLibertyServer(RepeatOnServer.getServerName(), thisClass);

        // Deploy test app directly to publish before security FAT framework starts the server
        WebArchive helloWar = ShrinkWrap.create(WebArchive.class, "hello.war")
                .addClass(HelloServlet.class)
                .setWebXML(HelloServlet.class.getResource("web.xml"));
        ShrinkHelper.exportAppToServer(testServer, helloWar, SERVER_ONLY);

        Log.info(thisClass, methodName, "Starting non-beta server: " + testServer.getServerName());

        testServer.startServer();

        serverHttpsString = "https://" + testServer.getHostname() + ":" + testServer.getHttpDefaultSecurePort();

        Log.info(thisClass, methodName, "Non-beta server started at: " + serverHttpsString);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (testServer != null && testServer.isStarted()) {
            testServer.stopServer("CWWKS5375E"); // socialLogin: Access token required but not provided
        }
    }

    /**
     * Test that when the server is not in beta mode, the protected resource metadata endpoint
     * returns 404 even when the OIDC client configuration has a {@code protectedResourceMetadata}
     * sub-element with {@code advertisedScopes} configured.
     */
    @Test
    public void testMetadataNotServedInNonBetaMode() throws Exception {
        String metadataUrl = serverHttpsString + PROTECTED_RESOURCE_METADATA_PATH + "/myApp/protected";
        WebConversation wc = new WebConversation();
        wc.setExceptionsThrownOnErrorStatus(false);
        WebRequest request = new GetMethodWebRequest(metadataUrl);
        WebResponse response = wc.getResponse(request);

        assertEquals("Expected 404 when server is not in beta mode, even with protectedResourceMetadata configured",
                404, response.getResponseCode());
    }

    @Test
    public void test401HasNoResourceMetadataInNonBetaMode() throws Exception {

        HttpsRequest req = new HttpsRequest(testServer, "/myApp/protected");
        req.allowInsecure();
        req.expectCode(401);
        req.run(String.class); // Don't actually care about the response body
        String wwwAuthenticateHeader = req.getResponseHeader("WWW-Authenticate");
        if (wwwAuthenticateHeader != null) {
            assertThat("Did not expect resource_metadata returned when server in non beta mode",
                    wwwAuthenticateHeader, not(containsString("resource_metadata")));
        }
    }
}
