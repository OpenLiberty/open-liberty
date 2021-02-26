/*******************************************************************************
 * Copyright (c) 2014, 2020 IBM Corporation and others.
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
public class TraceTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("traceTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final TraceEnabledTest mct = new TraceEnabledTest(wt);

    private static final Logger LOG = Logger.getLogger(SecureTest.class.getName());

    private static final String TRACE_WAR_NAME = "trace";

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
                                 "/trace/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + host + "&targetport=" + port
                                                                + "&secureport=" + securePort,
                                 "SuccessfulTest");
    }

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
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(TRACE_WAR_NAME);
            LOG.info("addAppToServer : " + TRACE_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
                ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), TraceApp);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + TRACE_WAR_NAME);
        bwst.setUp();
        bwst.setUp();

    }

    @AfterClass
    public static void tearDown() throws Exception {

        // give the system 10 seconds to settle down before stopping
        try {
            Thread.sleep(10000);
        } catch (InterruptedException x) {

        }

        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer(null);
        }
        bwst.tearDown();
    }

    @Test
    public void testProgrammaticCloseSuccessOnOpen() throws Exception {
        mct.testProgrammaticCloseSuccessOnOpen();
    }

    @Test
    public void testSSCProgrammaticCloseSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticCloseSuccessOnOpen() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccessOnOpen");
    }

    @Test
    public void testSSCConfiguratorSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testConfiguratorSuccess");
    }

    // Move to trace test bucket because of build break 217622
    @Mode(TestMode.FULL)
    @Test
    public void testSSCMultipleClientsPublishingandReceivingToThemselvesTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testMultipleClientsPublishingandReceivingToThemselvesTextSuccess");
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