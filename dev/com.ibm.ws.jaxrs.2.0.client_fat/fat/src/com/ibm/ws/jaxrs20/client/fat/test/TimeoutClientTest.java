/*******************************************************************************
 * Copyright (c) 2018, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class TimeoutClientTest extends AbstractTest {

    @Server("jaxrs20.client.TimeoutClientTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclienttimeout";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname,
                                      "com.ibm.ws.jaxrs20.client.jaxrsclienttimeout.client",
                                      "com.ibm.ws.jaxrs20.client.jaxrsclienttimeout.service");

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
            server.stopServer("CWWKW0700E", "CWWKW1306W");
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
    public void testTimeoutWork() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutWork");
        p.put("timeout", "1000"); //Return time specified on server side is 2000
        this.runTestOnServer(target, "testTimeout", p,
                             "[Timeout Error]:javax.ws.rs.ProcessingException: java.net.SocketTimeoutException: SocketTimeoutException", //CXF
                             "[Timeout Error]:jakarta.ws.rs.ProcessingException: RESTEASY004655"); //RESTEasy
    }

    @Test
    public void testTimeoutNotWork() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutNotWork");
        p.put("timeout", "10000"); //245493 CLGH<10277>
        this.runTestOnServer(target, "testTimeout", p, "[Basic Resource]:timeoutNotWork");
    }

    @Test
    @AllowedFFDC("java.lang.NumberFormatException") //CXF logs a NFE, RESTEasy does not - it prints a CWWKW1302W warning
    public void testTimeoutValueInvalid() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "timeoutValueInvalid");
        p.put("timeout", "invalidValue"); //will set the default 30000
        this.runTestOnServer(target, "testTimeout", p, "[Basic Resource]:timeoutValueInvalid");
    }
}