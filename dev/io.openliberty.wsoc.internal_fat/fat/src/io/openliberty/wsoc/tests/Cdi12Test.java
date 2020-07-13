/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.wsoc.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.fat.util.LoggingTest;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.browser.WebResponse;

import io.openliberty.wsoc.util.OnlyRunNotOnZRule;
import io.openliberty.wsoc.util.WebServerSetup;
import io.openliberty.wsoc.util.wsoc.WsocTest;
import io.openliberty.wsoc.tests.all.CdiTest;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 *
 */
public class Cdi12Test extends LoggingTest {

    // private static final Logger LOG = Logger.getLogger(ContextTest.class.getName());

    @ClassRule
    public static SharedServer SS = new SharedServer("cdi12TestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS, false);

    private final CdiTest ct = new CdiTest(wt);

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        // seem odd, but "context" is the root here because the client side app lives in the context war file
        return SS.verifyResponse(createWebBrowserForTestCase(),
                                 "/context/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + SS.getHost() + "&targetport=" + SS.getPort()
                                                 + "&secureport=" + SS.getSecurePort(),
                                 "SuccessfulTest");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        bwst.setUp();

    }

    @AfterClass
    public static void tearDown() throws Exception {
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
