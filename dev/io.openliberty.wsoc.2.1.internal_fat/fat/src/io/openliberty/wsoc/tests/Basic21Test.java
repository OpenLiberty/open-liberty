/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.browser.WebBrowser;
import com.ibm.ws.fat.util.browser.WebBrowserFactory;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import io.openliberty.wsoc.tests.all.MiscTest;
import io.openliberty.wsoc.tests.all.TimeOutTest;
import io.openliberty.wsoc.tests.all.UpgradeTest;
import io.openliberty.wsoc.tests.all.UserPropertiesTest;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.tests.all.SSLTest;

/**
 *  WebSocket 2.1 Tests
 */
@RunWith(FATRunner.class)
public class Basic21Test {

    @Server("basic21TestServer")
    public static LibertyServer LS;

    private static WebServerSetup bwst = null;

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static WsocTest wt = null;
    private static WsocTest wt_secure = null;
    private static TimeOutTest timeout = null;
    private static UserPropertiesTest userprop = null;
    private static UpgradeTest upgrade = null;
    private static MiscTest misc = null;
    private static SSLTest ssl = null;

    private static final Logger LOG = Logger.getLogger(Basic21Test.class.getName());

    private static final String BASIC_WAR_NAME = "basic21";

    @BeforeClass
    public static void setUp() throws Exception {

        // Build the war app and add the dependencies
        WebArchive BasicApp = ShrinkHelper.buildDefaultApp(BASIC_WAR_NAME + ".war",
                                                           "basic.war",
                                                           "basic.war.*",
                                                           "io.openliberty.wsoc.common",
                                                           "io.openliberty.wsoc.util.wsoc",
                                                           "io.openliberty.wsoc.tests.all",
                                                           "io.openliberty.wsoc.endpoints.client.basic");

        BasicApp = (WebArchive) ShrinkHelper.addDirectory(BasicApp, "test-applications/" + BASIC_WAR_NAME + ".war/resources");
        // BasicApp = BasicApp.addAsLibraries(BasicJar);
        ShrinkHelper.exportDropinAppToServer(LS, BasicApp);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + BASIC_WAR_NAME);
        bwst = new WebServerSetup(LS);
        wt = new WsocTest(LS, false);
        timeout = new TimeOutTest(wt);
        userprop = new UserPropertiesTest(wt);
        upgrade = new UpgradeTest(wt);
        misc = new MiscTest(wt);
        wt_secure = new WsocTest(LS, true);
        ssl = new SSLTest(wt_secure);
        bwst.setUp();

        // Allow Jetty to finish starting up - https://github.com/OpenLiberty/open-liberty/issues/23172
        // Updated to 5100 - Jan 2nd 2024
        Thread.sleep(5100);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer("CWWKH0023E", "CWWKH0020E", "CWWKH0039E", "CWWKH0040E", "SRVE8115W", "SRVE0190E");
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
        return verifyResponse(browser,
                              "/basic21/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                       + "&secureport=" + securePort,
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

    /*
     * The four tests below are used to test negative and zero timeouts
     * by confirming the "No timeout enabled" string is found in the logs.
     * Spec clarification as part of 2.1
     * https://github.com/jakartaee/websocket/issues/382
     * SKIPPED DUE TO Defect 291298
     * @Mode(TestMode.LITE)
     * @Test
     * public void testZeroTimeOut() throws Exception {
     *  timeout.testZeroTimeOut();
     *   String result  = LS.waitForStringInTraceUsingMark("Session timeout 0 is less than 1. No timeout enabled");
     *  assertNotNull("Timeout message not found!", result);
     * }
     */

     /*
      * ALSO SKIPPED DUE TO Defect 291298
      * @Mode(TestMode.LITE)
      * @Test
      * public void testNegativeTimeOut() throws Exception {
      *  timeout.testNegativeTimeOut();
      *  String result  = LS.waitForStringInTraceUsingMark("Session timeout -12 is less than 1. No timeout enabled");
      *  assertNotNull("Timeout message not found!", result);
      * }
      */


    /*
     * testSSC means liberty wsoc impl is the client and server
     * tests above use Jetty as the client
     */
    @Mode(TestMode.FULL)
    @Test
    public void testSSCZeroTimeOut() throws Exception {
        this.runAsLSAndVerifyResponse("TimeOutTest", "testZeroTimeOut");
        String result  = LS.waitForStringInTraceUsingMark("Session timeout 0 is less than 1. No timeout enabled");
        assertNotNull("Timeout message not found!", result);
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCNegativeTimeOut() throws Exception {
        this.runAsLSAndVerifyResponse("TimeOutTest", "testNegativeTimeOut");
        String result  = LS.waitForStringInTraceUsingMark("Session timeout -12 is less than 1. No timeout enabled");
        assertNotNull("Timeout message not found!", result);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCUserPropertiesOnServer() throws Exception {
        this.runAsLSAndVerifyResponse("UserPropertiesTest", "testUserPropertiesOnServer");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testUserPropertiesOnServer() throws Exception {
        userprop.testUserPropertiesOnServer();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCUserPropertiesOnClient() throws Exception {
        this.runAsLSAndVerifyResponse("UserPropertiesTest", "testUserPropertiesOnClient");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testUpgradeViaServletToWS() throws Exception {
        upgrade.testUpgradeViaServletToWS();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCUpgrade() throws Exception {
        this.runAsLSAndVerifyResponse("UpgradeTest", "testUpgradeViaServletToWS");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testFullURIReturned() throws Exception {
        misc.testGetRequestURIReturnsFullURI();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCFullURIReturned() throws Exception {
        this.runAsLSAndVerifyResponse("MiscTest", "testGetRequestURIReturnsFullURI");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testDefaultConfigurator() throws Exception {
        misc.testVerifyDefaultConfigurator();
    }

}
