/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webservices.handler.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.topology.utils.HttpUtils;

@RunWith(FATRunner.class)
public class HandlerRuleTest {
    @Server("HandlerRuleTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("HandlerRuleTestServer");
    private RestClient client;

    @Before
    public void setUp() throws Exception {
        client = new RestClient();

        ShrinkHelper.defaultUserFeatureArchive(server, "handlerRuleBundle", "com.ibm.ws.handlerrulebundle.myhandler");
        TestUtils.installUserFeature(server, "TestHandlerRuleFeature");
        ShrinkHelper.defaultUserFeatureArchive(server, "handlerRuleBundle2", "com.ibm.ws.handlerrulebundle2.myhandler");
        TestUtils.installUserFeature(server, "TestHandlerRuleFeature2");
        
        ShrinkHelper.defaultDropinApp(server, "rsApplication", "com.ibm.samples.jaxrs");
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testJAXRSHandlerRule() throws Exception {
        
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");
        server.setMarkToEndOfLog();
        server.waitForStringInLog("CWWKF0012I");
        //URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri("rsApplication", "hello")).build();
        URI uri = URI.create(TestUtils.getBaseTestUri("rsApplication", "hello"));
        ClientResponse response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in InOutHandlerBoth:JAX_RS"));
        assertNotNull("Flow.Type should be IN", server.waitForStringInLog("Flow.Type in InOutHandlerBoth:IN"));
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in InOutHandlerBoth2:JAX_RS"));
        assertNotNull("Flow.Type should be IN", server.waitForStringInLog("Flow.Type in InOutHandlerBoth2:IN"));
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in InOutHandlerBoth2:JAX_RS"));
        assertNotNull("Flow.Type should be OUT", server.waitForStringInLog("Flow.Type in InOutHandlerBoth2:OUT"));
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in InOutHandlerBoth:JAX_RS"));
        assertNotNull("Flow.Type should be OUT", server.waitForStringInLog("Flow.Type in InOutHandlerBoth:OUT"));
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in OutHandlerRS:JAX_RS"));
        assertNotNull("Flow.Type should be OUT", server.waitForStringInLog("Flow.Type in OutHandlerRS:OUT"));

    }
}
