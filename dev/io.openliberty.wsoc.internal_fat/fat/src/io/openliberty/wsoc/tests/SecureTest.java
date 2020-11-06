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
import com.ibm.ws.fat.util.DontRunWithWebServerRule;
import com.ibm.ws.fat.util.OnlyRunNotOnZRule;
import com.ibm.ws.fat.util.SharedServer;
import com.ibm.ws.fat.util.WebServerSetup;
import com.ibm.ws.fat.util.browser.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;

/**
 * Tests WebSocket Stuff
 *
 * @author unknown
 */
public class SecureTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("secureTestServer", true, true);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @ClassRule
    public static final TestRule WebServerRule = new DontRunWithWebServerRule();

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    protected WebResponse runAsSSCAndVerifyResponse(String className, String testName) throws Exception {
        return SS.verifyResponse(createWebBrowserForTestCase(),
                                 "/secure/SingleRequest?classname=" + className + "&testname=" + testName + "&targethost=" + SS.getHost() + "&targetport=" + SS.getPort()
                                                                + "&secureport=" + SS.getSecurePort() + "&secure=true",
                                 "SuccessfulTest");
    }

    @BeforeClass
    public static void setUp() throws Exception {
        bwst.setUp();
        // tests cannot work until ssl is up
        SS.getLibertyServer().waitForStringInLog("CWWKO0219I:.*ssl.*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        bwst.tearDown();
    }

    @Test
    public void testSSCAnnotatedSecureTextSuccess() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testAnnotatedSecureSuccess");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCWSSRequired() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testWSSRequired");
    }

    @Test
    @AllowedFFDC({ "java.io.IOException" })
    public void testSSCAnnotatedSecureForbidden() throws Exception {
        this.runAsSSCAndVerifyResponse("SecurityTest", "testAnnotatedSecureForbidden");
    }

}