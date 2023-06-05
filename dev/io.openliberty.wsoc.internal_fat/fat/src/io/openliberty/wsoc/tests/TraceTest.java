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
import io.openliberty.wsoc.tests.all.TraceEnabledTest;
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
public class TraceTest {

    public static final String SERVER_NAME = "traceTestServer";
    @Server(SERVER_NAME)

    public static LibertyServer LS;

    private static WebServerSetup bwst = null;

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static WsocTest wt = null;;
    private static TraceEnabledTest mct = null;

    private static final Logger LOG = Logger.getLogger(TraceTest.class.getName());

    private static final String TRACE_WAR_NAME = "trace";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive TraceApp = ShrinkHelper.buildDefaultApp(TRACE_WAR_NAME + ".war",
                                                           "trace.war",
                                                           "trace.war.configurator",
                                                           "io.openliberty.wsoc.common",
                                                           "io.openliberty.wsoc.util.wsoc",
                                                           "io.openliberty.wsoc.tests.all",
                                                           "io.openliberty.wsoc.endpoints.client.trace");
        TraceApp = (WebArchive) ShrinkHelper.addDirectory(TraceApp, "test-applications/" + TRACE_WAR_NAME + ".war/resources");
        ShrinkHelper.exportDropinAppToServer(LS, TraceApp);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + TRACE_WAR_NAME);

        bwst = new WebServerSetup(LS);
        bwst.setUp();
        wt = new WsocTest(LS, false);
        mct = new TraceEnabledTest(wt);

    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (LS != null && LS.isStarted()) {
            LS.stopServer();
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
                              "/trace/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
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

    @Test
    public void testProgrammaticCloseSuccessOnOpen() throws Exception {
        mct.testProgrammaticCloseSuccessOnOpen();
    }

    @Test
    public void testSSCProgrammaticCloseSuccess() throws Exception {
        this.runAsLSAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticCloseSuccessOnOpen() throws Exception {
        this.runAsLSAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccessOnOpen");
    }

    @Test
    public void testSSCConfiguratorSuccess() throws Exception {
        this.runAsLSAndVerifyResponse("TraceEnabledTest", "testConfiguratorSuccess");
    }

    // Move to trace test bucket because of build break 217622
    @Mode(TestMode.FULL)
    @Test
    public void testSSCMultipleClientsPublishingandReceivingToThemselvesTextSuccess() throws Exception {
        this.runAsLSAndVerifyResponse("TraceEnabledTest", "testMultipleClientsPublishingandReceivingToThemselvesTextSuccess");
    }

    // Move to trace test bucket because of build break 244260
    @Mode(TestMode.FULL)
    @Test
    public void testSinglePublisherMultipleReciverTextSuccess() throws Exception {
        mct.testSinglePublisherMultipleReciverTextSuccess();
    }

    // uncomment/Enable this test if it is the only LITE test, since we want at least one test to run.
    //@Mode(TestMode.LITE)
    //@Test
    //public void testAsyncAnnotatedTextSuccess() throws Exception {
    //    mct.testAsyncAnnotatedTextSuccess();
    //}

}