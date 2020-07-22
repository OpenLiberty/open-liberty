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
@SkipForRepeat(EE9_FEATURES) // TODO: Enable this once jca-2.0 is available
public class ConfigRESTHandlerJCATest extends FATServletClient {
    @Server("com.ibm.ws.rest.handler.config.jca.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ResourceAdapterArchive tca_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "TestConfigAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.config.adapter"));
        ShrinkHelper.exportToServer(server, "connectors", tca_rar);

        ResourceAdapterArchive ata_rar = ShrinkWrap.create(ResourceAdapterArchive.class, "AnotherTestAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.config.adapter"));
        ShrinkHelper.exportToServer(server, "connectors", ata_rar);

        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        messages.add("J2CA7001I: .* tca"); // J2CA7001I: Resource adapter tca installed in # seconds.
        messages.add("J2CA7001I: .* AnotherTestAdapter"); // J2CA7001I: Resource adapter AnotherTestAdapter installed in # seconds.

        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    // Test the output of the /ibm/api/config/connectionFactory/{uid} REST endpoint.
    @Test
    public void testConfigConnectionFactory() throws Exception {
        JsonObject cf = new HttpsRequest(server, "/ibm/api/config/connectionFactory/cf1").run(JsonObject.class);
        String err = "unexpected response: " + cf;
        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf1", cf.getString("uid"));
        assertEquals(err, "cf1", cf.getString("id"));
        assertEquals(err, "eis/cf1", cf.getString("jndiName"));

        JsonObject cm;
        assertNotNull(err, cm = cf.getJsonObject("connectionManagerRef"));
        assertEquals(err, "connectionManager", cm.getString("configElementName"));
        assertEquals(err, "cm1", cm.getString("uid"));
        assertEquals(err, "cm1", cm.getString("id"));
        assertEquals(err, 101, cm.getInt("maxPoolSize"));
        assertEquals(err, "EntirePool", cm.getString("purgePolicy"));

        JsonObject authData;
        assertNotNull(err, authData = cf.getJsonObject("containerAuthDataRef"));
        assertEquals(err, "containerAuthData", authData.getString("configElementName"));
        assertEquals(err, "connectionFactory[cf1]/containerAuthData[default-0]", authData.getString("uid"));
        assertNull(err, authData.getJsonObject("id"));
        assertEquals(err, "containerUser1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        assertNotNull(err, authData = cf.getJsonObject("recoveryAuthDataRef"));
        assertEquals(err, "recoveryAuthData", authData.getString("configElementName"));
        assertEquals(err, "connectionFactory[cf1]/recoveryAuthData[default-0]", authData.getString("uid"));
        assertNull(err, authData.getJsonObject("id"));
        assertEquals(err, "recoveryUser1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.tca.ConnectionFactory"));
        assertEquals(err, 3, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, true, props.getBoolean("enableBetaContent"));
        assertEquals(err, "localhost", props.getString("hostName"));
        assertEquals(err, 7766, props.getInt("portNumber"));
    }

    // Test the output of the /ibm/api/config/activationSpec REST endpoint.
    @Test
    public void testMultipleActivationSpecs() throws Exception {
        JsonArray aspecs = new HttpsRequest(server, "/ibm/api/config/activationSpec").run(JsonArray.class);
        String err = "unexpected response: " + aspecs;
        assertEquals(err, 2, aspecs.size()); // increase this if additional activationSpec elements are added

        JsonObject aspec;
        assertNotNull(err, aspec = aspecs.getJsonObject(0));
        assertEquals(err, "activationSpec", aspec.getString("configElementName"));
        assertEquals(err, "App1/EJB1/MyMDB", aspec.getString("uid"));
        assertEquals(err, "App1/EJB1/MyMDB", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));
        // App1/EJB1/MyMDB is already tested by testSingleActivationSpec

