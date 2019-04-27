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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
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

    // Test the output of the /ibm/api/config/jmsConnectionFactory/{uid} REST endpoint.
    @Test
    public void testJMSConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/jmsConnectionFactory/cf1").run(JsonObject.class);
        String err = "unexpected response: " + cf;
        assertEquals(err, "jmsConnectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf1", cf.getString("uid"));
        assertEquals(err, "cf1", cf.getString("id"));
        assertEquals(err, "jms/cf1", cf.getString("jndiName"));

        JsonArray array;
        JsonObject cm;
        assertNotNull(err, array = cf.getJsonArray("connectionManagerRef"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, cm = array.getJsonObject(0));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "jmsConnectionFactory[cf1]/connectionManager[cm]", cm.getString("uid"));
        assertEquals(err, "cm", cm.getString("id"));
        assertEquals(err, 1800, cm.getInt("maxIdleTime"));
        assertEquals(err, 14, cm.getInt("maxPoolSize"));
        assertEquals(err, 4, cm.getInt("minPoolSize"));

        JsonObject authData;
        assertNotNull(err, array = cf.getJsonArray("containerAuthDataRef"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, authData = array.getJsonObject(0));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertNotNull(err, array = cf.getJsonArray("recoveryAuthDataRef"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, authData = array.getJsonObject(0));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        JsonObject props;
        assertNotNull(err, array = cf.getJsonArray("properties.jmsra"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, props = array.getJsonObject(0));
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

        JsonArray array;
        JsonObject props;
        assertNotNull(err, array = cf.getJsonArray("properties.jmsra"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, props = array.getJsonObject(0));
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

        JsonArray array;
        JsonObject props;
        assertNotNull(err, array = cf.getJsonArray("properties.jmsra"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, props = array.getJsonObject(0));
        assertEquals(err, 4, props.size()); // increase this if we ever add additional configured values or default values
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
        JsonArray array;
        assertNotNull(err, array = cf.getJsonArray("containerAuthDataRef"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, authData = array.getJsonObject(0));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "jmsUser1", authData.getString("uid"));
        assertEquals(err, "jmsUser1", authData.getString("id"));
        assertEquals(err, "jmsU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertNull(err, cf.get("recoveryAuthDataRef"));

        JsonObject props;
        assertNotNull(err, array = cf.getJsonArray("properties.jmsra"));
        assertEquals(err, 1, array.size());
        assertNotNull(err, props = array.getJsonObject(0));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "host4.rchland.ibm.com", props.getString("hostName"));
    }
}