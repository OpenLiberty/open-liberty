/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS20ClientAsyncInvokerTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRS20ClientAsyncInvokerTest")
    public static LibertyServer server;

    private final static String appname = "bookstore";
    private final static String asyncInvokerTarget = appname + "/AsyncInvokerTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.fat.bookstore");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("CWWKE1102W");  //ignore server quiesce timeouts due to slow test machines
        }
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    @Test
    public void testAsyncInvoker_get1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_get1", p, "Good book");
    }

    @Test
    public void testAsyncInvoker_get2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_get2", p, "Good book");
    }

    //@Test
    public void testAsyncInvoker_get3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_get3", p, "true");
    }

    @Test
    public void testAsyncInvoker_get4() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_get4", p, "Good book");
    }

    @Test
    public void testAsyncInvoker_post1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_post1", p, "Test book");
    }

    @Test
    public void testAsyncInvoker_post2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_post2", p, "Test book2");
    }

    //@Test
    public void testAsyncInvoker_post3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_post3", p, "Test book3");
    }

    @Test
    public void testAsyncInvoker_post4() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_post4", p, "Test book4");
    }

    @Test 
    public void testAsyncInvoker_getReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_getReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    @AllowedFFDC("javax.ws.rs.ProcessingException")
    public void testAsyncInvoker_getConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_getConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    public void testAsyncInvoker_postReceiveTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_postReceiveTimeout", p, "Timeout as expected");
    }

    @Test
    @AllowedFFDC("javax.ws.rs.ProcessingException")
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    public void testAsyncInvoker_postConnectionTimeout() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_postConnectionTimeout", p, "Timeout as expected");
    }

    @Test
    @AllowedFFDC("javax.ws.rs.ProcessingException")
    public void testAsyncInvoker_getReceiveTimeoutwithInvocationCallback() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_getReceiveTimeoutwithInvocationCallback", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    @AllowedFFDC("javax.ws.rs.ProcessingException")
    public void testAsyncInvoker_getConnectionTimeoutwithInvocationCallback() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_getConnectionTimeoutwithInvocationCallback", p, "Timeout as expected");
    }

    @Test
    public void testAsyncInvoker_postReceiveTimeoutwithInvocationCallback() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_postReceiveTimeoutwithInvocationCallback", p, "Timeout as expected");
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16651
    @AllowedFFDC("javax.ws.rs.ProcessingException")
    public void testAsyncInvoker_postConnectionTimeoutwithInvocationCallback() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(asyncInvokerTarget, "testAsyncInvoker_postConnectionTimeoutwithInvocationCallback", p, "Timeout as expected");
    }
}
