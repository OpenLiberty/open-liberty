/*******************************************************************************
 * Copyright (c) 2017,2019 IBM Corporation and others.
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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateDataSourceTest extends FATServletClient {

    private static final Class<?> c = ValidateDataSourceTest.class;

    @Server("com.ibm.ws.rest.handler.validator.jdbc.fat")
    public static LibertyServer server;

    private static String VERSION_REGEX = "[0-9]+\\.[0-9]+.*";

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

        // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
        // Lacking this fix, transaction manager will experience an auth failure and log FFDC for it.
        // The following line causes an XA-capable data source to be used for the first time outside of a test method execution,
        // so that the FFDC is not considered a test failure.
        JsonObject response = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .run(JsonObject.class);
        Log.info(c, "setUp", "DefaultDataSource response: " + response);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", // TODO remove once transaction manager fixes its circular reference bug
                          "CWWKS1300E", // auth alias doesn't exist
                          "WTRN0112E" // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
        );
    }

    @Test
    public void testAppAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .requestProp("X-Validation-User", "dbuser")
                        .requestProp("X-Validation-Password", "dbpass")
                        .run(JsonObject.class);
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
    }

    @Test
    public void testVariableSubstitution() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .requestProp("X-Validation-User", "${DB_USER}")
                        .requestProp("X-Validation-Password", "${DB_PASS}");
        JsonObject json = request.run(JsonObject.class);
        assertSuccessResponse(json, "DefaultDataSource", "DefaultDataSource");
        json = request.run(JsonObject.class);
        assertSuccessResponse(json, "DefaultDataSource", "DefaultDataSource");
    }

    @Test
    public void testEnvVariableSubstitution() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .requestProp("X-Validation-User", "${env.DB_USER_ENV}")
                        .requestProp("X-Validation-Password", "${env.DB_PASS_ENV}");
        JsonObject json = request.run(JsonObject.class);
        assertSuccessResponse(json, "DefaultDataSource", "DefaultDataSource");
        json = request.run(JsonObject.class);
        assertSuccessResponse(json, "DefaultDataSource", "DefaultDataSource");
    }

    @Test
    @ExpectedFFDC({ "java.sql.SQLNonTransientConnectionException",
                    "java.sql.SQLNonTransientException",
                    "javax.resource.spi.SecurityException",
                    "javax.resource.spi.ResourceAllocationException" })
    public void testAppAuthFails() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .requestProp("X-Validation-User", "bogus")
                        .requestProp("X-Validation-Password", "bogus")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "DefaultDataSource", json.getString("uid"));
        assertEquals(err, "DefaultDataSource", json.getString("id"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));

        // Now examine the failure object
        json = json.getJsonObject("failure");
        assertNotNull(err, json);
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "08004", json.getString("sqlState"));
        assertEquals(err, "40000", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLNonTransientException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Invalid authentication"));
    }

    @Test
    public void testDataSourceWithoutJDBCDriver() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DataSourceWithoutJDBCDriver")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "DataSourceWithoutJDBCDriver", json.getString("uid"));
        assertEquals(err, "DataSourceWithoutJDBCDriver", json.getString("id"));
        assertEquals(err, "jdbc/withoutJDBCDriver", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertTrue(err, json.getString("message").contains("dependencies"));
    }

    @Test
    public void testDefaultAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/dataSource[default-0]?auth=container")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "dataSource[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "jdbc/defaultauth", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertTrue(err, json.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertTrue(err, json.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }

    @Test
    public void testFeatureOfParentConfigNotEnabled() throws Exception {
        String response = new HttpsRequest(server, "/ibm/api/validation/dataSource/databaseStore[unavailableDBStore]%2FdataSource[unavailableDS]")
                        .expectCode(404)
                        .run(String.class);
        String err = "unexpected response: " + response;
        // Is there any way to know that this configuration is unavailable due to being nested under a config element of a feature that is not enabled?
        assertTrue(err, response.contains("CWWKO1500E") && response.contains("dataSource") && response.contains("databaseStore[unavailableDBStore]/dataSource[unavailableDS]"));
    }

    @Test
    public void testFeatureNotEnabled() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/mongoDB/MongoDBNotEnabled")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "MongoDBNotEnabled", json.getString("uid"));
        assertEquals(err, "MongoDBNotEnabled", json.getString("id"));
        assertEquals(err, "mongo/db", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertTrue(err, json.getString("message").contains("feature"));
    }

    /**
     * Supply an invalid query parameter to the validation REST endpoint and expect an error.
     */
    @Test
    public void testInvalidQueryParameter() throws Exception {
        new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource?badParam=something")
                        .expectCode(400)
                        .run(String.class);
    }

    @Test
    @ExpectedFFDC(value = { "java.sql.SQLException",
                            "javax.resource.spi.ResourceAllocationException",
                            "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testMultiple() throws Exception {
        JsonArray json = new HttpsRequest(server, "/ibm/api/validation/dataSource?auth=application")
                        .requestProp("X-Validation-User", "dbuser")
                        .requestProp("X-Validation-Password", "dbpass")
                        .run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 6, json.size()); // Increase this if you add more data sources to server.xml

        // Order is currently alphabetical based on config.displayId

        // [0]: config.displayId=dataSource[DataSourceWithoutJDBCDriver]
        JsonObject j = json.getJsonObject(0);
        assertEquals(err, "DataSourceWithoutJDBCDriver", j.getString("uid"));
        assertEquals(err, "DataSourceWithoutJDBCDriver", j.getString("id"));
        assertEquals(err, "jdbc/withoutJDBCDriver", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNull(err, j.get("info"));
        assertNotNull(err, j = j.getJsonObject("failure"));
        assertTrue(err, j.getString("message").contains("dependencies"));

        // [1]: config.displayId=dataSource[DefaultDataSource]
        j = json.getJsonObject(1);
        assertEquals(err, "DefaultDataSource", j.getString("uid"));
        assertEquals(err, "DefaultDataSource", j.getString("id"));
        assertNull(err, j.get("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "Apache Derby", j.getString("databaseProductName"));
        assertTrue(err, j.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertNull(err, j.get("catalog")); // currently not supported by Derby
        assertEquals(err, "DBUSER", j.getString("schema"));
        assertEquals(err, "dbuser", j.getString("user"));

        // [2]: config.displayId=dataSource[WrongDefaultAuth]
        j = json.getJsonObject(2);
        assertEquals(err, "WrongDefaultAuth", j.getString("uid"));
        assertEquals(err, "WrongDefaultAuth", j.getString("id"));
        assertEquals(err, "jdbc/wrongdefaultauth", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", j.getString("jdbcDriverName"));
        assertTrue(err, j.getString("jdbcDriverVersion").matches(VERSION_REGEX));

        // [3]: config.displayId=dataSource[default-0]
        j = json.getJsonObject(3);
        assertEquals(err, "dataSource[default-0]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertEquals(err, "jdbc/defaultauth", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));

        // [4]: config.displayId=dataSource[jdbc/nonexistentdb]
        j = json.getJsonObject(4);
        assertEquals(err, "jdbc/nonexistentdb", j.getString("uid"));
        assertEquals(err, "jdbc/nonexistentdb", j.getString("id"));
        assertEquals(err, "jdbc/nonexistentdb", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNull(err, j.get("info"));
        assertNotNull(err, j = j.getJsonObject("failure"));
        assertEquals(err, "XJ004", j.getString("sqlState"));
        assertEquals(err, "40000", j.getString("errorCode"));
        assertEquals(err, "java.sql.SQLException", j.getString("class"));
        assertTrue(err, j.getString("message").contains("memory:doesNotExist"));
        JsonArray stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(1).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(2).startsWith("org.apache.derby."));
        assertNotNull(err, j = j.getJsonObject("cause"));
        assertEquals(err, "org.apache.derby.iapi.error.StandardException", j.getString("class"));
        assertTrue(err, j.getString("message").contains("memory:doesNotExist"));
        stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(1).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(2).startsWith("org.apache.derby."));

        // [5]: config.displayId=transaction/dataSource[default-0]
        j = json.getJsonObject(5);
        assertEquals(err, "transaction/dataSource[default-0]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertNull(err, j.get("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "Apache Derby", j.getString("databaseProductName"));
        assertTrue(err, j.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", j.getString("jdbcDriverName"));
        assertTrue(err, j.getString("jdbcDriverVersion").matches(VERSION_REGEX));
        assertNull(err, j.get("catalog")); // currently not supported by Derby
        assertEquals(err, "DBUSER", j.getString("schema"));
        assertEquals(err, "dbuser", j.getString("user"));
    }

    @Test
    public void testMultipleWithNoResults() throws Exception {
        String response = new HttpsRequest(server, "/ibm/api/validation/cloudantDatabase")
                        .expectCode(404)
                        .run(String.class);
        String err = "unexpected response: " + response;
        assertTrue(err, response.contains("CWWKO1500E"));
    }

    @Test
    public void testNestedUnderTransaction() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/transaction%2FdataSource[default-0]?auth=container")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "transaction/dataSource[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertTrue(err, json.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertTrue(err, json.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }

    @Test
    public void testNotFound() throws Exception {
        String response = new HttpsRequest(server, "/ibm/api/validation/dataSource/NotAConfiguredDataSource")
                        .expectCode(404)
                        .run(String.class);
        String err = "unexpected response: " + response;
        assertTrue(err, response.contains("CWWKO1500E") && response.contains("dataSource") && response.contains("NotAConfiguredDataSource"));
    }

    @Test
    public void testNotValidatable() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/library/Derby")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "Derby", json.getString("uid"));
        assertEquals(err, "Derby", json.getString("id"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertTrue(err, json.getString("message").contains("not possible to validate this type of resource"));
    }

    @Test
    public void testProvidedAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource?auth=container&authAlias=auth1")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
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
    }

    @Test
    public void testProvidedAuthAndDefaultAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/WrongDefaultAuth?auth=container&authAlias=auth1")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "WrongDefaultAuth", json.getString("uid"));
        assertEquals(err, "WrongDefaultAuth", json.getString("id"));
        assertEquals(err, "jdbc/wrongdefaultauth", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertTrue(err, json.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertTrue(err, json.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }

    @ExpectedFFDC(value = { "javax.security.auth.login.LoginException", "javax.resource.ResourceException", "java.sql.SQLException" })
    @Test
    public void testProvidedAuthDoesNotExist() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource?auth=container&authAlias=authDoesntExist")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "DefaultDataSource", json.getString("uid"));
        assertEquals(err, "DefaultDataSource", json.getString("id"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));

        // Now examine the failure object
        json = json.getJsonObject("failure");
        assertNotNull(err, json);
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("sqlState"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "java.sql.SQLException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("CWWKS1300E"));
    }

    @Test
    public void testTopLevelConfigDisplayID() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/dataSource[default-0]?auth=application")
                        .requestProp("X-Validation-User", "dbuser")
                        .requestProp("X-Validation-Password", "dbpass")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "dataSource[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "jdbc/defaultauth", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "Apache Derby", json.getString("databaseProductName"));
        assertTrue(err, json.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", json.getString("jdbcDriverName"));
        assertTrue(err, json.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }

    @Test
    public void testTopLevelID() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
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
        assertNull(err, json.get("catalog")); // currently not supported by Derby
        assertEquals(err, "DBUSER", json.getString("schema"));
        assertEquals(err, "dbuser", json.getString("user"));
    }

    @Test
    @ExpectedFFDC(value = { "java.sql.SQLException",
                            "javax.resource.spi.ResourceAllocationException",
                            "com.ibm.ws.rsadapter.exceptions.DataStoreAdapterException" })
    public void testTopLevelIDSQLException() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/jdbc%2Fnonexistentdb")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "jdbc/nonexistentdb", json.getString("uid"));
        assertEquals(err, "jdbc/nonexistentdb", json.getString("id"));
        assertEquals(err, "jdbc/nonexistentdb", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "XJ004", json.getString("sqlState"));
        assertEquals(err, "40000", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("memory:doesNotExist"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(1).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(2).startsWith("org.apache.derby."));
        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "org.apache.derby.iapi.error.StandardException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("memory:doesNotExist"));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(1).startsWith("org.apache.derby."));
        assertTrue(err, stack.getString(2).startsWith("org.apache.derby."));
    }

    @ExpectedFFDC(value = { "javax.resource.spi.SecurityException", "java.sql.SQLNonTransientException",
                            "javax.resource.spi.ResourceAllocationException", "java.sql.SQLNonTransientConnectionException" })
    @Test
    public void testWrongDefaultAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/WrongDefaultAuth?auth=container")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "WrongDefaultAuth", json.getString("uid"));
        assertEquals(err, "WrongDefaultAuth", json.getString("id"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));

        // Now examine the failure object
        json = json.getJsonObject("failure");
        assertNotNull(err, json);
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("successful"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "08004", json.getString("sqlState"));
        assertEquals(err, "40000", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLNonTransientException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Invalid authentication"));
    }

    @ExpectedFFDC(value = { "javax.resource.spi.SecurityException", "java.sql.SQLNonTransientException",
                            "javax.resource.spi.ResourceAllocationException", "java.sql.SQLNonTransientConnectionException" })
    @Test
    public void testWrongProvidedAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource?auth=container&authAlias=auth2")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;
        assertEquals(err, "DefaultDataSource", json.getString("uid"));
        assertEquals(err, "DefaultDataSource", json.getString("id"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));

        // Now examine the failure object
        json = json.getJsonObject("failure");
        assertNotNull(err, json);
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("jndiName"));
        assertNull(err, json.get("successful"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "08004", json.getString("sqlState"));
        assertEquals(err, "40000", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLNonTransientException", json.getString("class"));
        assertTrue(err, json.getString("message").contains("Invalid authentication"));
    }

    /*
     * Test that the validation endpoint is not accessible when using the reader role
     */
    @Test
    public void testValidateReaderRole() throws Exception {
        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/dataSource").basicAuth("reader", "readerpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }

        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource").basicAuth("reader", "readerpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }

        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/doesnotexist").basicAuth("reader", "readerpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }
    }

    /*
     * Test that the validation endpoint is not accessible to a user who is not assigned the
     * reader or administrator role
     */
    @Test
    public void testValidateUserWithoutRoles() throws Exception {
        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/dataSource").basicAuth("user", "userpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }

        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/dataSource/DefaultDataSource").basicAuth("user", "userpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }

        try {
            JsonArray json = new HttpsRequest(server, "/ibm/api/validation/doesnotexist").basicAuth("user", "userpwd").run(JsonArray.class);
            fail("unexpected response: " + json);
        } catch (Exception ex) {
            assertTrue("Expected 403 response", ex.getMessage().contains("403"));
        }
    }

    @Test
    public void testPOSTMethodRejected() throws Exception {
        String response = new HttpsRequest(server, "/ibm/api/validation/dataSource/dataSource[default-0]?auth=container")
                        .method("POST")
                        .expectCode(405) // Method Not Allowed
                        .run(String.class);
        String err = "Unexpected response: " + response.toString();
        assertTrue(err, response.contains("SRVE0295E"));
    }

    private static void assertSuccessResponse(JsonObject json, String expectedUID, String expectedID) {
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, expectedUID, json.getString("uid"));
        assertEquals(err, expectedID, expectedID == null ? json.get("id") : json.getString("id"));
        assertNull(err, json.get("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        JsonObject info = json.getJsonObject("info");
        assertNotNull(err, info);
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }
}
