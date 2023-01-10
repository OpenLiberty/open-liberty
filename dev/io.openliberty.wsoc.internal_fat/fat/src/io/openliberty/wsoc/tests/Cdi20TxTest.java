/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.io.File;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Filters;
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
import io.openliberty.wsoc.tests.all.CdiTest;
import io.openliberty.wsoc.tests.all.HeaderTest;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

@RunWith(FATRunner.class)
public class Cdi20TxTest {
    public static final String SERVER_NAME = "cdi20TxTestServer";
    @Server(SERVER_NAME)

    public static LibertyServer LS;

    private static WebServerSetup bwst = null;

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private static WsocTest wt = null;

    private static CdiTest ct = null;

    private static final Logger LOG = Logger.getLogger(Cdi20TxTest.class.getName());

    private static final String CDI_TX_WAR_NAME = "cditx";

    @BeforeClass
    public static void setUp() throws Exception {
        // Build the war app and add the dependencies
        WebArchive CdiApp = ShrinkHelper.buildDefaultApp(CDI_TX_WAR_NAME + ".war",
                                                         "cditx.war",
                                                         "io.openliberty.wsoc.common",
                                                         "io.openliberty.wsoc.util.wsoc",
                                                         "io.openliberty.wsoc.endpoints.client.basic",
                                                         "io.openliberty.wsoc.endpoints.client.context",
                                                         "io.openliberty.wsoc.endpoints.client.trace");
        CdiApp = (WebArchive) ShrinkHelper.addDirectory(CdiApp, "test-applications/" + CDI_TX_WAR_NAME + ".war/resources");
        // Exclude header test since not being used anywhere for CDI testing
        CdiApp = CdiApp.addPackages(true, Filters.exclude(HeaderTest.class), "io.openliberty.wsoc.tests.all");

        ShrinkHelper.exportDropinAppToServer(LS, CdiApp);

        LS.startServer();
        LS.waitForStringInLog("CWWKZ0001I.* " + CDI_TX_WAR_NAME);

        bwst = new WebServerSetup(LS);
        bwst.setUp();
        wt = new WsocTest(LS, false);
        ct = new CdiTest(wt);
    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        // Reset Variables for tests after tests have finished
        CdiTest.resetTests();
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
                              "/context/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                       + "&secureport=" + securePort,
                              expectedInResponse);
    }

    //
    //   CDI 2.0 Transaction TESTS

    @Mode(TestMode.LITE)
    @Test
    public void testTxNeverCdiInjectCDI20() throws Exception {
        ct.testCdiTxNeverInjectCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTxRequiredCdiInjectCDI20() throws Exception {
        ct.testCdiTxRequiredInjectCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTxMandatoryCdiInjectCDI20() throws Exception {
        ct.testCdiTxMandatoryInjectCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTxNotSupportedCdiInjectCDI20() throws Exception {
        ct.testCdiTxNotSupportedInjectCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTxRequiresNewCdiInjectCDI20() throws Exception {
        ct.testCdiTxRequiresNewInjectCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testTxSupportsCdiInjectCDI20() throws Exception {
        ct.testCdiTxSupportsInjectCDI12();
    }

    /**
     * Submits an HTTP request at the path specified by <code>resource</code>,
     * and verifies that the HTTP response body contains the all of the supplied text
     * specified by the array of * <code>expectedResponses</code>
     *
     * @param webBrowser the browser used to submit the request
     * @param resource the resource on the shared server to request
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

    protected WebResponse verifyResponse(String testName) throws Exception {
        WebBrowser browser = WebBrowserFactory.getInstance().createWebBrowser((File) null);
        String[] expectedInResponse = {
                                        "SuccessfulTest"
        };
        return verifyResponse(browser, "/cdi/RequestCDI?testname=" + testName, expectedInResponse);
    }

}
