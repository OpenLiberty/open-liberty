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

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.rest.handler.validator.loginmodule.TestLoginModule;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateDSCustomLoginModuleTest extends FATServletClient {

    private static final Class<?> c = ValidateDSCustomLoginModuleTest.class;

    @Server("validator-customLoginModule-Server")
    public static LibertyServer server;

    private static String VERSION_REGEX = "[0-9]+\\.[0-9]+.*";

    @BeforeClass
    public static void setUp() throws Exception {
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
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        server.waitForStringsInLogUsingMark(messages);

        // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
        // Lacking this fix, transaction manager will experience an auth failure and log FFDC for it.
        // The following line causes an XA-capable data source to be used for the first time outside of a test method execution,
        // so that the FFDC is not considered a test failure.
        JsonObject response = new HttpsRequest(server, "/ibm/api/validation/dataSource/customLoginDS").run(JsonObject.class);
        Log.info(c, "setUp", "DefaultDataSource response: " + response);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKE0701E", // TODO remove once transaction manager fixes its circular reference bug
                          "CWWKS1300E", // auth alias doesn't exist
                          "WTRN0112E" // TODO remove once transactions code is fixed to use container auth for the recovery log dataSource
        );
    }

    /**
     * Test validation of a dataSource that relies on a custom login module to add credentials.
     * This default endpoint invocation is equivalent to an application direct lookup, which does
     * not get container managed authentication applied and therefore should not succeed.
     */
    @Test
    @ExpectedFFDC({ "java.sql.SQLNonTransientConnectionException",
                    "java.sql.SQLNonTransientException",
                    "javax.resource.spi.SecurityException",
                    "javax.resource.spi.ResourceAllocationException" })
    public void testCustomLoginModuleDirectLookupInvalid() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/customLoginDS")
                        .method("POST")
                        .run(JsonObject.class);
        Log.info(c, testName.getMethodName(), "HTTP response: " + json);
        String err = "unexpected response: " + json;
        assertEquals(err, "customLoginDS", json.getString("uid"));
        assertEquals(err, "customLoginDS", json.getString("id"));
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
        assertEquals(err, "08004", getString(json, "sqlState"));
        assertEquals(err, "40000", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLNonTransientException", getString(json, "class"));
        assertTrue(err, getString(json, "message").contains("Invalid authentication"));
    }

    /**
     * Test validation of a dataSource that relies on a custom login module to add credentials
     */
    @Test
    public void testCustomLoginContainerAuth() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/customLoginDS?auth=container")
                        .method("POST")
                        .run(JsonObject.class);
        Log.info(c, testName.getMethodName(), "HTTP response: " + json);
        assertSuccessResponse(json, "customLoginDS", "customLoginDS", "jdbc/customLoginDS");
    }

    /**
     * Test validation of a dataSource has no auth configured in server.xml, but specifies a customLoginConfig
     * as a request parameter. This is to account for apps that may be doing this in their ibm-web-bnd.xml:
     * <resource-ref name="jdbc/customLoginDSWebBnd" binding-name="jdbc/customLoginDSWebBnd">
     * <custom-login-configuration name="customLoginEntry"/>
     * </resource-ref>
     */
    @Test
    public void testCustomLoginIBMWebBnd() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/customLoginDSWebBnd?auth=container&loginConfig=customLoginEntry")
                        .method("POST")
                        .run(JsonObject.class);
        Log.info(c, testName.getMethodName(), "HTTP response: " + json);
        assertSuccessResponse(json, "customLoginDSWebBnd", "customLoginDSWebBnd", "jdbc/customLoginDSWebBnd");
    }

    /**
     * Test validation of a dataSource has no auth configured in server.xml, but specifies a customLoginConfig
     * as a request parameter. This is to account for apps that may be doing this in their ibm-web-bnd.xml:
     * <resource-ref name="jdbc/customLoginDSWebBnd" binding-name="jdbc/customLoginDSWebBnd">
     * <custom-login-configuration name="customLoginEntry">
     * <property name="custom-login-prop" value="foo"/>
     * </custom-login-configuration>
     * </resource-ref>
     */
    @Test
    public void testCustomLoginModuleProperties() throws Exception {
        String URL = "/ibm/api/validation/dataSource/customLoginDSWebBnd?auth=container&loginConfig=customLoginEntry";
        String propsJson = "{ \"loginConfigProperties\": { \"" + TestLoginModule.CUSTOM_PROPERTY_KEY + "\": \"foo\" } }";
        JsonObject json = new HttpsRequest(server, URL)
                        .method("POST")
                        .jsonBody(propsJson)
                        .run(JsonObject.class);
        Log.info(c, testName.getMethodName(), "HTTP response: " + json);
        assertSuccessResponse(json, "customLoginDSWebBnd", "customLoginDSWebBnd", "jdbc/customLoginDSWebBnd");

        assertTrue("Did not find expected log statement indiciating custom login property was set",
                   server.findStringsInLogs("TEST_CHECK " + TestLoginModule.CUSTOM_PROPERTY_KEY + "=foo").size() > 0);
    }

    /**
     * Test specifyig a non-existant customLoginConfig
     */
    @Test
    @ExpectedFFDC({ "javax.security.auth.login.LoginException",
                    "javax.resource.ResourceException",
                    "java.sql.SQLException" })
    public void testCustomLoginIBMWebBndWrongName() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/dataSource/customLoginDSWebBnd?auth=container&loginConfig=bogus")
                        .method("POST")
                        .run(JsonObject.class);
        Log.info(c, testName.getMethodName(), "HTTP response: " + json);
        String err = "unexpected response: " + json;
        assertEquals(err, "customLoginDSWebBnd", getString(json, "uid"));
        assertEquals(err, "customLoginDSWebBnd", getString(json, "id"));
        assertEquals(err, "jdbc/customLoginDSWebBnd", getString(json, "jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNull(err, json.get("cause"));

        // Now examine the failure object
        json = json.getJsonObject("failure");
        assertNotNull(err, json);
        assertNull(err, json.get("uid"));
        assertNull(err, json.get("id"));
        assertNull(err, json.get("successful"));
        assertNull(err, json.get("failure"));
        assertNull(err, json.get("info"));
        assertEquals(err, "java.sql.SQLException", getString(json, "class"));
        assertTrue(err, getString(json, "message").contains("No LoginModules configured for bogus"));
    }

    /*
     * Test that a nested data source which has an atypical case can be validated
     * when the type matches the case specified in server.xml. In this test the specific
     * instance's uid is specified.
     */
    @Test
    public void testValidateNestedDifferentCase() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/DATASOURCE/transaction%2FDATASOURCE%5Bdefault-0%5D?auth=container&authAlias=auth1")
                        .run(JsonObject.class);
        String err = "unexpected response: " + json;

        assertEquals(err, "transaction/DATASOURCE[default-0]", json.getString("uid"));
        assertNull(err, json.get("id"));
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

    /*
     * Test that a nested data source which has an atypical case can be validated
     * when the type matches the case specified in server.xml. In this test no
     * UID is provided.
     */
    @Test
    public void testValidateNestedDifferentCaseMulitple() throws Exception {
        JsonArray json = new HttpsRequest(server, "/ibm/api/validation/DATASOURCE?auth=container&authAlias=auth1")
                        .run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 1, json.size());
        JsonObject j = json.getJsonObject(0);

        assertEquals(err, "transaction/DATASOURCE[default-0]", j.getString("uid"));
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

    private static void assertSuccessResponse(JsonObject json, String expectedUID, String expectedID, String expectedJndiName) {
        String err = "Unexpected json response: " + json.toString();
        assertEquals(err, expectedUID, getString(json, "uid"));
        assertEquals(err, expectedID, getString(json, "id"));
        assertEquals(err, expectedJndiName, getString(json, "jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        JsonObject info = json.getJsonObject("info");
        assertNotNull(err, info);
        assertEquals(err, "Apache Derby", info.getString("databaseProductName"));
        assertTrue(err, info.getString("databaseProductVersion").matches(VERSION_REGEX));
        assertEquals(err, "Apache Derby Embedded JDBC Driver", info.getString("jdbcDriverName"));
        assertTrue(err, info.getString("jdbcDriverVersion").matches(VERSION_REGEX));
    }

    private static String getString(JsonObject json, String key) {
        try {
            return json.getString(key);
        } catch (NullPointerException e) {
            // JSON-P will annoyingly throw a NPE if we request a String key that does not exist,
            // which leaves us with pretty useless JUnit failure messages. Return null instead so
            // the JUnit failures are more helpful
            return null;
        }
    }

}
