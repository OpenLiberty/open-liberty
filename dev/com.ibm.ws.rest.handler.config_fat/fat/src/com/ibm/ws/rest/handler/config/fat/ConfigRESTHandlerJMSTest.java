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
package com.ibm.ws.rest.handler.config.fat;

import static componenttest.annotation.SkipForRepeat.EE9_FEATURES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES) // TODO: Enable this once jms-3.0 is available
public class ConfigRESTHandlerJMSTest extends FATServletClient {
    @Server("com.ibm.ws.rest.handler.config.jms.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "TestConfigJMSAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.config.adapter")
                                        .addPackage("org.test.config.jmsadapter"));
        ShrinkHelper.exportToServer(server, "connectors", rar);

        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        messages.add("J2CA7001I: .* jmsra"); // J2CA7001I: Resource adapter tca installed in # seconds.

        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    // Test the output of the /ibm/api/config/jmsActivationSpec/{uid} REST endpoint.
    @Test
    public void testJMSActivationSpec() throws Exception {
        JsonObject aspec = new HttpsRequest(server, "/ibm/api/config/jmsActivationSpec/App1%2FEJB1%2FMessageDrivenBean1").run(JsonObject.class);
        String err = "unexpected response: " + aspec;
        assertEquals(err, "jmsActivationSpec", aspec.getString("configElementName"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean1", aspec.getString("uid"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean1", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));
        assertTrue(err, aspec.getBoolean("autoStart"));

        JsonObject authData;
        assertNotNull(err, authData = aspec.getJsonObject("authDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        // properties.jmsra (under jmsActivationSpec)
        JsonObject props;
        assertNotNull(err, props = aspec.getJsonObject("properties.jmsra"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "javax.jms.Topic", props.getString("destinationType"));

        // jmsDestination
        JsonObject dest;
        assertNotNull(err, dest = props.getJsonObject("destinationRef"));
        assertEquals(err, 5, dest.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "jmsDestination", dest.getString("configElementName"));
        assertEquals(err, "dest1", dest.getString("uid"));
        assertEquals(err, "dest1", dest.getString("id"));
        assertEquals(err, "jms/dest1", dest.getString("jndiName"));

        // properties.jmsra.JMSDestinationImpl (under jmsDestination)
        assertNotNull(err, props = dest.getJsonObject("properties.jmsra.JMSDestinationImpl"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values
        assertEquals(err, "3605 Hwy 52N, Rochester, MN 55901", props.getString("destinationName"));
    }

    // Test the output of the /ibm/api/config/jmsActivationSpec REST endpoint.
    @Test
    public void testJMSActivationSpecs() throws Exception {
        JsonArray activationSpecs = new HttpsRequest(server, "/ibm/api/config/jmsActivationSpec").run(JsonArray.class);
        String err = "unexpected response: " + activationSpecs;
        assertEquals(err, 2, activationSpecs.size());

        JsonObject aspec;
        assertNotNull(err, aspec = activationSpecs.getJsonObject(0));
        assertEquals(err, "jmsActivationSpec", aspec.getString("configElementName"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean1", aspec.getString("uid"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean1", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));
        assertTrue(err, aspec.getBoolean("autoStart"));
        // App1/EJB1/MessageDrivenBean1 is already covered by testJMSActivationSpec

        assertNotNull(err, aspec = activationSpecs.getJsonObject(1));
        assertEquals(err, "jmsActivationSpec", aspec.getString("configElementName"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean2", aspec.getString("uid"));
        assertEquals(err, "App1/EJB1/MessageDrivenBean2", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));
        assertTrue(err, aspec.getBoolean("autoStart"));
        assertNull(err, aspec.get("authDataRef"));

        // properties.jmsra (under jmsActivationSpec)
        JsonObject props;
        assertNotNull(err, props = aspec.getJsonObject("properties.jmsra"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "javax.jms.Topic", props.getString("destinationType"));

        // jmsTopic
        JsonObject dest;
        assertNotNull(err, dest = props.getJsonObject("destinationRef"));
        assertEquals(err, 5, dest.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "jmsTopic", dest.getString("configElementName"));
        assertEquals(err, "topic1", dest.getString("uid"));
        assertEquals(err, "topic1", dest.getString("id"));
        assertEquals(err, "jms/topic1", dest.getString("jndiName"));

        // properties.jmsra (under jmsTopic)
        assertNotNull(err, props = dest.getJsonObject("properties.jmsra"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values
        assertEquals(err, "What's for dinner?", props.getString("topicName"));
    }

    // Test the output of the /ibm/api/config/jmsConnectionFactory/{uid} REST endpoint.
    @Test
    public void testJMSConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsConnectionFactory/cf1").run(JsonObject.class);
        String err = "unexpected response: " + cf;
        assertEquals(err, "jmsConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf1", cf.getString("uid"));
        assertEquals(err, "cf1", cf.getString("id"));
        assertEquals(err, "jms/cf1", cf.getString("jndiName"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "jmsConnectionFactory[cf1]/connectionManager[cm]", cm.getString("uid"));
        assertEquals(err, "cm", cm.getString("id"));
        assertEquals(err, 1800, cm.getInt("maxIdleTime"));
        assertEquals(err, 14, cm.getInt("maxPoolSize"));
        assertEquals(err, 4, cm.getInt("minPoolSize"));

        JsonObject authData;
        assertNotNull(err, authData = cf.getJsonObject("containerAuthDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertNotNull(err, authData = cf.getJsonObject("recoveryAuthDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.jmsra"));
        assertEquals(err, 3, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "^", props.getString("escapeChar"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, 4455, props.getInt("portNumber"));
    }

    // Test the output of the /ibm/api/config/jmsConnectionFactory REST endpoint.
    @Test
    public void testJMSConnectionFactories() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/jmsConnectionFactory").run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(err, 2, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));
        assertEquals(err, "jmsConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "DefaultJMSConnectionFactory", cf.getString("uid"));
        assertEquals(err, "DefaultJMSConnectionFactory", cf.getString("id"));
        assertNull(err, cf.get("jndiName"));

        assertNotNull(err, cf = cfs.getJsonObject(1));
        assertEquals(err, "jmsConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf1", cf.getString("uid"));
        assertEquals(err, "cf1", cf.getString("id"));
        assertEquals(err, "jms/cf1", cf.getString("jndiName"));
        // cf1 is already tested by testJMSConnectionFactory
    }

    // Test the output of the /ibm/api/config/jmsDestination/{uid} REST endpoint.
    @Test
    public void testJMSDestination() throws Exception {
        JsonObject dest = new HttpsRequest(server, "/ibm/api/config/jmsDestination/dest1").run(JsonObject.class);
        String err = "unexpected response: " + dest;
        assertEquals(err, "jmsDestination", dest.getString("configElementName"));
        assertEquals(err, "dest1", dest.getString("uid"));
        assertEquals(err, "dest1", dest.getString("id"));
        assertEquals(err, "jms/dest1", dest.getString("jndiName"));

        JsonObject props;
        assertNotNull(err, props = dest.getJsonObject("properties.jmsra.JMSDestinationImpl"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "3605 Hwy 52N, Rochester, MN 55901", props.getString("destinationName"));
    }

    // Test the output of the /ibm/api/config/jmsDestination REST endpoint,
    // which should return all configured JMS destinations.
    @Test
    public void testJMSDestinations() throws Exception {
        JsonArray destinations = new HttpsRequest(server, "/ibm/api/config/jmsDestination").run(JsonArray.class);
        String err = "unexpected response: " + destinations;
        assertEquals(err, 2, destinations.size());

        JsonObject dest;
        assertNotNull(err, dest = destinations.getJsonObject(0));
        assertEquals(err, "jmsDestination", dest.getString("configElementName"));
        assertEquals(err, "dest1", dest.getString("uid"));
        assertEquals(err, "dest1", dest.getString("id"));
        assertEquals(err, "jms/dest1", dest.getString("jndiName"));
        // dest1 already tested by testJMSDestination

        assertNotNull(err, dest = destinations.getJsonObject(1));
        assertEquals(err, "jmsDestination", dest.getString("configElementName"));
        assertEquals(err, "dest2", dest.getString("uid"));
        assertEquals(err, "dest2", dest.getString("id"));
        assertEquals(err, "jms/dest2", dest.getString("jndiName"));
        JsonObject props;
        assertNotNull(err, props = dest.getJsonObject("properties.jmsra.JMSQueueImpl"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "201 4th St SE, Rochester, MN 55904", props.getString("destinationName"));
        assertEquals(err, "D2", props.getString("queueName"));
    }

    // Test the output of the /ibm/api/config/jmsQueue/{uid} REST endpoint.
    @Test
    public void testJMSQueue() throws Exception {
        JsonObject q = new HttpsRequest(server, "/ibm/api/config/jmsQueue/q1").run(JsonObject.class);
        String err = "unexpected response: " + q;
        assertEquals(err, "jmsQueue", q.getString("configElementName"));
        assertEquals(err, "q1", q.getString("uid"));
        assertEquals(err, "q1", q.getString("id"));
        assertEquals(err, "jms/q1", q.getString("jndiName"));

        JsonObject props;
        assertNotNull(err, props = q.getJsonObject("properties.jmsra.JMSQueueImpl"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "Q1", props.getString("queueName"));
    }

    // Test the output of the /ibm/api/config/jmsQueue REST endpoint,
    // which should return all configured JMS queues.
    @Test
    public void testJMSQueues() throws Exception {
        JsonArray queues = new HttpsRequest(server, "/ibm/api/config/jmsQueue").run(JsonArray.class);
        String err = "unexpected response: " + queues;
        assertEquals(err, 2, queues.size());

        JsonObject q;
        assertNotNull(err, q = queues.getJsonObject(0));
        assertEquals(err, "jmsQueue", q.getString("configElementName"));
        assertEquals(err, "q1", q.getString("uid"));
        assertEquals(err, "q1", q.getString("id"));
        assertEquals(err, "jms/q1", q.getString("jndiName"));
        // q1 already tested by testJMSQueue

        assertNotNull(err, q = queues.getJsonObject(1));
        assertEquals(err, "jmsQueue", q.getString("configElementName"));
        assertEquals(err, "q2", q.getString("uid"));
        assertEquals(err, "q2", q.getString("id"));
        assertEquals(err, "jms/q2", q.getString("jndiName"));
        JsonObject props;
        assertNotNull(err, props = q.getJsonObject("properties.jmsra.JMSOtherQueueImpl"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "qm", props.getString("queueManager"));
        assertEquals(err, "Q2", props.getString("queueName"));
    }

    // Test the output of the /ibm/api/config/jmsQueueConnectionFactory/{uid} REST endpoint.
    @Test
    public void testJMSQueueConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsQueueConnectionFactory/jmsQueueConnectionFactory[default-0]").run(JsonObject.class);
        String err = "unexpected response: " + cf;
        assertEquals(err, "jmsQueueConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "jmsQueueConnectionFactory[default-0]", cf.getString("uid"));
        assertNull(err, cf.get("id"));
        assertEquals(err, "jms/qcf2", cf.getString("jndiName"));

        assertNull(err, cf.get("containerAuthDataRef"));
        assertNull(err, cf.get("recoveryAuthDataRef"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.jmsra"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "localhost", props.getString("hostName"));
    }

    // Test the output of the /ibm/api/config/jmsQueueConnectionFactory REST endpoint.
    @Test
    public void testJMSQueueConnectionFactories() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/jmsQueueConnectionFactory").run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(err, 1, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));
        assertEquals(err, "jmsQueueConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "jmsQueueConnectionFactory[default-0]", cf.getString("uid"));
        assertNull(err, cf.get("id"));
        assertEquals(err, "jms/qcf2", cf.getString("jndiName"));
        // cf1 is already tested by testJMSConnectionFactory
    }

    // Test the output of the /ibm/api/config/jmsTopic/{uid} REST endpoint.
    @Test
    public void testJMSTopic() throws Exception {
        JsonObject topic = new HttpsRequest(server, "/ibm/api/config/jmsTopic/topic1").run(JsonObject.class);
        String err = "unexpected response: " + topic;
        assertEquals(err, "jmsTopic", topic.getString("configElementName"));
        assertEquals(err, "topic1", topic.getString("uid"));
        assertEquals(err, "topic1", topic.getString("id"));
        assertEquals(err, "jms/topic1", topic.getString("jndiName"));

        JsonObject props;
        assertNotNull(err, props = topic.getJsonObject("properties.jmsra"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "What's for dinner?", props.getString("topicName"));
    }

    // Test the output of the /ibm/api/config/jmsQueue REST endpoint,
    // which should return all configured JMS queues.
    @Test
    public void testJMSTopics() throws Exception {
        JsonArray topics = new HttpsRequest(server, "/ibm/api/config/jmsTopic").run(JsonArray.class);
        String err = "unexpected response: " + topics;
        assertEquals(err, 2, topics.size());

        JsonObject q;
        assertNotNull(err, q = topics.getJsonObject(0));
        assertEquals(err, "jmsTopic", q.getString("configElementName"));
        assertEquals(err, "topic1", q.getString("uid"));
        assertEquals(err, "topic1", q.getString("id"));
        assertEquals(err, "jms/topic1", q.getString("jndiName"));
        // topic1 already tested by testJMSTopic

        assertNotNull(err, q = topics.getJsonObject(1));
        assertEquals(err, "jmsTopic", q.getString("configElementName"));
        assertEquals(err, "topic2", q.getString("uid"));
        assertEquals(err, "topic2", q.getString("id"));
        assertEquals(err, "jms/topic2", q.getString("jndiName"));
        JsonObject props;
        assertNotNull(err, props = q.getJsonObject("properties.jmsra"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "Who pays for free shipping?", props.getString("topicName"));
    }

    // Test the output of the /ibm/api/config/jmsTopicConnectionFactory/{uid} REST endpoint.
    @Test
    public void testJMSTopicConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsTopicConnectionFactory/cf3").run(JsonObject.class);
        String err = "unexpected response: " + cf;
        assertEquals(err, "jmsTopicConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf3", cf.getString("uid"));
        assertEquals(err, "cf3", cf.getString("id"));
        assertEquals(err, "jms/tcf3", cf.getString("jndiName"));

        assertNull(err, cf.get("containerAuthDataRef"));
        assertNull(err, cf.get("recoveryAuthDataRef"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.jmsra"));
        assertEquals(err, 5, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "DefaultClientId", props.getString("clientId"));
        assertEquals(err, "%", props.getString("escapeChar"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, "user3", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));
    }

    // Test the output of the /ibm/api/config/jmsTopicConnectionFactory REST endpoint.
    @Test
    public void testJMSTopicConnectionFactories() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/jmsTopicConnectionFactory").run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(err, 2, cfs.size());

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));
        assertEquals(err, "jmsTopicConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf3", cf.getString("uid"));
        assertEquals(err, "cf3", cf.getString("id"));
        assertEquals(err, "jms/tcf3", cf.getString("jndiName"));
        // cf3 is already tested by testJMSTopicConnectionFactory

        assertNotNull(err, cf = cfs.getJsonObject(1));
        assertEquals(err, "jmsTopicConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf4", cf.getString("uid"));
        assertEquals(err, "cf4", cf.getString("id"));
        assertEquals(err, "jms/tcf4", cf.getString("jndiName"));

        JsonObject authData;
        assertNotNull(err, authData = cf.getJsonObject("containerAuthDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertNull(err, cf.get("recoveryAuthDataRef"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.jmsra"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "DefaultClientId", props.getString("clientId"));
        assertEquals(err, "host4.rchland.ibm.com", props.getString("hostName"));
    }
}