/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;
import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPITestUtil;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import test.common.TestLogger;

/**
 * Tests to ensure MP OpenAPI annotations in a single app deployed on a server
 * are read and an Open API documentation is returned.
 */
@RunWith(FATRunner.class)
@SuppressWarnings("restriction")
public class ApplicationProcessorTest extends FATServletClient {
    private static final Class<?> c = ApplicationProcessorTest.class;
    private static final String APP_NAME_1 = "appWithAnnotations";
    private static final String APP_NAME_2 = "appWithStaticDoc";
    private static final String APP_NAME_3 = "simpleServlet";

    @Server("FATServer")
    public static LibertyServer server;

    @Rule
    public final TestLogger logger = new TestLogger();

    @BeforeClass
    public static void setUpTest() throws Exception {
        ShrinkHelper.defaultApp(server, APP_NAME_1, "app.web.airlines.*");
        ShrinkHelper.defaultApp(server, APP_NAME_2);
        ShrinkHelper.defaultApp(server, APP_NAME_3, "app.web.servlet");

        LibertyServer.setValidateApps(false);
        server.startServer(c.getSimpleName() + ".log");
    }

    /**
     * This ensures all the applications are removed before running each test to make sure
     * we start with a clean server.xml.
     */
    @Before
    public void setUp() throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        config.getApplications().stream().forEach(app -> OpenAPITestUtil.removeApplication(server, app.getName()));
        server.updateServerConfiguration(config);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWWKO1650E");
    }

    @Test
    public void testApplicationProcessor() throws Exception {
        OpenAPITestUtil.ensureOpenAPIEndpointIsReady(server);

        // Validate the app is deployed
        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessor(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationAdded(server, APP_NAME_1);
        String app1Doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        JsonNode openapiNode = OpenAPITestUtil.readYamlTree(app1Doc);
        checkServer(openapiNode, "https://{username}.gigantic-server.com:{port}/{basePath}", "https://test-server.com:80/basePath");
        checkPaths(openapiNode, 16);

        // Add a second app and ensure it's not deployed
        OpenAPITestUtil.addApplication(server, APP_NAME_2);
        OpenAPITestUtil.waitForApplicationAdded(server, APP_NAME_2);
        String openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Only a single application must be processed by the application processor.", app1Doc, openapi);

        // Remove the first application and ensure now the appWithStaticDoc app is now processed
        OpenAPITestUtil.removeApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessor(server, APP_NAME_2);
        String app2Doc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        System.out.println(app2Doc);
        openapiNode = OpenAPITestUtil.readYamlTree(app2Doc);
        checkServer(openapiNode, "https:///MySimpleAPI/1.0.0");
        checkPaths(openapiNode, 1);

        // Remove the appWithStaticDoc app and ensure that the default empty OpenAPI documentation is created
        OpenAPITestUtil.removeApplication(server, APP_NAME_2);
        String emptyDoc = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        openapiNode = OpenAPITestUtil.readYamlTree(emptyDoc);
        checkServer(openapiNode, "http://" + server.getHostname() + ":" + server.getHttpDefaultPort());
        checkPaths(openapiNode, 0);

        // Add an empty servlet app is deployed and ensure the default empty OpenAPI documentation is created
        OpenAPITestUtil.addApplication(server, APP_NAME_3);
        OpenAPITestUtil.waitForApplicationProcessor(server, APP_NAME_3);
        OpenAPITestUtil.waitForApplicationAdded(server, APP_NAME_3);
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Server with a single empty app should not change the default OpenAPI document.", emptyDoc, openapi);

        // Now add an app with OpenAPI artifacts and ensure it shows up
        OpenAPITestUtil.addApplication(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationProcessor(server, APP_NAME_1);
        OpenAPITestUtil.waitForApplicationAdded(server, APP_NAME_1);
        openapi = OpenAPIConnection.openAPIDocsConnection(server, false).download();
        assertEquals("FAIL: Only a single application must be processed by the application processor.", app1Doc, openapi);
    }

    private void checkServer(JsonNode root, String... expectedUrls) {
        JsonNode serversNode = root.get("servers");
        assertNotNull(serversNode);
        assertTrue(serversNode.isArray());
        ArrayNode servers = (ArrayNode) serversNode;

        List<String> urls = Arrays.asList(expectedUrls);
        servers.findValues("url").forEach(url -> assertTrue("FAIL: Unexpected server URL " + url, urls.contains(url.asText())));
        assertEquals("FAIL: Found incorrect number of server objects.", urls.size(), servers.size());
    }

    private void checkPaths(JsonNode root, int expectedCount, String... containedPaths) {
        JsonNode pathsNode = root.get("paths");
        assertNotNull(pathsNode);
        assertTrue(pathsNode.isObject());
        ObjectNode paths = (ObjectNode) pathsNode;

        assertEquals("FAIL: Found incorrect number of server objects.", expectedCount, paths.size());
        List<String> expected = Arrays.asList(containedPaths);
        expected.stream().forEach(path -> assertNotNull("FAIL: OpenAPI document does not contain the expected path " + path, paths.get(path)));
    }
}
