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
public class JAXRS21ClientCallbackTest extends JAXRS21AbstractTest {

    @Server("jaxrs21.client.JAXRS21ClientCallbackTest")
    public static LibertyServer server;

    private static final String clientcallbackwar = "jaxrs21clientcallback";

    private final static String target = "jaxrs21clientcallback/JAXRS21ClientTestServlet";

    @BeforeClass
    public static void setup() throws Exception {

        ShrinkHelper.defaultDropinApp(server, clientcallbackwar,
                                      "com.ibm.ws.jaxrs21.client.callback.client",
                                      "com.ibm.ws.jaxrs21.client.callback.server");

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
        server.stopServer();
    }

    @Before
    public void preTest() {
        serverRef = server;
    }

    @After
    public void afterTest() {
        serverRef = null;
    }

    /**
     * Test: Test the new Reactive client by simply changing the use of async to rx
     *
     * Expected Results: No Exceptions
     */

    @Test
    public void testClientAPIInsideInvocationCallbackWithReactiveClient() throws Exception {
        this.runTestOnServer(target, "testClientAPIInsideInvocationCallbackWithReactiveClient", null, "PASS");
    }

}
