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
import com.ibm.ws.fat.util.wsoc.WsocTest;
import com.ibm.ws.fat.wsoc.endpoints.client.basic.AnnotatedClientEP;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.OnlyRunInJava7Rule;

/**
 *
 */
public class MiscellaneousTest extends LoggingTest {

    @ClassRule
    public static SharedServer SS = new SharedServer("miscellaneousTestServer", false);

    private static WebServerSetup bwst = new WebServerSetup(SS);

    @ClassRule
    public static final TestRule java7Rule = new OnlyRunInJava7Rule();

    @Rule
    public final TestRule notOnZRule = new OnlyRunNotOnZRule();

    private final WsocTest wt = new WsocTest(SS.getHost(), SS.getPort(), false);

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
    public void testNOCDIInjection1() throws Exception {

        String[] textValues = { "EJB Injection withoutCDI worked" };

        String uri = "/miscellaneous/miscNoCDIInjectedEndpoint";
        wt.runEchoTest(new AnnotatedClientEP.TextTest(textValues), uri, textValues);

    }

}
