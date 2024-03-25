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
import static org.junit.Assert.assertNull;

import java.net.URI;

import org.apache.wink.client.ClientResponse;
import org.apache.wink.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
public class JAXRSMessageContextAPITest {
    @Server("RSTestServer")
    public static LibertyServer server = LibertyServerFactory.getLibertyServer("RSTestServer");
    private RestClient client;

    @Before
    public void setUp() throws Exception {
        client = new RestClient();
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    public void testJAXRSMessageContextAPI() throws Exception {
        //server.installUserBundle("RSMessageContextAPITestHandler_1.0.0");
        ShrinkHelper.defaultUserFeatureArchive(server, "rsUserBundle3", "com.ibm.ws.rsuserbundle3.myhandler");
        TestUtils.installUserFeature(server, "RSMessageContextAPITestFeature");
        ShrinkHelper.defaultDropinApp(server, "rsApplication", "com.ibm.samples.jaxrs");
        server.startServer();
        server.waitForStringInLog("CWWKF0011I");
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("RSMessageContextAPITest/WithBundles/server.xml");
        server.waitForStringInLog("CWWKF0012I");
        if (JakartaEEAction.isEE10OrLaterActive()) {
            assertNotNull("Expected to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
        } else {
            assertNull("Did not expect to see application rsApplication updated", server.waitForStringInLog("CWWKZ0003I: The application rsApplication updated"));
        }
        //URI uri = UriBuilder.fromUri(TestUtils.getBaseTestUri("rsApplication", "hello")).build();
        URI uri = URI.create(TestUtils.getBaseTestUri("rsApplication", "hello"));
        ClientResponse response = client.resource(uri).get();
        assertEquals(200, response.getStatusCode());
        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in RSMessageContextAPITestInHandler1:JAX_RS"));
        assertNotNull("Flow.Type should be IN", server.waitForStringInLog("Flow.Type in RSMessageContextAPITestInHandler1:IN"));
        assertNotNull("isServerSide should be true", server.waitForStringInLog("isServerSide in RSMessageContextAPITestInHandler1:true"));
        assertNotNull("isClientSide should be false", server.waitForStringInLog("isClientSide in RSMessageContextAPITestInHandler1:false"));
        assertNotNull("containTestProperty2 should be false", server.waitForStringInLog("containTestProperty2 in RSMessageContextAPITestInHandler1:false"));

        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in RSMessageContextAPITestInHandler2:JAX_RS"));
        assertNotNull("Flow.Type should be IN", server.waitForStringInLog("Flow.Type in RSMessageContextAPITestInHandler2:IN"));
        assertNotNull("isServerSide should be true", server.waitForStringInLog("isServerSide in RSMessageContextAPITestInHandler2:true"));
        assertNotNull("isClientSide should be false", server.waitForStringInLog("isClientSide in RSMessageContextAPITestInHandler2:false"));
        assertNotNull("containTestProperty2 should be false", server.waitForStringInLog("containTestProperty2 in RSMessageContextAPITestInHandler2:false"));

        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in RSMessageContextAPITestOutHandler2:JAX_RS"));
        assertNotNull("Flow.Type should be OUT", server.waitForStringInLog("Flow.Type in RSMessageContextAPITestOutHandler2:OUT"));
        assertNotNull("isServerSide should be true", server.waitForStringInLog("isServerSide in RSMessageContextAPITestOutHandler2:true"));
        assertNotNull("isClientSide should be false", server.waitForStringInLog("isClientSide in RSMessageContextAPITestOutHandler2:false"));
        assertNotNull("containTestProperty3 should be false", server.waitForStringInLog("containTestProperty3 in RSMessageContextAPITestOutHandler3:false"));

        assertNotNull("Engine.Type should be JAX-RS", server.waitForStringInLog("Engine.Type in RSMessageContextAPITestOutHandler1:JAX_RS"));
        assertNotNull("Flow.Type should be OUT", server.waitForStringInLog("Flow.Type in RSMessageContextAPITestOutHandler1:OUT"));
        assertNotNull("isServerSide should be true", server.waitForStringInLog("isServerSide in RSMessageContextAPITestOutHandler1:true"));
        assertNotNull("isClientSide should be false", server.waitForStringInLog("isClientSide in RSMessageContextAPITestOutHandler1:false"));
        assertNotNull("containTestProperty4 should be false", server.waitForStringInLog("containTestProperty4 in RSMessageContextAPITestOutHandler1:false"));
    }
}