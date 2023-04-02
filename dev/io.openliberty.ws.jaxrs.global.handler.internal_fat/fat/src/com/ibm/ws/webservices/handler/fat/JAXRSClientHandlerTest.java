/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
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
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URI;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
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
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JAXRSClientHandlerTest {
    @Server("RSTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("RSTestServer");
    private RestClient client;

    @BeforeClass
    public static void beforeClass() throws Exception {
        //server.installUserBundle("RSHandler1_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle1", "com.ibm.ws.rsuserbundle1.myhandler");
        TestUtils.installUserFeature(server, "RSHandler1Feature");
        //server.installUserBundle("RSHandler2_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle2", "com.ibm.ws.rsuserbundle2.myhandler");
        TestUtils.installUserFeature(server, "RSHandler2Feature");
        server.setServerConfigurationFile("dynamicallyAddRemoveJAXRS/WithTwo/server.xml");
        ShrinkHelper.defaultDropinApp(server, "rs20ApplicationWithClient", "com.ibm.samples.jaxrs");
    }
    @Before
    public void setUp() throws Exception {
        client = new RestClient();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
        server.uninstallUserBundle("rsUserBundle1");
        server.uninstallUserFeature("RSHandler1Feature");
        server.uninstallUserBundle("rsUserBundle2");
        server.uninstallUserFeature("RSHandler2Feature");
    }

    @Test
    public void testClientHandlers() throws Exception {
        
        server.startServer();
        URI uri = URI.create(TestUtils.getBaseTestUri("rs20ApplicationWithClient", "HelloRest20ClientServlet?port=" + System.getProperty("HTTP_default", "8000")));
        ClientResponse response = client.resource(uri).get();
        assertEquals("Expected result is Hello JAX-RS, actual result is" + response.getEntity(String.class), "Hello JAX-RS", response.getEntity(String.class));

        assertEquals(200, response.getStatusCode());
        assertNotNull("No RSClientOutHander message", server.waitForStringInLog("in RSClientOutHander handleMessage method"));
        assertNotNull("No RSInHandler1 message", server.waitForStringInLog("in RSInHandler1 handleMessage method"));
        assertNotNull("No RSInHandler2 message", server.waitForStringInLog("in RSInHandler2 handleMessage method"));
        assertNotNull("No RSClientInHander message", server.waitForStringInLog("in RSClientInHander handleMessage method"));
    }
}
