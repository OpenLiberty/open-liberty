/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import java.util.Set;
import java.util.logging.Logger;

import org.jboss.shrinkwrap.api.Filters;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.tests.all.CdiTest;
import io.openliberty.wsoc.tests.all.HeaderTest;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

@RunWith(FATRunner.class)
public class Cdi20TxTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("cdi20TxTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final CdiTest ct = new CdiTest(wt);

    private static final Logger LOG = Logger.getLogger(Cdi20TxTest.class.getName());

    private static final String CDI_TX_WAR_NAME = "cditx";

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        int securePort = 0, port = 0;
        String host = "";
        LibertyServer server = SS.getLibertyServer();
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
        // seem odd, but "context" is the root here because the client side app lives in the context war file
        return SS.verifyResponse(createWebBrowserForTestCase(),
                                 "/context/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                                                + "&secureport=" + securePort,
                                 "SuccessfulTest");
    }

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

        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(CDI_TX_WAR_NAME);
            LOG.info("addAppToServer : " + CDI_TX_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), CdiApp);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI_TX_WAR_NAME);

        bwst.setUp();
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
        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer(null);
        }
        bwst.tearDown();
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

    protected WebResponse verifyResponse(String testName) throws Exception {
        return SS.verifyResponse(createWebBrowserForTestCase(), "/cdi/RequestCDI?testname=" + testName, "SuccessfulTest");
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.fat.util.LoggingTest#getSharedServer()
     */
    @Override
    protected SharedServer getSharedServer() {
        return SS;
    }

}
