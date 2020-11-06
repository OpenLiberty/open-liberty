/*******************************************************************************
 * Copyright (c) 2014, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.fat.wsoc.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.ws.fat.LoggingTest;
import com.ibm.ws.fat.util.OnlyRunNotOnZRule;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.WebServerSetup;
import com.ibm.ws.fat.util.browser.WebResponse;
import com.ibm.ws.fat.util.wsoc.WsocTest;
import com.ibm.ws.fat.wsoc.tests.all.TraceEnabledTest;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
public class TraceTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("traceTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS.getHost(), SS.getPort(), false);

    private final TraceEnabledTest mct = new TraceEnabledTest(wt);

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        return SS.verifyResponse(createWebBrowserForTestCase(),
                                 "/trace/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + SS.getHost() + "&targetport=" + SS.getPort()
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

    @Mode(TestMode.LITE)
    @Test
    public void testProgrammaticCloseSuccessOnOpen() throws Exception {
        mct.testProgrammaticCloseSuccessOnOpen();
    }

    @Mode(TestMode.LITE)
    @Test
    public void testSSCProgrammaticCloseSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccess");
    }

    @Mode(TestMode.FULL)
    @Test
    public void testSSCProgrammaticCloseSuccessOnOpen() throws Exception {
        this.runAsSSCAndVerifyResponse("TraceEnabledTest", "testProgrammaticCloseSuccessOnOpen");
    }

    @Mode(TestMode.LITE)
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

}