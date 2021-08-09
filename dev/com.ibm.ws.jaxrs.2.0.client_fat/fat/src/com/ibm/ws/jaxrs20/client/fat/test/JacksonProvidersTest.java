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
//import com.ibm.ws.jaxrs20.fat.TestUtils;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class JacksonProvidersTest extends AbstractTest {

    @Server("jaxrs20.client.JacksonProvidersTest")
    public static LibertyServer server;

    private final static String appname = "jackson";
    private final static String target_jackson = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.fat.jackson.client",
                                                       "com.ibm.ws.jaxrs20.client.fat.jackson.service");

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
    public void testNewClient_ClientJacksonProviders() throws Exception {
        this.runTestOnServer(target_jackson, "testNewClient_ClientJacksonProviders", null, "OK");
    }

    @Test
    public void testNewInvocationBuilder_ClientJacksonProviders() throws Exception {
        this.runTestOnServer(target_jackson, "testNewInvocationBuilder_ClientJacksonProviders", null, "OK");
    }

    @Test
    public void testFlowProgram_ClientJacksonProviders_Pojo() throws Exception {
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_Pojo_Get", null, "first");
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_Pojo_Post", null, "jordan");
    }

    @Test
    public void testFlowProgram_ClientJacksonProviders_ListString() throws Exception {
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ListString_Get", null, "string1");
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ListString_Post", null, "ellen");
    }

    @Test
    public void testFlowProgram_ClientJacksonProviders_ListPojo() throws Exception {
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ListPojo_Get", null, "first1");
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ListPojo_Post", null, "jordan");
    }

    @Test
    public void testFlowProgram_ClientJacksonProviders_ArrayPojo() throws Exception {
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ArrayPojo_Get", null, "last3");
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_ArrayPojo_Post", null, "ellen");
    }

    @Test
    public void testFlowProgram_ClientJacksonProviders_Map() throws Exception {
        this.runTestOnServer(target_jackson, "testFlowProgram_ClientJacksonProviders_Map_Get", null, "firstArrValue");
    }
}
