/*******************************************************************************
 * Copyright (c) 2014, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.wsoc.util.DontRunWithWebServerRule;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
@RunWith(FATRunner.class)
public class SecureTest {
    public static final String SERVER_NAME = "secureTestServer";
    @Server(SERVER_NAME)

    public static LibertyServer LS;

    private static WebServerSetup bwst = null;
    private static WsocTest wt = null;

    @ClassRule
    public static final TestRule WebServerRule = new DontRunWithWebServerRule();

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static final Logger LOG = Logger.getLogger(SecureTest.class.getName());

    private static final String SECURE_WAR_NAME = "secure";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive SecureApp = ShrinkHelper.buildDefaultApp(SECURE_WAR_NAME + ".war",
                                                            "secure.war",
                                                            "io.openliberty.wsoc.common",
                                                            "io.openliberty.wsoc.util.wsoc",
                                                            "io.openliberty.wsoc.tests.all",
                                                            "io.openliberty.wsoc.endpoints.client.secure");
        SecureApp = (WebArchive) ShrinkHelper.addDirectory(SecureApp, "test-applications/" + SECURE_WAR_NAME + ".war/resources");

        ShrinkHelper.exportAppToServer(LS, SecureApp);
        LS.addInstalledAppForValidation(SECURE_WAR_NAME);

        bwst = new WebServerSetup(LS);
        bwst.setUp();
        wt = new WsocTest(LS, false);

        LS.startServer();

        // tests cannot work until ssl is up
        LS.waitForStringInLog("CWWKS4105I:.*configuration is ready.*");
        // tests cannot work until ssl is up
        LS.waitForStringInLog("CWWKO0219I:.*ssl.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer("CWWKH0039E");
        }
        bwst.tearDown();
    }

    protected WebResponse runAsLSAndVerifyResponse(String className, String testName) throws Exception {
        int securePort = 0, port = 0;
        String host = "";
        LibertyServer server = LS;
        if (WebServerControl.isWebserverInFront()) {
            try {
                host = WebServerControl.getHostname();
                securePort = WebServerControl.getSecurePort();
                port = Integer.valueOf(WebServerControl.getPort()).intValue();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get ports or host from webserver", e);
            }
        } else {
            securePort = server.getHttpDefaultSecurePort();
            host = server.getHostname();
            port = server.getHttpDefaultPort();
        }
        WebBrowser browser = WebBrowserFactory.getInstance().createWebBrowser((File) null);
        String[] expectedInResponse = {
                                        "SuccessfulTest"
        };
        // seem odd, but "context" is the root here because the client side app lives in the context war file
        return verifyResponse(browser,
                              "/secure/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                       + "&secureport=" + securePort + "&secure=true",
                              expectedInResponse);
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the all of the supplied text
     * specified by the array of * <code>expectedResponses</code>
     *
     * @param webBrowser        the browser used to submit the request
     * @param resource          the resource on the shared server to request
     * @param expectedResponses an array of the different subsets of the text expected from the HTTP response
     * @return the HTTP response (in case further validation is required)
     * @throws Exception if the <code>expectedResponses</code> is not contained in the HTTP response body
     */
    public WebResponse verifyResponse(WebBrowser webBrowser, String resource, String[] expectedResponses) throws Exception {
        WebResponse response = webBrowser.request(HttpUtils.createURL(LS, resource).toString());
        LOG.info("Response from webBrowser: " + response.getResponseBody());
        for (String textToFind : expectedResponses) {
            response.verifyResponseBodyContains(textToFind);
        }

        return response;
    }

    @Test
    public void testSSCAnnotatedSecureTextSuccess() throws Exception {
        this.runAsLSAndVerifyResponse("SecurityTest", "testAnnotatedSecureSuccess");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCWSSRequired() throws Exception {
        this.runAsLSAndVerifyResponse("SecurityTest", "testWSSRequired");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCAnnotatedSecureForbidden() throws Exception {
        this.runAsLSAndVerifyResponse("SecurityTest", "testAnnotatedSecureForbidden");
    }

}