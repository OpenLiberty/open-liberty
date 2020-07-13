/*******************************************************************************
 * Copyright (c) 2015, 2020 IBM Corporation and others.
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

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.wsoc.tests.all.CdiTest;
import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerControl;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;

/**
 * Skipped for EE9 until the jmsServer and jmsClient for JakartaEE9 are developed and delivered
 * to test CDI and Websocket integration
 */
@RunWith(FATRunner.class)
@SkipForRepeat(SkipForRepeat.EE9_FEATURES)
public class Cdi12Test extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("cdi12TestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final CdiTest ct = new CdiTest(wt);

    private static final Logger LOG = Logger.getLogger(Cdi12Test.class.getName());

    private static final String CDI_WAR_NAME = "cdi";
    private static final String CONTEXT_WAR_NAME = "context";

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        int securePort = 0, port = 0;
        String host="";
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
        WebArchive CdiApp = ShrinkHelper.buildDefaultApp(CDI_WAR_NAME + ".war",
                                                                         "cdi.war",
                                                                         "io.openliberty.wsoc.common",
                                                                         "io.openliberty.wsoc.util.wsoc",
                                                                         "io.openliberty.wsoc.tests.all",
                                                                         "io.openliberty.wsoc.endpoints.client.basic",
                                                                         "io.openliberty.wsoc.endpoints.client.context");
        CdiApp = (WebArchive) ShrinkHelper.addDirectory(CdiApp, "test-applications/"+CDI_WAR_NAME+".war/resources");
        WebArchive ContextApp = ShrinkHelper.buildDefaultApp(CONTEXT_WAR_NAME + ".war",
                                                                         "context.war",
                                                                         "io.openliberty.wsoc.common",
                                                                         "io.openliberty.wsoc.util.wsoc",
                                                                         "io.openliberty.wsoc.tests.all",
                                                                         "io.openliberty.wsoc.endpoints.client.basic",
                                                                         "io.openliberty.wsoc.endpoints.client.context");
        ContextApp = (WebArchive) ShrinkHelper.addDirectory(ContextApp, "test-applications/"+CONTEXT_WAR_NAME+".war/resources");
        // Verify if the apps are in the server before trying to deploy them
        if (SS.getLibertyServer().isStarted()) {
            Set<String> appInstalled = SS.getLibertyServer().getInstalledAppNames(CDI_WAR_NAME);
            LOG.info("addAppToServer : " + CDI_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
            ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), CdiApp);

            appInstalled = SS.getLibertyServer().getInstalledAppNames(CONTEXT_WAR_NAME);
            LOG.info("addAppToServer : " + CONTEXT_WAR_NAME + " already installed : " + !appInstalled.isEmpty());
            if (appInstalled.isEmpty())
            ShrinkHelper.exportDropinAppToServer(SS.getLibertyServer(), ContextApp);
        }
        SS.startIfNotStarted();
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CDI_WAR_NAME);
        SS.getLibertyServer().waitForStringInLog("CWWKZ0001I.* " + CONTEXT_WAR_NAME);
        bwst.setUp();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (SS.getLibertyServer() != null && SS.getLibertyServer().isStarted()) {
            SS.getLibertyServer().stopServer(null);
        }
        bwst.tearDown();
    }

    //
    //   CDI 1.2 TESTS

    @Mode(TestMode.LITE)
    @Test
    public void testCdiInterceptor() throws Exception {
        ct.testCdiInterceptor();
    }

    // this test seem to require the jms stuff to accessible for teardown, not sure why
    @Mode(TestMode.FULL)
    @Test
    public void testSSCCdiInterceptor() throws Exception {
        this.runAsSSCAndVerifyResponse("CdiTest", "testCdiInterceptor");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCdiInjectCDI12() throws Exception {
        ct.testCdiInjectCDI12();
    }

    // this test seem to require the jms stuff to accessible for teardown, not sure why
    @Mode(TestMode.LITE)
    @Test
    public void testSSCCdiInjectCDI12() throws Exception {
        this.runAsSSCAndVerifyResponse("CdiTest", "testCdiInjectCDI12");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testCdiProgrammaticEndpointCDI12() throws Exception {
        ct.testCdiProgrammaticEndpointCDI12();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testCdiProgrammaticEndpointMultipleOnMessageCDI12() throws Exception {
        ct.testCdiProgrammaticEndpointMultipleOnMessageCDI12();
    }

    @Mode(TestMode.FULL)
    @Test
    public void testClientCDIOneCDI12() throws Exception {
        // Client CDI side did not use Session Scope, so no need to change anything here
        this.verifyResponse("testClientCDIOne");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testClientCDITwoCDI12() throws Exception {
        // Client CDI side did not use Session Scope, so no need to change anything here
        this.verifyResponse("testClientCDITwo");
    }

    @Mode(TestMode.LITE)
    @Test
    public void testCdiInjectWithIdleTimeout() throws Exception {
        ct.testCdiInjectWithIdleTimeout();
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
