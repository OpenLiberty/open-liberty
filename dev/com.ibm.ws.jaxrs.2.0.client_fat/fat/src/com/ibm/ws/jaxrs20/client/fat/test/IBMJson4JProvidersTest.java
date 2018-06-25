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
public class IBMJson4JProvidersTest extends AbstractTest {

    @Server("jaxrs20.client.IBMJson4JProvidersTest")
    public static LibertyServer server;

    private final static String appname = "ibmjson4j";
    private final static String target_ibmjson4j = appname + "/ClientTestServlet";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkHelper.defaultDropinApp(server, appname,
                                                       "com.ibm.ws.jaxrs20.client.fat.ibmjson4j.client",
                                                       "com.ibm.ws.jaxrs20.client.fat.ibmjson4j.service");

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
    public void testNewClient_ClientIBMJson4JProviders() throws Exception {
        this.runTestOnServer(target_ibmjson4j, "testNewClient_ClientIBMJson4JProviders", null, "OK");
    }

    @Test
    public void testNewWebTarget_ClientIBMJson4JProviders() throws Exception {
        this.runTestOnServer(target_ibmjson4j, "testNewWebTarget_ClientIBMJson4JProviders", null, "OK");
    }

    @Test
    public void testFlowProgram_ClientIBMJson4JProviders_JSONObject() throws Exception {
        this.runTestOnServer(target_ibmjson4j, "testFlowProgram_ClientIBMJson4JProviders_JSONObject_Get", null, "Jordan");
        this.runTestOnServer(target_ibmjson4j, "testFlowProgram_ClientIBMJson4JProviders_JSONObject_Post", null, "Ellen");
    }

    @Test
    public void testFlowProgram_ClientIBMJson4JProviders_JSONArray() throws Exception {
        this.runTestOnServer(target_ibmjson4j, "testFlowProgram_ClientIBMJson4JProviders_JSONArray_Get", null, "IBM");
        this.runTestOnServer(target_ibmjson4j, "testFlowProgram_ClientIBMJson4JProviders_JSONArray_Post", null, "CXF");
    }

    @Test
    public void testFlowProgram_ClientIBMJson4JProviders_JSONJAXB() throws Exception {
        this.runTestOnServer(target_ibmjson4j, "testFlowProgram_ClientIBMJson4JProviders_JAXB_Get", null, "Java");
    }

    //TODO: we should also migrate more jaxrs-1.1 cases here
}
