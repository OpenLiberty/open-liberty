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
public class JsonPProvidersTest extends AbstractTest {

    @Server("jaxrs20.client.JsonPProvidersTest")
    public static LibertyServer server;

    private final static String appname = "jsonp";
    private final static String target_jsonp = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.fat.jsonp.client",
                                                       "com.ibm.ws.jaxrs20.client.fat.jsonp.service");

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
    public void testNewInvocationBuilder_ClientJsonPProviders() throws Exception {
        this.runTestOnServer(target_jsonp, "testNewInvocationBuilder_ClientJsonPProviders", null, "OK");
    }

    @Test
    public void testNewInvocation_ClientJsonPProviders() throws Exception {
        this.runTestOnServer(target_jsonp, "testNewInvocation_ClientJsonPProviders", null, "OK");
    }

    @Test
    public void testFlowProgram_ClientJsonPProviders_JsonObject() throws Exception {
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonObject_Get", null, "jordan");
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonObject_Post", null, "1");
    }

    @Test
    public void testFlowProgram_ClientJsonPProviders_JsonArray() throws Exception {
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonArray_Get", null, "alex");
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonArray_Post", null, "1");
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonArray_Post2", null, "bin");
    }

    @Test
    public void testFlowProgram_ClientJsonPProviders_JsonStructure() throws Exception {
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonStructure_Get", null, "ellen");
        this.runTestOnServer(target_jsonp, "testFlowProgram_ClientJsonPProviders_JsonStructure_Post", null, "1");
    }
}
