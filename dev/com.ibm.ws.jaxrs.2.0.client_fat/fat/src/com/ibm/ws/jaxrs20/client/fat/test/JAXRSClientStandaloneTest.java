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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JAXRSClientStandaloneTest extends AbstractTest {

    @Server("jaxrs20.client.JAXRSClientStandaloneTest")
    public static LibertyServer server;

    private final static String appname = "jaxrsclientstandalone";
    private final static String target = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientStandalone.client",
                                                       "com.ibm.ws.jaxrs20.client.JAXRSClientStandalone.service");

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
    public void testNewClientBuilder_ClientStandalone() throws Exception {
        this.runTestOnServer(target, "testNewClientBuilder_ClientStandalone", null, "OK");
    }

    @Test
    public void testNewClient_ClientStandalone() throws Exception {
        this.runTestOnServer(target, "testNewClient_ClientStandalone", null, "OK");
    }

    @Test
    public void testNewWebTarget_ClientStandalone() throws Exception {
        this.runTestOnServer(target, "testNewWebTarget_ClientStandalone", null, "OK");
    }

    @Test
    public void testNewInvocationBuilder_ClientStandalone() throws Exception {
        this.runTestOnServer(target, "testNewInvocationBuilder_ClientStandalone", null, "OK");
    }

    @Test
    public void testNewInvocation_ClientStandalone() throws Exception {
        this.runTestOnServer(target, "testNewInvocation_ClientStandalone", null, "OK");
    }

    @Test
    public void testFlowProgram_ClientStandalone() throws Exception {
        Map<String, String> p = new HashMap<String, String>();
        p.put("param", "alex");
        this.runTestOnServer(target, "testFlowProgram_ClientStandalone", p, "[Basic Resource]:alex");
    }

    //TODO: we should also migrate more jaxrs-1.1 cases here
}
