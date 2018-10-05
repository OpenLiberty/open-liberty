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

import org.jboss.shrinkwrap.api.spec.WebArchive;
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
public class ClientContextInjectionTest extends AbstractTest {

    @Server("jaxrs20.client.ClientContextInjectionTest")
    public static LibertyServer server;

    private final static String appname = "clientcontextinjection";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.client",
                                                       "com.ibm.ws.jaxrs20.client.ClientContextInjectionTest.service");

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

    /**
     * CTS Tests
     *
     */

    @Test
    public void CTSTestClientContextInjection_returnGivenString() throws Exception {
        this.runTestOnServer(target, "testClientContextInjection_returnGivenString", null, "Hello");
    }

    @Test
    public void CTSTestClientContextInjection_reader() throws Exception {
        this.runTestOnServer(target, "testClientContextInjection_reader", null, "101111111");
    }

    @Test
    public void CTSTestClientContextInjection_writer() throws Exception {
        this.runTestOnServer(target, "testClientContextInjection_writer", null, "11111111");
    }

    @Test
    public void CTSTestClientContextInjection_method() throws Exception {
        this.runTestOnServer(target, "testClientContextInjection_method", null, "11111111");
    }

    @Test
    public void CTSTestClientContextInjection_application() throws Exception {
        this.runTestOnServer(target, "testClientContextInjection_application", null, "11111111");
    }
}