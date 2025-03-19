/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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
package com.ibm.ws.rest.handler.validator.cloudant.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.DockerImageName;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateCloudantTest extends FATServletClient {

    @Server("com.ibm.ws.rest.handler.validator.cloudant.fat")
    public static LibertyServer server;

    private static final Class<?> c = ValidateCloudantTest.class;

    private static final String CLOUDANT_DB_NAME = "testdb";
    private static final String CLOUDANT_USER = "admin";
    private static final String CLOUDANT_PASS = "pass";
    private static String CLOUDANT_URL;
    private static String databaseURI;

    //TODO Start using ImageBuilder
//    private static final DockerImageName CLOUDANT_DEV = ImageBuilder.build("cloudant-dev:2.0.1").getDockerImageName();

    private static final DockerImageName CLOUDANT_DEV = DockerImageName.parse("kyleaure/cloudant-developer:1.0");

    @ClassRule //FIXME the cloudant-developer image is deprecated consider using CouchDB
    public static GenericContainer<?> cloudant = new GenericContainer<>(CLOUDANT_DEV)
                    .withExposedPorts(5984)
                    .withLogConsumer(ValidateCloudantTest::log);

    @BeforeClass
    public static void setUp() throws Exception {
        String host = cloudant.getHost();
        String port = String.valueOf(cloudant.getMappedPort(5984));
        Log.info(c, "setUp", "Using Cloudant properties: host=" + host + "  port=" + port);
        CLOUDANT_URL = "http://" + host + ":" + port;
        databaseURI = CLOUDANT_URL + "/" + CLOUDANT_DB_NAME;
        server.addEnvVar("CLOUDANT_HOST", host);
        server.addEnvVar("CLOUDANT_PORT", port);
        server.addEnvVar("CLOUDANT_DB_NAME", CLOUDANT_DB_NAME);
        server.addEnvVar("CLOUDANT_USER", CLOUDANT_USER);
        server.addEnvVar("CLOUDANT_PASS", CLOUDANT_PASS);
        server.addEnvVar("CLOUDANT_URL", CLOUDANT_URL);

        server.startServer();

        List<String> messages = new ArrayList<>();
        messages.add("CWWKS0008I"); // CWWKS0008I: The security service is ready.
        messages.add("CWWKS4105I"); // CWWKS4105I: LTPA configuration is ready after # seconds.
        messages.add("CWWKO0219I: .* defaultHttpEndpoint-ssl"); // CWWKO0219I: TCP Channel defaultHttpEndpoint-ssl has been started and is now listening for requests on host *  (IPv6) port 8020.
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKS1300E"); //Auth alias doesn't exist
    }

    private static void log(OutputFrame frame) {
        String msg = frame.getUtf8String();
        if (msg.endsWith("\n"))
            msg = msg.substring(0, msg.length() - 1);
        Log.info(c, "cloudantdb", msg);
    }

    /*
     * Test that a cloudant database where the username and password are supplied as properties
     * on the referenced cloudant element is able to successfully connect.
     */
    @Test
    public void testCloudantDBProps() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbAuthProps");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertSuccess(json, "dbAuthProps", err);
    }

    /*
     * Test that a user and password supplied through container auth override any that are specified
     * as properties in server config. In this case an auth alias with invalid parameters are supplied
     * so the validation fails.
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBPropsOverride() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbAuthProps?auth=container&authAlias=invalidAuthData");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbAuthProps", json.getString("uid"));
        assertEquals(err, "dbAuthProps", json.getString("id"));
        assertEquals(err, "cloudant/dbAuthProps", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that when no user and password are supplied through any means the validation fails.
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBNoAuth() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/cloudantDatabase%5Bdefault-0%5D");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "cloudantDatabase[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "cloudant/dbNoAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that a cloudantDatabase with a container auth alias specified passes when auth=Container is
     * specified.
     */
    @Test
    public void testCloudantDBContainerAuth() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth?auth=container");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertSuccess(json, "dbCtrAuth", err);
    }

    /*
     * Test that a cloudantDatabase with a container auth alias specified will use a different
     * auth alias if one is supplied as a parameter. In this case the auth alias specified has
     * invalid credentials so the validation fails.
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBContainerAuthOverride() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth?auth=container&authAlias=invalidAuthData");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbCtrAuth", json.getString("uid"));
        assertEquals(err, "dbCtrAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbCtrAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that if no auth type is specified on a cloudantDatabase which has a containerAuthAlias
     * specified then the validation will fail.
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBContainerAuthNoAuthSpecified() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbCtrAuth", json.getString("uid"));
        assertEquals(err, "dbCtrAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbCtrAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that validation of a cloudantDatabase which doesn't exist fails appropriately.
     */
    @Test
    public void testCloudantDBNotConfigured() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/doesntExist");
        String response = request.expectCode(404).run(String.class);
        String err = "unexpected response: " + response;
        assertTrue(err, response.contains("CWWKO1500E") && response.contains("cloudantDatabase") && response.contains("doesntExist"));

    }

    /*
     * Test that validation fails appropriately when an auth alias that doesn't exist is specified
     * as a parameter.
     */
    @Test
    @ExpectedFFDC(value = { "javax.security.auth.login.LoginException" })
    public void testCloudantDBInvalidAuthAlais() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth?auth=container&authAlias=doesntExist");
        JsonObject json = request.run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "dbCtrAuth", json.getString("uid"));
        assertEquals(err, "dbCtrAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbCtrAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "javax.security.auth.login.LoginException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("CWWKS1300E"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
    }

    /*
     * Test that the container auth alias is not supplied when auth=Application is specified as a parameter
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBApplicationAuth() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth?auth=Application");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbCtrAuth", json.getString("uid"));
        assertEquals(err, "dbCtrAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbCtrAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that an auth alias supplied as a parameter is not used if auth = Application is supplied.
     */
    @Test
    @ExpectedFFDC({ "com.cloudant.client.org.lightcouch.CouchDbException" })
    public void testCloudantDBApplicationAuthWithAlias() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCtrAuth?auth=Application&authAlias=cloudantAuthData");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbCtrAuth", json.getString("uid"));
        assertEquals(err, "dbCtrAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbCtrAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Unauthorized"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    /*
     * Test that in an older version of the cloudant client before the metaInformation method was added,
     * the server version is obtained instead using the serverVersion method
     */
    @Test
    public void testCloudantDBOldClient() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbCloudantOld?auth=container");

        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();

        assertEquals(err, "dbCloudantOld", json.getString("uid"));
        assertEquals(err, "dbCloudantOld", json.getString("id"));
        assertEquals(err, "cloudant/dbCloudantOld", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, databaseURI, json.getString("uri"));
        assertEquals(err, "2.0.0", json.getString("serverVersion"));
        assertNull(err, json.get("vendorName"));
        assertNull(err, json.get("vendorVersion"));
        assertNull(err, json.get("vendorVariant"));
    }

    /*
     * Test that cloudant resources cannot be validated
     */
    @Test
    public void testCantValidateCloudant() throws Exception {
        JsonObject json = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudant/cloudantNoAuth").run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "cloudantNoAuth", json.getString("uid"));
        assertEquals(err, "cloudantNoAuth", json.getString("id"));
        assertEquals(err, "cloudant/cloudantNoAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertTrue(err, json.getString("message").contains("validate"));
    }

    /*
     * Test that mutliple cloudantDatabase resources can be validated through the endpoint when a specific
     * cloudantDatabase is not specified. Uses container auth with a specified authAlais.
     */
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testMultiple() throws Exception {
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase?auth=container&authAlias=cloudantAuthData");
        JsonArray json = request.run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 6, json.size()); // Increase this if you add more cloudant databases to server.xml

        // Order is currently alphabetical based on config.displayId

        // [0]: cloudantDatabase[dbAuthProps]
        JsonObject j = json.getJsonObject(0);
        assertSuccess(j, "dbAuthProps", err);

        // [1]: cloudantDatabase[dbCloudantOld]
        j = json.getJsonObject(1);
        assertEquals(err, "dbCloudantOld", j.getString("uid"));
        assertEquals(err, "dbCloudantOld", j.getString("id"));
        assertEquals(err, "cloudant/dbCloudantOld", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, databaseURI, j.getString("uri"));
        assertEquals(err, "2.0.0", j.getString("serverVersion"));
        assertNull(err, j.get("vendorName"));
        assertNull(err, j.get("vendorVersion"));
        assertNull(err, j.get("vendorVariant"));

        // [2]: cloudantDatabase[dbCtrAuth]
        j = json.getJsonObject(2);
        assertSuccess(j, "dbCtrAuth", err);

        // [3]: cloudantDatabase[dbIncorrectName]
        j = json.getJsonObject(3);
        assertEquals(err, "dbIncorrectName", j.getString("uid"));
        assertEquals(err, "dbIncorrectName", j.getString("id"));
        assertEquals(err, "cloudant/dbIncorrectName", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNotNull(err, j = j.getJsonObject("failure"));
        assertEquals(err, "java.lang.reflect.InvocationTargetException", j.getString("class"));
        JsonArray stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertNotNull(err, j = j.getJsonObject("cause"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.NoDocumentException", j.getString("class"));
        assertTrue(err, j.getString("message").contains("invalidDBName"));
        stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));

        // [4]: cloudantDatabase[dbTestAuth]
        j = json.getJsonObject(4);
        assertEquals(err, "dbTestAuth", j.getString("uid"));
        assertEquals(err, "dbTestAuth", j.getString("id"));
        assertEquals(err, "cloudant/dbTestAuth", j.getString("jndiName"));
        //The result will vary based on test order, so not checking here

        // [5]: cloudantDatabase[default-0]
        j = json.getJsonObject(5);
        assertEquals(err, "cloudantDatabase[default-0]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertEquals(err, "cloudant/dbNoAuth", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, databaseURI, j.getString("uri"));
    }

    /*
     * Test that a non-admin user is able to successfully validate to a database for which they
     * are a member and cannot successfully validate to a database for which they are not a member.
     */
    @Test
    @ExpectedFFDC({ "java.lang.reflect.InvocationTargetException" })
    public void testNonAdminUser() throws Exception {
        //Add a new user travis to the db
        URL url = new URL(CLOUDANT_URL + "/_users/org.couchdb.user:travis");
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("PUT");
        http.setRequestProperty("Authorization", "Basic YWRtaW46cGFzcw=="); //Base64 encoded admin:pass
        http.setDoOutput(true);

        try (OutputStreamWriter os = new OutputStreamWriter(http.getOutputStream())) {
            os.write("{\"name\":\"travis\",\"password\":\"password\",\"roles\":[],\"type\":\"user\"}");
        }

        assertEquals("Unexpected response received from cloudant: " + getResponse(http), 201, http.getResponseCode());

        //Add a new user kevin to the db
        url = new URL(CLOUDANT_URL + "/_users/org.couchdb.user:kevin");
        con = url.openConnection();
        http = (HttpURLConnection) con;
        http.setRequestMethod("PUT");
        http.setRequestProperty("Authorization", "Basic YWRtaW46cGFzcw=="); //Base64 encoded admin:pass
        http.setDoOutput(true);

        try (OutputStreamWriter os = new OutputStreamWriter(http.getOutputStream())) {
            os.write("{\"name\":\"kevin\",\"password\":\"password\",\"roles\":[],\"type\":\"user\"}");
        }

        assertEquals("Unexpected response received from cloudant: " + getResponse(http), 201, http.getResponseCode());

        //Create a new database testauthdb
        url = new URL(CLOUDANT_URL + "/testauthdb");
        con = url.openConnection();
        http = (HttpURLConnection) con;

        http.setRequestMethod("PUT");
        http.setRequestProperty("Authorization", "Basic YWRtaW46cGFzcw==");
        assertEquals("Unexpected response received from cloudant: " + getResponse(http), 201, http.getResponseCode());

        //Add the user travis as a member of the testauthdb database
        url = new URL(CLOUDANT_URL + "/testauthdb/_security");
        con = url.openConnection();
        http = (HttpURLConnection) con;

        http.setRequestMethod("PUT");
        http.setRequestProperty("Authorization", "Basic YWRtaW46cGFzcw=="); //Base64 encoded admin:pass
        http.setDoOutput(true);

        try (OutputStreamWriter os = new OutputStreamWriter(http.getOutputStream())) {
            os.write("{\"admins\": { \"names\": [], \"roles\": [] }, \"members\": { \"names\": [\"travis\"], \"roles\": [] } }");
        }

        assertEquals("Unexpected response received from database: " + getResponse(http), 200, http.getResponseCode());

        //Test that validation succeeds for travis and testauthdb
        HttpsRequest request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbTestAuth?auth=container&authAlias=travisAuthData");
        JsonObject json = request.run(JsonObject.class);

        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbTestAuth", json.getString("uid"));
        assertEquals(err, "dbTestAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbTestAuth", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, CLOUDANT_URL + "/testauthdb", json.getString("uri"));
        assertEquals(err, "2.0.0", json.getString("serverVersion"));
        assertEquals(err, "IBM Cloudant", json.getString("vendorName"));
        assertEquals(err, "1.1.0", json.getString("vendorVersion"));
        assertEquals(err, "local", json.getString("vendorVariant"));

        //Test that validation fails for kevin and testauthdb
        request = FATSuite.createHttpsRequestWithAdminUser(server, "/ibm/api/validation/cloudantDatabase/dbTestAuth?auth=container&authAlias=kevinAuthData");
        json = request.run(JsonObject.class);

        err = "Unexpected json response: " + json.toString();
        assertEquals(err, "dbTestAuth", json.getString("uid"));
        assertEquals(err, "dbTestAuth", json.getString("id"));
        assertEquals(err, "cloudant/dbTestAuth", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "java.lang.reflect.InvocationTargetException", json.getString("class"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "com.cloudant.client.org.lightcouch.CouchDbException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Forbidden"));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com.cloudant.client"));
    }

    private static String getResponse(HttpURLConnection http) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = http.getInputStream()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (br.ready()) {
                sb.append(br.readLine() + System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static void assertSuccess(JsonObject json, String expectedID, String err) {
        assertEquals(err, expectedID, json.getString("uid"));
        assertEquals(err, expectedID, json.getString("id"));
        assertEquals(err, "cloudant/" + expectedID, json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, databaseURI, json.getString("uri"));
        assertEquals(err, "2.0.0", json.getString("serverVersion"));
        assertEquals(err, "IBM Cloudant", json.getString("vendorName"));
        assertEquals(err, "1.1.0", json.getString("vendorVersion"));
        assertEquals(err, "local", json.getString("vendorVariant"));
    }

}
