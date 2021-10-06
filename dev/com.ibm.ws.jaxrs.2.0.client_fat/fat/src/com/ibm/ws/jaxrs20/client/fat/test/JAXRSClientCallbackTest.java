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
public class JAXRSClientCallbackTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRSClientCallbackTest")
    public static LibertyServer server;

    private final static String appname = "jaxrs20clientcallback";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, appname,
                                      "com.ibm.ws.jaxrs20.client.callback.client",
                                      "com.ibm.ws.jaxrs20.client.callback.server");

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
    public void testClientAPIInsideInvocationCallback() throws Exception {
        this.runTestOnServer(target, "testClientAPIInsideInvocationCallback", null, "PASS");
    }

    @Test
    public void testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_Response() throws Exception {
        this.runTestOnServer(target, "testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_Response", null, "completed hello");
    }

    @Test
    public void testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_String() throws Exception {
        this.runTestOnServer(target, "testCanReadEntityAndConsumeInvocationCallbackWithoutBuffering_String", null, "hello hello");
    }
}
