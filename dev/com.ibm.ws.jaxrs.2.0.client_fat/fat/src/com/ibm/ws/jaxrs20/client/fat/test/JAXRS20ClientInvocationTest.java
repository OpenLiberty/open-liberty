/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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

import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS20ClientInvocationTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRS20ClientInvocationTest")
    public static LibertyServer server;

    private final static String appname = "bookstore";
    private final static String invocationTarget = appname + "/InvocationTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname, "com.ibm.ws.jaxrs20.fat.bookstore");

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
            server.stopServer();
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
    public void testClientClass() throws Exception {
        Map<String, String> p = new HashMap<String, String>();        
        if (JakartaEE9Action.isActive()) {
            this.runTestOnServer(invocationTarget, "testClientClass", p, "io.openliberty.org.jboss.resteasy.common.client.LibertyResteasyClientImpl");
        } else {
            this.runTestOnServer(invocationTarget, "testClientClass", p, "com.ibm.ws.jaxrs20.client.JAXRSClientImpl");
        }
    }

    @Test
    @SkipForRepeat("EE9_FEATURES") // Skip this test for EE9 as this test is failing intermittently with EE9.  See issue https://github.com/OpenLiberty/open-liberty/issues/16650
    public void testInvocation_invoke1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_invoke1", p, "Good book");
    }

    @Test
    public void testInvocation_invoke2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_invoke2", p, "Good book");
    }

    // @Test
    public void testInvocation_invoke3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_invoke3", p, "true");
    }

    @Test
    public void testInvocation_submit1() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_submit1", p, "Good book");
    }

    @Test
    public void testInvocation_submit2() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_submit2", p, "Good book");
    }

//    @Test
    public void testInvocation_submit3() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_submit3", p, "true");
    }

    @Test
    public void testInvocation_submit4() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_submit4", p, "Good book");
    }

    @Test
    public void testInvocation_property() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocation_property", p, "123");
    }

    @Test
    public void testInvocationBuilder_property() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        this.runTestOnServer(invocationTarget, "testInvocationBuilder_property", p, "123");
    }
}