        assertNotNull(err, aspec = aspecs.getJsonObject(1));
        assertEquals(err, "activationSpec", aspec.getString("configElementName"));
        assertEquals(err, "MyDefaultMessageListener", aspec.getString("uid"));
        assertEquals(err, "MyDefaultMessageListener", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));
        assertNull(err, aspec.get("containerAuthDataRef"));
        assertNull(err, aspec.get("recoveryAuthDataRef"));
        JsonObject props;
        assertNotNull(err, props = aspec.getJsonObject("properties.tca"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, 8, props.getInt("minSize"));
        assertEquals(err, 1.618, props.getJsonNumber("multiplicationFactor").doubleValue(), 0.0001);
    }

    // Test the output of the /ibm/api/config/adminObject REST endpoint.
    @Test
    public void testMultipleAdminObjects() throws Exception {
        JsonArray adminObjects = new HttpsRequest(server, "/ibm/api/config/adminObject").run(JsonArray.class);
        String err = "unexpected response: " + adminObjects;
        assertEquals(err, 3, adminObjects.size()); // increase this if additional adminObject elements are added

        JsonObject conspec;
        assertNotNull(err, conspec = adminObjects.getJsonObject(0));
        assertEquals(err, "adminObject", conspec.getString("configElementName"));
        assertEquals(err, "conspec1", conspec.getString("uid"));
        assertEquals(err, "conspec1", conspec.getString("id"));
        assertEquals(err, "eis/conspec1", conspec.getString("jndiName"));
        JsonObject props;
        assertNotNull(err, props = conspec.getJsonObject("properties.tca.ConnectionSpec"));
        assertEquals(err, 4, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, 10000, props.getInt("connectionTimeout"));
        assertEquals(err, false, props.getBoolean("readOnly"));
        assertEquals(err, "1user", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));

        assertNotNull(err, conspec = adminObjects.getJsonObject(1));
        assertEquals(err, "adminObject", conspec.getString("configElementName"));
        assertEquals(err, "conspec2", conspec.getString("uid"));
        assertEquals(err, "conspec2", conspec.getString("id"));
        assertEquals(err, "eis/conspec2", conspec.getString("jndiName"));
        assertNull(err, conspec.get("properties.tca.ConnectionSpec"));

        assertNotNull(err, conspec = adminObjects.getJsonObject(2));
        assertEquals(err, "adminObject", conspec.getString("configElementName"));
        assertEquals(err, "adminObject[default-0]", conspec.getString("uid"));
        assertNull(err, conspec.get("id"));
        assertEquals(err, "eis/conspec3", conspec.getString("jndiName"));
        assertNotNull(err, props = conspec.getJsonObject("properties.tca.ConnectionSpec"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, false, props.getBoolean("readOnly"));
    }

    // Test the output of the /ibm/api/config/connectionFactory REST endpoint.
    @Test
    public void testMultipleConnectionFactories() throws Exception {
        JsonArray cfs = new HttpsRequest(server, "/ibm/api/config/connectionFactory").run(JsonArray.class);
        String err = "unexpected response: " + cfs;
        assertEquals(err, 3, cfs.size()); // increase this if additional connectionFactory elements are added

        JsonObject cf;
        assertNotNull(err, cf = cfs.getJsonObject(0));
        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf1", cf.getString("uid"));
        assertEquals(err, "cf1", cf.getString("id"));
        assertEquals(err, "eis/cf1", cf.getString("jndiName"));
        // cf1 is already tested by testConfigConnectionFactory

        assertNotNull(err, cf = cfs.getJsonObject(1));
        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "cf3", cf.getString("uid"));
        assertEquals(err, "cf3", cf.getString("id"));
        assertEquals(err, "eis/cf3", cf.getString("jndiName"));
        JsonObject props;
        assertNotNull(err, props = cf.getJsonObject("properties.AnotherTestAdapter.ConnectionFactory"));
        assertEquals(err, 2, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, false, props.getBoolean("enableBetaContent"));
        assertEquals(err, "localhost", props.getString("hostName"));

        assertNotNull(err, cf = cfs.getJsonObject(2));
        assertEquals(err, "connectionFactory", cf.getString("configElementName"));
        assertEquals(err, "connectionFactory[default-0]", cf.getString("uid"));
        assertNull(err, cf.get("id"));
        assertEquals(err, "eis/ds2", cf.getString("jndiName"));

        assertNull(err, cf.get("connectionManagerRef"));
        assertNull(err, cf.get("containerAuthDataRef"));
        assertNull(err, cf.get("recoveryAuthDataRef"));

        assertNotNull(err, props = cf.getJsonObject("properties.tca.DataSource"));
        assertEquals(err, 4, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, "$", props.getString("escapeChar"));
        assertEquals(err, "localhost", props.getString("hostName"));
        // assertEquals(err, 7654, props.getInt("portNumber")); // TODO include the internal default for portNumber?
        assertEquals(err, "user2", props.getString("userName"));
        assertEquals(err, "******", props.getString("password"));
    }

    // Test the output of the /ibm/api/config/resourceAdapter/{uid} REST endpoint.
    @Test
    public void testConfigResourceAdapter() throws Exception {
        JsonObject adapter = new HttpsRequest(server, "/ibm/api/config/resourceAdapter/tca").run(JsonObject.class);
        String err = "unexpected response: " + adapter;
        assertEquals(err, "resourceAdapter", adapter.getString("configElementName"));
        assertEquals(err, "tca", adapter.getString("uid"));
        assertEquals(err, "tca", adapter.getString("id"));
        assertTrue(err, adapter.getString("location").endsWith("TestConfigAdapter.rar"));

        JsonObject props;
        assertNotNull(err, props = adapter.getJsonObject("properties.tca"));
        assertEquals(err, true, props.getBoolean("debugMode"));
        assertEquals(err, "host1.openliberty.io", props.getString("hostName"));
    }

    // Test the output of the /ibm/api/config/properties.{id of resourceAdapter}/{config display id} REST endpoint
    // Test the output of the /ibm/api/config/properties.{generated identifier for resourceAdapter}/{config display id} REST endpoint
    @Test
    public void testConfigResourceAdapterPropertiesByIdentifier() throws Exception {
        JsonObject props = new HttpsRequest(server, "/ibm/api/config/properties.tca/resourceAdapter[tca]%2Fproperties.tca[tca]").run(JsonObject.class);
        String err = "unexpected response: " + props;
        assertEquals(err, "properties.tca", props.getString("configElementName"));
        assertEquals(err, "resourceAdapter[tca]/properties.tca[tca]", props.getString("uid"));
        assertNull(err, props.get("id"));
        assertEquals(err, true, props.getBoolean("debugMode"));
        assertEquals(err, "host1.openliberty.io", props.getString("hostName"));

        props = new HttpsRequest(server, "/ibm/api/config/properties.AnotherTestAdapter/resourceAdapter[default-0]%2Fproperties.AnotherTestAdapter[AnotherTestAdapter]")
                        .run(JsonObject.class);
        err = "unexpected response: " + props;
        assertEquals(err, "properties.AnotherTestAdapter", props.getString("configElementName"));
        assertEquals(err, "resourceAdapter[default-0]/properties.AnotherTestAdapter[AnotherTestAdapter]", props.getString("uid"));
        assertNull(err, props.get("id"));
        assertNull(err, props.get("debugMode"));
        assertEquals(err, "host1.openliberty.io", props.getString("hostName"));
    }

    // Test the output of the /ibm/api/config/properties.{generated identifier for resourceAdapter}
    @Test
    public void testConfigResourceAdapterPropertiesFromDefaultInstance() throws Exception {
        JsonArray array = new HttpsRequest(server, "/ibm/api/config/properties.AnotherTestAdapter").run(JsonArray.class);
        String err = "unexpected response: " + array;
        assertEquals(err, 1, array.size());
        JsonObject props;
        assertNotNull(err, props = array.getJsonObject(0));
        assertEquals(err, "properties.AnotherTestAdapter", props.getString("configElementName"));
        assertEquals(err, "resourceAdapter[default-0]/properties.AnotherTestAdapter[AnotherTestAdapter]", props.getString("uid"));
        assertNull(err, props.get("id"));
        assertNull(err, props.get("debugMode"));
        assertEquals(err, "host1.openliberty.io", props.getString("hostName"));
    }

    // Test the output of the /ibm/api/config/resourceAdapter/{config display id} REST endpoint.
    @Test
    public void testConfigResourceAdapterWithoutId() throws Exception {
        JsonObject adapter = new HttpsRequest(server, "/ibm/api/config/resourceAdapter/resourceAdapter[default-0]").run(JsonObject.class);
        String err = "unexpected response: " + adapter;
        assertEquals(err, "resourceAdapter", adapter.getString("configElementName"));
        assertEquals(err, "resourceAdapter[default-0]", adapter.getString("uid"));
        assertNull(err, adapter.get("id"));
        assertTrue(err, adapter.getString("location").endsWith("AnotherTestAdapter.rar"));

        JsonObject props;
        assertNotNull(err, props = adapter.getJsonObject("properties.AnotherTestAdapter"));
        assertNull(err, props.get("debugMode"));
        assertEquals(err, "host1.openliberty.io", props.getString("hostName"));
    }

    // Test the output of the /ibm/api/config/activationSpec/{uid} REST endpoint.
    @Test
    public void testSingleActivationSpec() throws Exception {
        JsonObject aspec = new HttpsRequest(server, "/ibm/api/config/activationSpec/App1%2FEJB1%2FMyMDB").run(JsonObject.class);
        String err = "unexpected response: " + aspec;

        assertEquals(err, "activationSpec", aspec.getString("configElementName"));
        assertEquals(err, "App1/EJB1/MyMDB", aspec.getString("uid"));
        assertEquals(err, "App1/EJB1/MyMDB", aspec.getString("id"));
        assertNull(err, aspec.get("jndiName"));

        JsonObject authData;
        assertNotNull(err, authData = aspec.getJsonObject("authDataRef"));
        assertEquals(err, "authData", authData.getString("configElementName"));
        assertEquals(err, "activationSpec[App1/EJB1/MyMDB]/authData[default-0]", authData.getString("uid"));
        assertNull(err, authData.getJsonObject("id"));
        assertEquals(err, "actspecU1", authData.getString("user"));
        assertEquals(err, "******", authData.getString("password"));

        JsonObject props;
        assertNotNull(err, props = aspec.getJsonObject("properties.tca"));
        assertEquals(err, 4, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, 8192, props.getInt("maxSize"));
        assertEquals(err, "*", props.getString("messageSelector"));
        assertEquals(err, 8, props.getInt("minSize"));
        assertEquals(err, 1.618, props.getJsonNumber("multiplicationFactor").doubleValue(), 0.0001);
    }

    // Test the output of the /ibm/api/config/adminObject/{uid} REST endpoint.
    @Test
    public void testSingleAdminObject() throws Exception {
        JsonObject conspec = new HttpsRequest(server, "/ibm/api/config/adminObject/adminObject[default-0]").run(JsonObject.class);
        String err = "unexpected response: " + conspec;

        assertEquals(err, "adminObject", conspec.getString("configElementName"));
        assertEquals(err, "adminObject[default-0]", conspec.getString("uid"));
        assertNull(err, conspec.get("id"));
        assertEquals(err, "eis/conspec3", conspec.getString("jndiName"));

        JsonObject props;
        assertNotNull(err, props = conspec.getJsonObject("properties.tca.ConnectionSpec"));
        assertEquals(err, 1, props.size()); // increase this if we ever add additional configured values or default values
        assertEquals(err, false, props.getBoolean("readOnly"));
    }
}