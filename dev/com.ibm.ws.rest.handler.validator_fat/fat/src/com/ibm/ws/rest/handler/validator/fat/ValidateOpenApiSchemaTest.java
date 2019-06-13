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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
        messages.add("CWWKT0016I"); // CWWKT0016I: Web application available (default_host): http://9.10.111.222:8010/ibm/api/
        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
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

        request.method("POST");
        json = request.run(JsonObject.class);
        err = "Unexpected json response: " + json.toString();
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
}
