/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rest.handler.validator.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateJMSTest extends FATServletClient {
    @Server("com.ibm.ws.rest.handler.validator.jms.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/

        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Use the validation REST endpoint to validate a single javax.jms.ConnectionFactory, where the validation is successful.
     */
    @Test
    public void testJMSConnectionFactory() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/jmsConnectionFactory/jmscf1").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "jmscf1", json.getString("uid"));
        assertEquals(err, "jmscf1", json.getString("id"));
        assertEquals(err, "jms/cf1", json.getString("jndiName"));
        // TODO complete this test once implementation is further along
        //assertTrue(err, json.getBoolean("successful"));
        //assertNull(err, json.get("failure"));
        //assertNotNull(err, json = json.getJsonObject("info"));
        // ...
    }

    /**
     * Use the validation REST endpoint to validate a single javax.jms.QueueConnectionFactory, where the validation is successful.
     */
    @Test
    public void testQueueConnectionFactory() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/jmsQueueConnectionFactory/qcf1").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "qcf1", json.getString("uid"));
        assertEquals(err, "qcf1", json.getString("id"));
        assertEquals(err, "jms/qcf1", json.getString("jndiName"));
        // TODO complete this test once implementation is further along
        //assertTrue(err, json.getBoolean("successful"));
        //assertNull(err, json.get("failure"));
        //assertNotNull(err, json = json.getJsonObject("info"));
        // ...
    }

    /**
     * Use the validation REST endpoint to validate a single javax.jms.TopicConnectionFactory, where the validation is successful.
     */
    @Test
    public void testTopicConnectionFactory() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/jmsTopicConnectionFactory/jmsTopicConnectionFactory[default-0]").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "jmsTopicConnectionFactory[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "jms/tcf1", json.getString("jndiName"));
        // TODO complete this test once implementation is further along
        //assertTrue(err, json.getBoolean("successful"));
        //assertNull(err, json.get("failure"));
        //assertNotNull(err, json = json.getJsonObject("info"));
        // ...
    }
}
