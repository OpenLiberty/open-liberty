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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateOpenApiSchemaTest extends FATServletClient {

    @Server("com.ibm.ws.rest.handler.validator.openapi.fat")
    public static LibertyServer server;

    private static String VERSION_REGEX = "[0-9]+\\.[0-9]+.*";

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive app = ShrinkWrap.create(WebArchive.class, "testOpenAPIApp.war")//
                        .addPackages(true, "web")//
                        .addAsManifestResource(new File("test-applications/testOpenAPIApp/resources/META-INF/openapi.yaml"));
        ShrinkHelper.exportDropinAppToServer(server, app);

        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "TestValAdapter.rar")
                        .addAsLibraries(ShrinkWrap.create(JavaArchive.class)
                                        .addPackage("org.test.validator.adapter"));
        ShrinkHelper.exportToServer(server, "dropins", rar);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, "customLoginModule.jar");
        jar.addPackage("com.ibm.ws.rest.handler.validator.loginmodule");
        ShrinkHelper.exportToServer(server, "/", jar);

        server.startServer();

        // Wait for the API to become available
        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWPKI0803A"); // CWPKI0803A: SSL certificate created in # seconds. SSL key file: ...
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I: .*openapi/platform"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKZ0005E"); // CWWKZ0005E: The server is not configured to handle the resource... TestValAdapter.rar.
                                         // This error occurs when we disable JCA in the testDisableJCAValidator test.
    }

    /**
     * Test the validation schema is available under the openapi/platform/vaidation endpoint and
     * honors the format=json parameter.
     */
    @Test
    public void testAllValidatorsAsJSON() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation?format=json");
        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        JsonObject paths = json.getJsonObject("paths");
        assertNotNull(err, paths);
        String pathsString = paths.toString();
        assertTrue(err, pathsString.contains("/validation/cloudantDatabase/"));
        assertTrue(err, pathsString.contains("/validation/cloudantDatabase/{uid}"));
        assertTrue(err, pathsString.contains("/validation/connectionFactory/"));
        assertTrue(err, pathsString.contains("/validation/connectionFactory/{uid}"));
        assertTrue(err, pathsString.contains("/validation/dataSource/"));
        assertTrue(err, pathsString.contains("/validation/dataSource/{uid}"));
        assertTrue(err, pathsString.contains("/validation/jmsConnectionFactory/"));
        assertTrue(err, pathsString.contains("/validation/jmsConnectionFactory/{uid}"));
        assertTrue(err, pathsString.contains("/validation/jmsQueueConnectionFactory/"));
        assertTrue(err, pathsString.contains("/validation/jmsQueueConnectionFactory/{uid}"));
        assertTrue(err, pathsString.contains("/validation/jmsTopicConnectionFactory/"));
        assertTrue(err, pathsString.contains("/validation/jmsTopicConnectionFactory/{uid}"));
        assertTrue(err, paths.size() == 12);
    }

    /**
     * Test the validation schema is available under the openapi/platform/vaidation endpoint and
     * is returned in YAML format by default.
     */
    @Test
    public void testAllValidatorsAsYAML() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation");
        String yaml = request.run(String.class);
        SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null, null);
        assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Paths paths = openAPI.getPaths();
        assertNotNull(paths);
        String err = "Unexpected paths response: " + Arrays.toString(paths.entrySet().toArray());
        assertTrue(err, paths.containsKey("/validation/cloudantDatabase/"));
        assertTrue(err, paths.containsKey("/validation/cloudantDatabase/{uid}"));
        assertTrue(err, paths.containsKey("/validation/connectionFactory/"));
        assertTrue(err, paths.containsKey("/validation/connectionFactory/{uid}"));
        assertTrue(err, paths.containsKey("/validation/dataSource/"));
        assertTrue(err, paths.containsKey("/validation/dataSource/{uid}"));
        assertTrue(err, paths.containsKey("/validation/jmsConnectionFactory/"));
        assertTrue(err, paths.containsKey("/validation/jmsConnectionFactory/{uid}"));
        assertTrue(err, paths.containsKey("/validation/jmsQueueConnectionFactory/"));
        assertTrue(err, paths.containsKey("/validation/jmsQueueConnectionFactory/{uid}"));
        assertTrue(err, paths.containsKey("/validation/jmsTopicConnectionFactory/"));
        assertTrue(err, paths.containsKey("/validation/jmsTopicConnectionFactory/{uid}"));
        assertTrue(err, paths.size() == 12);
    }

    /**
     * Single test method to verify that validation REST endpoint is working at all.
     */
    @Test
    public void testDefaultDataSource() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .requestProp("X-Validation-User", "dbuser1")
                        .requestProp("X-Validation-Password", "dbpwd1");
        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "DefaultDataSource", json.getString("uid"));
        assertEquals(err, "DefaultDataSource", json.getString("id"));
        assertNull(err, json.get("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertTrue(err, json.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertTrue(err, json.getString("jdbcDriverVersion").matches(VERSION_REGEX));
        assertEquals(err, "DBUSER1", json.getString("schema"));
        assertEquals(err, "dbuser1", json.getString("user"));
    }

    /**
     * Single test method to verify that JMS is set up properly in this server.
     */
    @Test
    public void testDefaultJMSConnectionFactory() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/jmsConnectionFactory/DefaultJMSConnectionFactory");
        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "DefaultJMSConnectionFactory", json.getString("uid"));
        assertEquals(err, "DefaultJMSConnectionFactory", json.getString("id"));
        assertNull(err, json.get("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "IBM", json.getString("jmsProviderName"));
        assertEquals(err, "1.0", json.getString("jmsProviderVersion"));
        assertEquals(err, "2.0", json.getString("jmsProviderSpecVersion"));
        assertEquals(err, "clientID", json.getString("clientID"));
    }

    /**
     * Test the validation OpenAPI endpoint honors the Accept header of application/json and
     * doesn't return cloudant API data when cloudant isn't enabled.
     */
    @Test
    public void testDisableCloudantValidator() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        String featureToDisable = "cloudant-1.0";
        try {
            //Disable cloudant.
            config.getFeatureManager().getFeatures().remove(featureToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);

            //Test that cloudant elements have been removed from the OpenAPI document.
            HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation");
            JsonObject json = request.requestProp("Accept", "application/json").run(JsonObject.class);
            String err = "Unexpected json response: " + json.toString();
            JsonObject paths = json.getJsonObject("paths");
            assertNotNull(err, paths);
            String pathsString = paths.toString();
            assertFalse(err, pathsString.contains("/validation/cloudantDatabase/"));
            assertFalse(err, pathsString.contains("/validation/cloudantDatabase/{uid}"));
            assertTrue(err, pathsString.contains("/validation/connectionFactory/"));
            assertTrue(err, pathsString.contains("/validation/connectionFactory/{uid}"));
            assertTrue(err, pathsString.contains("/validation/dataSource/"));
            assertTrue(err, pathsString.contains("/validation/dataSource/{uid}"));
            assertTrue(err, pathsString.contains("/validation/jmsConnectionFactory/"));
            assertTrue(err, pathsString.contains("/validation/jmsConnectionFactory/{uid}"));
            assertTrue(err, pathsString.contains("/validation/jmsQueueConnectionFactory/"));
            assertTrue(err, pathsString.contains("/validation/jmsQueueConnectionFactory/{uid}"));
            assertTrue(err, pathsString.contains("/validation/jmsTopicConnectionFactory/"));
            assertTrue(err, pathsString.contains("/validation/jmsTopicConnectionFactory/{uid}"));
            assertTrue(err, paths.size() == 10);
        } finally {
            //Re-enable cloudant.
            config.getFeatureManager().getFeatures().add(featureToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
        }
    }

    /**
     * Test the validation OpenAPI doesn't return JCA or JMS API data when JCA isn't enabled in the server.
     */
    @Test
    public void testDisableJCAValidator() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        List<String> featuresToDisable = Arrays.asList("jca-1.7", "jms-2.0", "wasJmsClient-2.0", "wasJmsServer-1.0");
        try {
            //Disable JCA (JMS 2.0 implicitly enabled it).
            config.getFeatureManager().getFeatures().removeAll(featuresToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);

            //Test that JCA and JMS elements have been removed from the OpenAPI document.
            HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation");
            String yaml = request.run(String.class);
            SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null, null);
            assertNotNull(result);
            OpenAPI openAPI = result.getOpenAPI();
            assertNotNull(openAPI);
            Paths paths = openAPI.getPaths();
            assertNotNull(paths);
            String err = "Unexpected paths response: " + Arrays.toString(paths.entrySet().toArray());
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/"));
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/{uid}"));
            assertFalse(err, paths.containsKey("/validation/connectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/connectionFactory/{uid}"));
            assertTrue(err, paths.containsKey("/validation/dataSource/"));
            assertTrue(err, paths.containsKey("/validation/dataSource/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsConnectionFactory/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsQueueConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsQueueConnectionFactory/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsTopicConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsTopicConnectionFactory/{uid}"));
            assertTrue(err, paths.size() == 4);

        } finally {
            //Re-enable disabled features.
            config.getFeatureManager().getFeatures().addAll(featuresToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
        }
    }

    /**
     * Test the validation OpenAPI doesn't return JDBC API data when JDBC isn't enabled in the server.
     */
    @Test
    public void testDisableJDBCValidator() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        String featureToDisable = "jdbc-4.2";
        try {
            //Disable JDBC.
            config.getFeatureManager().getFeatures().remove(featureToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);

            //Test that JDBC elements have been removed from the OpenAPI document.
            HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation");
            String yaml = request.run(String.class);
            SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null, null);
            assertNotNull(result);
            OpenAPI openAPI = result.getOpenAPI();
            assertNotNull(openAPI);
            Paths paths = openAPI.getPaths();
            assertNotNull(paths);
            String err = "Unexpected paths response: " + Arrays.toString(paths.entrySet().toArray());
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/"));
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/{uid}"));
            assertTrue(err, paths.containsKey("/validation/connectionFactory/"));
            assertTrue(err, paths.containsKey("/validation/connectionFactory/{uid}"));
            assertFalse(err, paths.containsKey("/validation/dataSource/"));
            assertFalse(err, paths.containsKey("/validation/dataSource/{uid}"));
            assertTrue(err, paths.containsKey("/validation/jmsConnectionFactory/"));
            assertTrue(err, paths.containsKey("/validation/jmsConnectionFactory/{uid}"));
            assertTrue(err, paths.containsKey("/validation/jmsQueueConnectionFactory/"));
            assertTrue(err, paths.containsKey("/validation/jmsQueueConnectionFactory/{uid}"));
            assertTrue(err, paths.containsKey("/validation/jmsTopicConnectionFactory/"));
            assertTrue(err, paths.containsKey("/validation/jmsTopicConnectionFactory/{uid}"));
            assertTrue(err, paths.size() == 10);

        } finally {
            //Re-enable JDBC.
            config.getFeatureManager().getFeatures().add(featureToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
        }
    }

    /**
     * Test the validation OpenAPI doesn't return JMS API data when JMS isn't enabled in the server.
     */
    @Test
    public void testDisableJMSValidator() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        List<String> featuresToDisable = Arrays.asList("jms-2.0", "wasJmsClient-2.0", "wasJmsServer-1.0");
        try {
            //Disable JMS features.
            config.getFeatureManager().getFeatures().removeAll(featuresToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);

            //Test that JMS elements have been removed from the OpenAPI document.
            HttpsRequest request = new HttpsRequest(server, "/openapi/platform/validation");
            String yaml = request.run(String.class);
            SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null, null);
            assertNotNull(result);
            OpenAPI openAPI = result.getOpenAPI();
            assertNotNull(openAPI);
            Paths paths = openAPI.getPaths();
            assertNotNull(paths);
            String err = "Unexpected paths response: " + Arrays.toString(paths.entrySet().toArray());
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/"));
            assertTrue(err, paths.containsKey("/validation/cloudantDatabase/{uid}"));
            assertTrue(err, paths.containsKey("/validation/connectionFactory/"));
            assertTrue(err, paths.containsKey("/validation/connectionFactory/{uid}"));
            assertTrue(err, paths.containsKey("/validation/dataSource/"));
            assertTrue(err, paths.containsKey("/validation/dataSource/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsConnectionFactory/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsQueueConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsQueueConnectionFactory/{uid}"));
            assertFalse(err, paths.containsKey("/validation/jmsTopicConnectionFactory/"));
            assertFalse(err, paths.containsKey("/validation/jmsTopicConnectionFactory/{uid}"));
            assertTrue(err, paths.size() == 6);

        } finally {
            //Re-enable JMS features.
            config.getFeatureManager().getFeatures().addAll(featuresToDisable);
            server.setMarkToEndOfLog();
            server.updateServerConfiguration(config);
            server.waitForConfigUpdateInLogUsingMark(null, true);
        }
    }

    /**
     * Verify that REST endpoint for validation of cloudantDatabase is reachable
     * and reports an error when the cloudantDatabase element is pointing at the wrong library.
     */
    @AllowedFFDC("java.lang.ClassNotFoundException")
    @Test
    public void testWrongLibraryForCloudant() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/cloudantDatabase/cldb");
        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "cldb", json.getString("uid"));
        assertEquals(err, "cldb", json.getString("id"));
        assertEquals(err, "cloudant/testdb", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "java.lang.ClassNotFoundException", json.getString("class"));
        assertEquals(err, "com.cloudant.client.api.ClientBuilder", json.getString("message"));
        JsonArray stack;
        assertNotNull(err, stack = json.getJsonArray("stack"));
        assertTrue(err, stack.size() > 3);
    }
}
