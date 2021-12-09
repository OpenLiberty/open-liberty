/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs21.client.fat.test;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRS21TimeoutClientTest extends JAXRS21AbstractTest {
    @Server("jaxrs21.client.JAXRS21TimeoutClientTest")
    public static LibertyServer server;

    private static final String clienttimeoutwar = "jaxrs21clienttimeout";

    private final static String target = "jaxrs21clienttimeout/JAXRS21ClientTestServlet";

    private final static String ignoreErrorOrWarningMsg = ".CWWKW0700E.";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, clienttimeoutwar,
                                      "com.ibm.ws.jaxrs21.client.jaxrs21clienttimeout.client",
                                      "com.ibm.ws.jaxrs21.client.jaxrs21clienttimeout.service");


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
        server.stopServer(ignoreErrorOrWarningMsg);
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
    public void testTimeoutWork() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutWork");
        p.put("timeout", "1000"); // Return time specified on server side is 2000
        this.runTestOnServer(target, "testTimeout", p, "[Timeout Error]:SocketTimeoutException");
    }

    @Test
    public void testTimeoutNotWork() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutNotWork");
        p.put("timeout", "10000");
        this.runTestOnServer(target, "testTimeout", p, "[Basic Resource]:timeoutNotWork");
    }

    @Test
    public void testTimeoutNonRoutable() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "testTimeoutNonRoutable");
        p.put("timeout", "10000");
        this.runTestOnServer(target, "testTimeoutNonRoutable", p, "[Basic Resource]:testTimeoutNonRoutable");
    }

    @Test
    // @ExpectedFFDC("java.lang.NumberFormatException") new code path so no FFDC
    public void testTimeoutValueInvalid() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutValueInvalid");
        p.put("timeout", "invalidValue"); // will set the default 30000
        this.runTestOnServer(target, "testTimeout", p, "[Basic Resource]:timeoutValueInvalid");
    }

}