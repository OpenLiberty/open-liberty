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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.json.JsonObject;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Paths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.openapi.impl.parser.OpenAPIParser;
import com.ibm.ws.microprofile.openapi.impl.parser.core.models.SwaggerParseResult;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpsRequest;

@RunWith(FATRunner.class)
@SkipForRepeat(EE9_FEATURES) // TODO: Enable this once mpopenapi-2.0 (jakarta enabled) is available
public class ConfigOpenApiSchemaTest extends FATServletClient {

    @Server("com.ibm.ws.rest.handler.config.openapi.fat")
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
        messages.add("CWWKT0016I: .*openapi/platform"); // CWWKT0016I: Web application available (default_host): http://oc2832358306.rchland.ibm.com:8010/openapi/platform/
        server.waitForStringsInLogUsingMark(messages);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    /**
     * Test the config schema is available under the openapi/platform/config endpoint, and
     * honors both the "format=json" query parameter and "Accept application/json" http header.
     */
    @Test
    public void testConfigOpenAPIAsJSON() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/openapi/platform/config?format=json");
        JsonObject json = request.run(JsonObject.class);
        String err = "Unexpected json response: " + json.toString();
        JsonObject paths = json.getJsonObject("paths");
        assertNotNull(err, paths);
        String pathsString = paths.toString();
        assertTrue(err, pathsString.contains("/config/"));
        assertTrue(err, pathsString.contains("/config/{elementName}"));
        assertTrue(err, pathsString.contains("/config/{elementName}/{uid}"));
        assertTrue(err, paths.size() == 3);

        //test again with json specified in the header
        request = new HttpsRequest(server, "/openapi/platform/config");
        json = request.requestProp("Accept", "application/json").run(JsonObject.class);
        err = "Unexpected json response: " + json.toString();
        paths = json.getJsonObject("paths");
        assertNotNull(err, paths);
        pathsString = paths.toString();
        assertTrue(err, pathsString.contains("/config/"));
        assertTrue(err, pathsString.contains("/config/{elementName}"));
        assertTrue(err, pathsString.contains("/config/{elementName}/{uid}"));
        assertTrue(err, paths.size() == 3);
    }

    /**
     * Test the config schema is available under the openapi/platform/config endpoint, and
     * is returned as YAML by default.
     */
    @Test
    public void testConfigOpenAPIAsYAML() throws Exception {
        HttpsRequest request = new HttpsRequest(server, "/openapi/platform/config");
        String yaml = request.run(String.class);
        SwaggerParseResult result = new OpenAPIParser().readContents(yaml, null, null, null);
        assertNotNull(result);
        OpenAPI openAPI = result.getOpenAPI();
        assertNotNull(openAPI);
        Paths paths = openAPI.getPaths();
        assertNotNull(paths);
        String err = "Unexpected paths response: " + Arrays.toString(paths.entrySet().toArray());
        assertTrue(err, paths.containsKey("/config/"));
        assertTrue(err, paths.containsKey("/config/{elementName}"));
        assertTrue(err, paths.containsKey("/config/{elementName}/{uid}"));
        assertTrue(err, paths.size() == 3);
    }
}
