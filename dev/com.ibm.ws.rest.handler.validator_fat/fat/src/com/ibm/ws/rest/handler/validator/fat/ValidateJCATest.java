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

import java.net.URLEncoder;
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

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
public class ValidateJCATest extends FATServletClient {
    @Server("com.ibm.ws.rest.handler.validator.jca.fat")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        ResourceAdapterArchive rar = ShrinkWrap.create(ResourceAdapterArchive.class, "TestValidationAdapter.rar")
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
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        messages.add("J2CA7001I: .* TestValidationAdapter"); // J2CA7001I: Resource adapter TestValidationAdapter installed in # seconds.

        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("J2CA0046E: .*eis/cf-port-not-in-range", // intentionally raised error to test exception path
                          "J2CA0021E: .*eis/cf6", // intentional error due to invalid user being supplied by login module
                          "J2CA0021E: .*IllegalStateException: Connection was dropped", // testing exception path
                          "J2CA0021E: .*javax.resource.ResourceException", // testing exception path
                          "J2CA0021E: .*javax.resource.spi.ResourceAllocationException", // testing exception path
                          "J2CA0021E: .*ResourceAdapterInternalException: Something bad has happened. See cause." // testing exception path
        );
    }

    /**
     * Validate a connectionFactory using application authentication, but not specifying a user or password,
     * and instead relying on the built-in user/password of the mock resource adapter.
     */
    @Test
    public void testApplicationAuthForConnectionFactoryWithDefaultUser() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1?auth=application").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "DefaultUserName", json.getString("user"));
    }

    /**
     * Validate a connectionFactory using application authentication and specify a user/password.
     * Include unicode characters in the header parameters by URL encoding them.
     */
    @Test
    public void testApplicationAuthForConnectionFactoryWithSpecifiedUser() throws Exception {
        String encodedUser = URLEncoder.encode("user\u217b1", "UTF-8");
        String encodedPwd = URLEncoder.encode("1user\u217b", "UTF-8");
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1?auth=application&headerParamsURLEncoded=true")
                        .requestProp("X-Validation-User", encodedUser)
                        .requestProp("X-Validation-Password", encodedPwd);
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "user\u217b1", json.getString("user"));
    }

    /**
     * Validate a connectionFactory for a JCA data source using application authentication and specify a user/password,
     * where the validation fails and raises an exception with a SQL State and error code.
     */
    @AllowedFFDC("java.sql.SQLNonTransientConnectionException")
    @Test
    public void testApplicationAuthForJCADataSourceWithSpecifiedUserFails() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/ds5?auth=application")
                        .requestProp("X-Validation-User", "user5")
                        .requestProp("X-Validation-Password", "5user");
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "ds5", json.getString("uid"));
        assertEquals(err, "ds5", json.getString("id"));
        assertEquals(err, "eis/ds5", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.getJsonObject("info"));

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "08001", json.getString("sqlState"));
        assertEquals(err, "127", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLNonTransientConnectionException", json.getString("class"));
        assertEquals(err, "Connection rejected for user names that end in '5'.", json.getString("message"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.JDBCConnectionImpl.invoke("));
        assertTrue(err, stack.getString(1).startsWith("com.sun.proxy.$Proxy"));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("sqlState"));
        assertEquals(err, "ERR_SEC_USR5", json.getString("errorCode"));
        assertEquals(err, "javax.resource.spi.SecurityException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Not accepting user names that end with '5'."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.JDBCConnectionImpl.invoke("));
        assertTrue(err, stack.getString(1).startsWith("com.sun.proxy.$Proxy"));
        assertTrue(err, stack.getString(2).startsWith("com."));

        // cause
        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "28000", json.getString("sqlState"));
        assertEquals(err, "0", json.getString("errorCode"));
        assertEquals(err, "java.sql.SQLInvalidAuthorizationSpecException", json.getString("class"));
        assertEquals(err, "The database is unable to accept user names that include a '5'.", json.getString("message"));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.JDBCConnectionImpl.invoke("));
        assertTrue(err, stack.getString(1).startsWith("com.sun.proxy.$Proxy"));
        assertTrue(err, stack.getString(2).startsWith("com."));
        assertNull(err, json.getJsonObject("cause"));
    }

    /**
     * Attempt to validate a connectionFactory that does not exist in server configuration.
     */
    @Test
    public void testConnectionFactoryNotFound() throws Exception {
        String response = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/NotAConfiguredConnectionFactory").expectCode(404).run(String.class);
        String err = "unexpected response: " + response;
        assertTrue(err, response.contains("CWWKO1500E") && response.contains("connectionFactory") && response.contains("NotAConfiguredConnectionFactory"));
    }

    /**
     * Validate a connectionFactory using container authentication, relying on the default containerAuthData from server config.
     */
    @Test
    public void testContainerAuthForConnectionFactoryWithDefaultAuthData() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1?auth=container").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "containerAuthUser1", json.getString("user"));
    }

    /**
     * Validate a connectionFactory using container authentication and explicitly specifying the authData element from server config to use.
     * Use a unicode value in a query parameter.
     */
    @Test
    public void testContainerAuthForConnectionFactoryWithSpecifiedAuthData() throws Exception {
        String encodedAuthAlias = URLEncoder.encode("auth-\u2171", "UTF-8");
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1?auth=container&authAlias=" + encodedAuthAlias).run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "containerAuthUser2", json.getString("user"));
    }

    /**
     * Validate a connectionFactory for a JCA data source using container authentication with a custom login module
     * and custom login properties.
     * Include a unicode value in login config props.
     */
    @Test
    public void testCustomLoginModuleForJCADataSource() throws Exception {
        String encodedLoginNameProp = "loginName=" + URLEncoder.encode("\u2159lmUser", "UTF-8"); // \u2159 is '1/6'
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/ds5?auth=container&loginConfig=customLoginEntry&headerParamsURLEncoded=true")
                        .method("GET")
                        .requestProp("X-Login-Config-Props", encodedLoginNameProp + ",loginNum=6")
                        .run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "ds5", json.getString("uid"));
        assertEquals(err, "ds5", json.getString("id"));
        assertEquals(err, "eis/ds5", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationEIS", json.getString("databaseProductName"));
        assertEquals(err, "33.56.65", json.getString("databaseProductVersion"));
        assertEquals(err, "TestValidationJDBCAdapter", json.getString("driverName"));
        assertEquals(err, "36.77.85", json.getString("driverVersion"));
        assertEquals(err, "TestValDB", json.getString("catalog"));
        assertEquals(err, "\u2159LMUSER6", json.getString("schema"));
        assertEquals(err, "\u2159lmUser6", json.getString("user"));
    }

    /**
     * Validate a connectionFactory for a JCA data source using container authentication with a custom login module
     * and custom login properties.
     * Include a unicode value in login config props.
     */
    @Test
    public void testCustomLoginPropertyThatLacksProperDelimiter() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/ds5?auth=container&loginConfig=customLoginEntry")
                        .method("GET")
                        .requestProp("X-Login-Config-Props", "loginName|myName") // correct delimiter is '=', not '|'
                        .run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "ds5", json.getString("uid"));
        assertEquals(err, "ds5", json.getString("id"));
        assertEquals(err, "eis/ds5", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure"));
        String message;
        assertNotNull(err, message = json.getString("message"));
        assertTrue(err, message.contains("=")); // message warns that the '=' delimiter is missing
    }

    /**
     * Validate a connectionFactory where the resource adapter implements an optional "testConnection()" method
     * on its ManagedConnection, which in this case fails with an Error subclass.
     */
    @AllowedFFDC("java.io.IOError")
    @Test
    public void testErrorOnTestConnection() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1")
                        .requestProp("X-Validation-User", "testerruser1") // this user name is a signal to force a testConnection failure
                        .requestProp("X-Validation-Password", "1testerruser");
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "java.io.IOError", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("java.sql.SQLNonTransientConnectionException: Database appears to be down."));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionImpl.testConnection(ManagedConnectionImpl.java:"));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));

        // cause
        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "-13579", json.getString("errorCode"));
        assertEquals(err, "08006", json.getString("sqlState"));
        assertEquals(err, "java.sql.SQLNonTransientConnectionException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Database appears to be down."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionImpl.testConnection(ManagedConnectionImpl.java:"));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));
        assertNull(err, json.getJsonObject("cause"));
    }

    /**
     * Validate a connectionFactory where the resource adapter implements an optional "testConnection()" method
     * on its ManagedConnection, which in this case indicate failures by returning a false value rather than raising an exception.
     */
    @AllowedFFDC("javax.resource.spi.ResourceAllocationException")
    @Test
    public void testFailTestConnection() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1")
                        .requestProp("X-Validation-User", "testfailuser1") // this user name is a signal to force a testConnection failure
                        .requestProp("X-Validation-Password", "1testfailuser");
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "javax.resource.spi.ResourceAllocationException", json.getString("class"));
        assertNotNull(err, json.getString("message"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com."));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));
        assertNull(err, json.getJsonObject("cause"));
    }

    /**
     * Validate a connectionFactory with a container authentication resource reference with login properties, and verify that it
     * uses the login module indicated by the jaasLoginContextEntryRef to log in, supplying it with the login properties.
     */
    @Test
    public void testJaasLoginModuleForContainerAuthWithLoginProperties() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/jaasLoginCF?auth=container")
                        .requestProp("X-Login-Config-Props", "loginName=JAASUser,loginNum=6")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "jaasLoginCF", json.getString("uid"));
        assertEquals(err, "jaasLoginCF", json.getString("id"));
        assertEquals(err, "eis/cf6", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "JAASUser6", json.getString("user"));
    }

    /**
     * Validate a connectionFactory with a container authentication resource reference, and verify that it
     * uses the login module indicated by the jaasLoginContextEntryRef to log in.
     */
    @AllowedFFDC("javax.resource.spi.SecurityException")
    @Test
    public void testJaasLoginModuleForContainerAuthWithoutLoginProperties() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/jaasLoginCF?auth=container")
                        .run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "jaasLoginCF", json.getString("uid"));
        assertEquals(err, "jaasLoginCF", json.getString("id"));
        assertEquals(err, "eis/cf6", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));
        assertNotNull(err, json = json.getJsonObject("failure")); // login module defaults to dbuser/dbpass, which are not considered valid
        assertEquals(err, "ERR_AUTH", json.getString("errorCode"));
        assertEquals(err, "javax.resource.spi.SecurityException", json.getString("class"));
        String message = json.getString("message");
        assertTrue(err, message.startsWith("Unable to authenticate with dbuser"));
        JsonArray stack;
        assertNotNull(err, stack = json.getJsonArray("stack"));
        assertTrue(err, stack.size() > 3);
    }

    /**
     * Use /ibm/api/validation/connectionFactory to validate all connection factories
     */
    @AllowedFFDC({
                   "java.lang.IllegalArgumentException", // intentionally raised by mock resource adapter to cover exception paths
                   "javax.resource.spi.CommException", // intentionally raised by mock resource adapter to cover exception paths
                   "javax.resource.spi.InvalidPropertyException", // intentionally raised by mock resource adapter to cover exception paths
                   "javax.resource.spi.ResourceAllocationException" // Liberty wraps or replaces above errors with ResourceAllocationException
    })
    @Test
    public void testMultipleConnectionFactories() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory");
        JsonArray json = request.method("GET").run(JsonArray.class);
        String err = "unexpected response: " + json;

        assertEquals(err, 6, json.size()); // Increase this if you add more connection factories to server.xml

        // Order is currently alphabetical based on config.displayId

        // [0]: config.displayId=connectionFactory[cf1]
        JsonObject j = json.getJsonObject(0);
        assertEquals(err, "cf1", j.getString("uid"));
        assertEquals(err, "cf1", j.getString("id"));
        assertEquals(err, "eis/cf1", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", j.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", j.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", j.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", j.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", j.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", j.getString("eisProductName"));
        assertEquals(err, "33.56.65", j.getString("eisProductVersion"));
        assertEquals(err, "DefaultUserName", j.getString("user"));

        // [1]: config.displayId=connectionFactory[cf2]
        j = json.getJsonObject(1);
        assertEquals(err, "cf2", j.getString("uid"));
        assertEquals(err, "cf2", j.getString("id"));
        assertEquals(err, "eis/cf-invalid-host", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNull(err, j.get("info"));
        // Liberty converts javax.resource.spi.CommException to ResourceAllocationException. Why? // TODO
        assertNotNull(err, j = j.getJsonObject("failure"));
        assertNull(err, j.get("errorCode"));
        assertEquals(err, "javax.resource.spi.ResourceAllocationException", j.getString("class"));
        assertTrue(err, j.getString("message").startsWith("Unable to connect to notfound.rchland.ibm.com"));
        JsonArray stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com."));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));
        // cause // TODO should at least chain the original exception as the cause
        //assertNotNull(err, j = j.getJsonObject("cause"));
        //assertNull(err, j.get("errorCode"));
        //assertEquals(err, "javax.resource.spi.CommException", j.getString("class"));
        //assertTrue(err, j.getString("message").startsWith("Unable to connect to notfound.rchland.ibm.com"));
        //stack = j.getJsonArray("stack");
        //assertNotNull(err, stack);
        //assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        //assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        //assertTrue(err, stack.getString(1).startsWith("com."));
        //assertTrue(err, stack.getString(2).startsWith("com."));
        assertNull(err, j.getJsonObject("cause"));

        // [2]: config.displayId=connectionFactory[default-0]
        j = json.getJsonObject(2);
        // Liberty converts javax.resource.spi.InvalidPropertyException to ResourceAllocationException. Why? // TODO
        assertNotNull(err, j = j.getJsonObject("failure"));
        assertEquals(err, "ERR_PORT_NEG", j.getString("errorCode"));
        assertEquals(err, "javax.resource.spi.ResourceAllocationException", j.getString("class"));
        assertTrue(err, j.getString("message").startsWith("portNumber"));
        stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com."));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));
        // cause // TODO should at least chain the original exception as the cause
        //assertNotNull(err, j = j.getJsonObject("cause"));
        //assertEquals(err, "ERR_PORT_NEG", j.getString("errorCode"));
        //assertEquals(err, "javax.resource.spi.InvalidPropertyException", j.getString("class"));
        //assertTrue(err, j.getString("message").startsWith("portNumber"));
        //stack = j.getJsonArray("stack");
        //assertNotNull(err, stack);
        //assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        //assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        //assertTrue(err, stack.getString(1).startsWith("com."));
        //assertTrue(err, stack.getString(2).startsWith("com."));
        // cause
        assertNotNull(err, j = j.getJsonObject("cause"));
        assertNull(err, j.get("errorCode"));
        assertEquals(err, "java.lang.IllegalArgumentException", j.getString("class"));
        assertEquals(err, "Negative port numbers are not allowed.", j.getString("message"));
        stack = j.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));
        assertNull(err, j.getJsonObject("cause"));

        // [3]: config.displayId=connectionFactory[default-1]
        j = json.getJsonObject(3);
        assertEquals(err, "connectionFactory[default-1]", j.getString("uid"));
        assertNull(err, j.get("id"));
        assertEquals(err, "eis/cf-port-not-in-range", j.getString("jndiName"));
        assertFalse(err, j.getBoolean("successful"));
        assertNull(err, j.get("info"));
        assertNotNull(err, j.getJsonObject("failure"));
        // connectionFactory[default-1] is already tested under testTopLevelConnectionFactoryWithoutIDWithChainedExceptions

        // [4]: config.displayId=connectionFactory[ds5]
        j = json.getJsonObject(4);
        assertEquals(err, "ds5", j.getString("uid"));
        assertEquals(err, "ds5", j.getString("id"));
        assertEquals(err, "eis/ds5", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestValidationEIS", j.getString("databaseProductName"));
        assertEquals(err, "33.56.65", j.getString("databaseProductVersion"));
        assertEquals(err, "TestValidationJDBCAdapter", j.getString("driverName"));
        assertEquals(err, "36.77.85", j.getString("driverVersion"));
        assertEquals(err, "TestValDB", j.getString("catalog"));
        assertEquals(err, "DEFAULTUSERNAME", j.getString("schema"));
        assertEquals(err, "DefaultUserName", j.getString("user"));

        // [5]: config.displayId=connectionFactory[jaasLoginCF]
        j = json.getJsonObject(5);
        assertEquals(err, "jaasLoginCF", j.getString("uid"));
        assertEquals(err, "jaasLoginCF", j.getString("id"));
        assertEquals(err, "eis/cf6", j.getString("jndiName"));
        assertTrue(err, j.getBoolean("successful"));
        assertNull(err, j.get("failure"));
        assertNotNull(err, j = j.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", j.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", j.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", j.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", j.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", j.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", j.getString("eisProductName"));
        assertEquals(err, "33.56.65", j.getString("eisProductVersion"));
        assertEquals(err, "DefaultUserName", j.getString("user")); // login module does not apply for direct lookup
    }

    /**
     * Validate a connectionFactory where the resource adapter implements an optional "testConnection()" method
     * on its ManagedConnection, which in this case fails with a ResourceException subclass.
     */
    @AllowedFFDC("javax.resource.spi.ResourceAdapterInternalException")
    @Test
    public void testResourceExceptionOnTestConnection() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1")
                        .requestProp("X-Validation-User", "testresxuser1") // this user name is a signal to force a testConnection failure
                        .requestProp("X-Validation-Password", "1testresxuser");
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "javax.resource.spi.ResourceAdapterInternalException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Something bad has happened. See cause."));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionImpl.testConnection(ManagedConnectionImpl.java:"));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));

        // cause
        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "ERR_CONNECT", json.getString("errorCode"));
        assertEquals(err, "javax.resource.spi.CommException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Lost connection to host."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionImpl.testConnection(ManagedConnectionImpl.java:"));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));
        assertNull(err, json.getJsonObject("cause"));
    }

    /**
     * Validate a connectionFactory where the resource adapter implements an optional "testConnection()" method
     * on its ManagedConnection, which in this case fails with a RuntimeException subclass.
     */
    @AllowedFFDC({ "java.lang.IllegalStateException", "javax.resource.ResourceException" })
    @Test
    public void testRuntimeExceptionOnTestConnection() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1")
                        .requestProp("X-Validation-User", "testrunxuser1") // this user name is a signal to force a testConnection failure
                        .requestProp("X-Validation-Password", "1testrunxuser");
        JsonObject json = request.method("GET").run(JsonObject.class);

        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        assertNotNull(err, json = json.getJsonObject("failure"));
        assertEquals(err, "javax.resource.ResourceException", json.getString("class"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com."));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "java.lang.IllegalStateException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Connection was dropped."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionImpl.testConnection(ManagedConnectionImpl.java:"));
        assertNotNull(err, stack.getString(1));
        assertNotNull(err, stack.getString(2));
        assertNull(err, json.getJsonObject("cause"));
    }

    /**
     * Validate a connectionFactory that is configured at top level with an id attribute.
     */
    @Test
    public void testTopLevelConnectionFactoryWithID() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/cf1").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "cf1", json.getString("uid"));
        assertEquals(err, "cf1", json.getString("id"));
        assertEquals(err, "eis/cf1", json.getString("jndiName"));
        assertTrue(err, json.getBoolean("successful"));
        assertNull(err, json.get("failure"));
        assertNotNull(err, json = json.getJsonObject("info"));
        assertEquals(err, "TestValidationAdapter", json.getString("resourceAdapterName"));
        assertEquals(err, "28.45.53", json.getString("resourceAdapterVersion"));
        assertEquals(err, "1.7", json.getString("connectorSpecVersion"));
        assertEquals(err, "OpenLiberty", json.getString("resourceAdapterVendor"));
        assertEquals(err, "This tiny resource adapter doesn't do much at all.", json.getString("resourceAdapterDescription"));
        assertEquals(err, "TestValidationEIS", json.getString("eisProductName"));
        assertEquals(err, "33.56.65", json.getString("eisProductVersion"));
        assertEquals(err, "DefaultUserName", json.getString("user"));
    }

    /**
     * Validate a connectionFactory that is configured at top level without an id attribute,
     * where the validation attempt fails with an exception that has a chain of cause exceptions,
     * some of which have error codes.
     */
    @AllowedFFDC({ "java.lang.IllegalArgumentException", // intentionally raised by mock resource adapter to cover exception paths
                   "javax.resource.spi.ResourceAllocationException" // same error as above, wrapped as ResourceAllocationException
    })
    @Test
    public void testTopLevelConnectionFactoryWithoutIDWithChainedExceptions() throws Exception {
        JsonObject json = new HttpsRequest(server, "/ibm/api/validation/connectionFactory/connectionFactory[default-1]").run(JsonObject.class);
        String err = "Unexpected json response: " + json;
        assertEquals(err, "connectionFactory[default-1]", json.getString("uid"));
        assertNull(err, json.get("id"));
        assertEquals(err, "eis/cf-port-not-in-range", json.getString("jndiName"));
        assertFalse(err, json.getBoolean("successful"));
        assertNull(err, json.get("info"));

        // Liberty wraps the IllegalArgumentException with ResourceAllocationException
        assertNotNull(err, json = json.getJsonObject("failure"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "javax.resource.spi.ResourceAllocationException", json.getString("class"));
        JsonArray stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("com."));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "java.lang.IllegalArgumentException", json.getString("class"));
        assertEquals(err, "22", json.getString("message"));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "ERR_PORT_INV", json.getString("errorCode"));
        assertEquals(err, "org.test.validator.adapter.InvalidPortException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Port cannot be used."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertEquals(err, "ERR_PORT_OOR", json.getString("errorCode"));
        assertEquals(err, "javax.resource.spi.ResourceAllocationException", json.getString("class"));
        assertTrue(err, json.getString("message").startsWith("Port not in allowed range."));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNotNull(err, json = json.getJsonObject("cause"));
        assertNull(err, json.get("errorCode"));
        assertEquals(err, "javax.resource.ResourceException", json.getString("class"));
        assertEquals(err, "Port number is too low.", json.getString("message"));
        stack = json.getJsonArray("stack");
        assertNotNull(err, stack);
        assertTrue(err, stack.size() > 10); // stack is actually much longer, but size could vary
        assertTrue(err, stack.getString(0).startsWith("org.test.validator.adapter.ManagedConnectionFactoryImpl.createManagedConnection(ManagedConnectionFactoryImpl.java:"));
        assertTrue(err, stack.getString(1).startsWith("com."));
        assertTrue(err, stack.getString(2).startsWith("com."));

        assertNull(err, json.get("cause"));
    }
}
