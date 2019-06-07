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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
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
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "TestClient1", json.getString("clientID"));
    }

    /**
     * Use the validation REST endpoint to validate a single javax.jms.ConnectionFactory, where the validation indicates a failure.
     */
    @AllowedFFDC({ "com.ibm.websphere.sib.exception.SIResourceException",
                   "com.ibm.wsspi.channelfw.exception.InvalidChainNameException",
                   "javax.jms.JMSException",
                   "javax.resource.ResourceException",
                   "javax.resource.spi.ResourceAllocationException"
    })
    @Test
    public void testJMSConnectionFactoryFailure() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/jmsConnectionFactory/jmscf2").run(JsonObject.class);
        JsonArray stack;
        String err = "Unexpected json response: " + json;
        assertEquals(err, "jmscf2", json.getString("uid"));
        assertEquals(err, "jmscf2", json.getString("id"));
        assertEquals(err, "jms/cf2", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        // Detail within the following could change if the resource adapter ever changes.
        // If so, just update the expected values accordingly.

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "CWSIA0241", json.getString("errorCode"));
        assertEquals(err, "javax.jms.JMSException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("CWSIA0241E"));
        assertNotNull(err, stack = json.getJsonArray("stack"));
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.ibm.ws.sib.api.jms."));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "com.ibm.websphere.sib.exception.SIResourceException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("CWSIT0127E"));
        assertNotNull(err, stack = json.getJsonArray("stack"));
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.ibm.ws.sib."));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        // In order to avoid testing internals too much, after this point, we will test only that the
        // chain of exceptions ends after a reasonable amount and that required attributes are non-null.

        for (int i = 2; i < 10 && (json = json.getJsonObject("cause")) != null; i++) {
            String errMsg = "Cause depth " + i + " " + err;

            assertNotNull(errMsg, json.get("class"));
            assertNotNull(errMsg, json.get("message"));
            assertNotNull(errMsg, stack = json.getJsonArray("stack"));
            assertTrue(errMsg, stack.size() > 3);
            assertNotNull(errMsg, stack.get(0));
        }

        // If the exception chain is unreasonably long, the validation impl might have missed detection of a cycle
        // or might be improperly repeating the same cause.
        assertNull(err, json);
    }

    /**
     * Use /ibm/api/validation/jmsConnectionFactory to validate all connection factories.
     */
    @AllowedFFDC({ "com.ibm.websphere.sib.exception.SIResourceException",
                   "com.ibm.wsspi.channelfw.exception.InvalidChainNameException",
                   "javax.jms.JMSException",
                   "javax.resource.ResourceException",
                   "javax.resource.spi.ResourceAllocationException"
    })
    @Test
    public void testMultipleConnectionFactories() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/jmsConnectionFactory");
        JsonArray json = request.method("GET").run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 3, json.size()); // Increase this if you add more connection factories to server.xml

        // Order is currently alphabetical based on config.displayId

        // [0]: config.displayId=DefaultJMSConnectionFactory
        JsonObject j = json.getJsonObject(0);
        assertEquals(err, "DefaultJMSConnectionFactory", j.getString("uid"));
        assertEquals(err, "DefaultJMSConnectionFactory", j.getString("id"));
        assertNull(err, j.get("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "IBM", j.getString("jmsProviderName"));
        assertEquals(err, "1.0", j.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", j.getString("jmsProviderSpecVersion"));
        assertEquals(err, "clientID", j.getString("clientID"));

        // [1]: config.displayId=jmsConnectionFactory[jmscf1]
        j = json.getJsonObject(1);
        assertEquals(err, "jmscf1", j.getString("uid"));
        assertEquals(err, "jmscf1", j.getString("id"));
        assertEquals(err, "jms/cf1", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j.getJsonObject("info"));
        // jmscf1 is already covered by testJMSConnectionFactory

        // [2]: config.displayId=jmsConnectionFactory[jmscf2]
        j = json.getJsonObject(2);
        assertEquals(err, "jmscf2", j.getString("uid"));
        assertEquals(err, "jmscf2", j.getString("id"));
        assertEquals(err, "jms/cf2", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNull(err, j.get("info"));
        assertNotNull(err, j.getJsonObject("failure"));
        // jmscf2 is already covered by testJMSConnectionFactoryFailure
    }

    /**
     * Use /ibm/api/validation/jmsQueueConnectionFactory to validate all queue connection factories, where there is only one.
     */
    @Test
    public void testMultipleQueueConnectionFactories() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/jmsQueueConnectionFactory");
        JsonArray qcfs = request.method("GET").run(JsonArray.class);
        JsonObject json;
        String err = "unexpected response: " + qcfs;

        assertEquals(err, 1, qcfs.size());
        assertNotNull(err, json = qcfs.getJsonObject(0));
        assertEquals(err, "qcf1", json.getString("uid"));
        assertEquals(err, "qcf1", json.getString("id"));
        assertEquals(err, "jms/qcf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertNull(err, json.get("clientID"));
    }

    /**
     * Use /ibm/api/validation/jmsTopicConnectionFactory to validate all 3 topic connection factories.
     */
    @Test
    public void testMultipleTopicConnectionFactories() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/jmsTopicConnectionFactory");
        JsonArray tcfs = request.method("GET").run(JsonArray.class);
        JsonObject json;
        String err = "unexpected response: " + tcfs;

        assertEquals(err, 3, tcfs.size());

        assertNotNull(err, json = tcfs.getJsonObject(0));
        assertEquals(err, "jmsTopicConnectionFactory[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "jms/tcf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "clientID", json.getString("clientID"));

        assertNotNull(err, json = tcfs.getJsonObject(1));
        assertEquals(err, "tcf2", json.getString("uid"));
        assertEquals(err, "tcf2", json.getString("id"));
        assertEquals(err, "jms/tcf2", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "tcf2id", json.getString("clientID"));

        assertNotNull(err, json = tcfs.getJsonObject(2));
        assertEquals(err, "tcf3", json.getString("uid"));
        assertEquals(err, "tcf3", json.getString("id"));
        assertNull(err, json.get("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "tcf3id", json.getString("clientID"));
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
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertNull(err, json.get("clientID"));
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
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "clientID", json.getString("clientID"));
    }
}
