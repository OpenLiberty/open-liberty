/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.DISABLE_VALIDATION;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.ACCEPT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_PROTOCOL_VERSION;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.MCP_SESSION_ID;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_ACCEPT_DEFAULT;
import static io.openliberty.mcp.internal.fat.utils.TestConstants.VALUE_MCP_PROTOCOL_VERSION;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Set;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.config.Application;
import com.ibm.websphere.simplicity.config.EnterpriseApplication;
import com.ibm.websphere.simplicity.config.Mcp;
import com.ibm.websphere.simplicity.config.ServerConfiguration;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class DynamicMcpConfigTest extends FATServletClient {

    private static final String INCLUDE_FILE = "server-dynamic-app.xml";
    private static final String APP_NAME = "dynamicMcpPathUpdateTest";
    private static final String APP_ONE = "dynamicMcpMultiAppOne";
    private static final String APP_TWO = "dynamicMcpMultiAppTwo";
    private static final String APP_THREE = "dynamicMcpMultiAppThree";

    private static final String INITIALIZE_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": "1",
                      "method": "initialize",
                      "params": {
                        "protocolVersion": "2025-11-25",
                        "capabilities": {},
                        "clientInfo": {
                          "name": "fat-test-client",
                          "title": "FAT Test Client",
                          "version": "1.0.0"
                        }
                      }
                    }
                    """;

    @Server("mcp-server-dynamic-config")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        deployEar(server, APP_NAME);
        deployEar(server, APP_ONE);
        deployEar(server, APP_TWO);
        deployEar(server, APP_THREE);
        server.saveServerConfiguration();
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        try {
            server.deleteFileFromLibertyServerRoot(INCLUDE_FILE);
            server.restoreServerConfiguration();
        } finally {
            server.stopServer("SRVE0190E",  // File not found when probing unserved paths
                              "CWNEN0047W", // Resource annotations on fields ignored when mcp-1.0 not yet active
                              "CWNEN0049W"); // Resource annotations on methods ignored when mcp-1.0 not yet active
        }
    }

    @After
    public void cleanupAfterTest() throws Exception {
        server.deleteFileFromLibertyServerRoot(INCLUDE_FILE);
        server.setMarkToEndOfLog();
        server.restoreServerConfiguration();
        server.waitForConfigUpdateInLogUsingMark(Collections.emptySet(), true);
    }

    private static void deployEar(LibertyServer server, String appName) throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, appName + ".war")
                                   .addPackage(BasicTools.class.getPackage());
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, appName + ".ear")
                                          .addAsModule(war);
        ShrinkHelper.exportAppToServer(server, ear, new DeployOptions[] { SERVER_ONLY, DISABLE_VALIDATION });
    }

    private void assertEndpointNotFound(String appName, String mcpPath) throws Exception {
        try (McpClient client = McpClient.builder(server, "/" + appName).path(mcpPath).build()) {
            client.callMCPRaw(INITIALIZE_REQUEST).expectedStatus(404).run();
        }
    }

    private void assertEndpointLive(String appName, String mcpPath) throws Exception {
        try (McpClient client = McpClient.builder(server, "/" + appName).path(mcpPath).build()) {
            client.initialize();
            String tools = client.listAllTools();
            assertNotNull("Expected tool/list response from " + mcpPath, tools);
        }
    }

    /**
     * Setup R (Ben's include drop-in test):
     * Server starts with no mcp-1.0 and no app configured. An optional include file containing
     * both mcp-1.0 and the enterpriseApplication with <mcp path="..."> and <info name="...">
     * is dropped into the server root after startup.
     */
    @Test
    public void testMcpEndpointBecomesAvailableWhenIncludeFileIsDroppedIn() throws Exception {
        String endpoint = "/dynamic-mcp-updated";

        assertEndpointNotFound(APP_NAME, endpoint);

        server.setMarkToEndOfLog();
        server.copyFileToLibertyServerRoot(INCLUDE_FILE);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        try (McpClient client = McpClient.builder(server, "/" + APP_NAME).path(endpoint).build()) {
            client.initialize();
            String tools = client.listAllTools();
            assertNotNull("Expected tool/list response", tools);
        }

        HttpRequest httpRequest = new HttpRequest(server, "/" + APP_NAME + endpoint)
                                                                                     .requestProp(ACCEPT, VALUE_ACCEPT_DEFAULT)
                                                                                     .requestProp(MCP_PROTOCOL_VERSION, VALUE_MCP_PROTOCOL_VERSION)
                                                                                     .jsonBody(INITIALIZE_REQUEST)
                                                                                     .method("POST")
                                                                                     .expectCode(200);
        String response = httpRequest.run(String.class);
        JSONAssert.assertEquals("Expected serverInfo name ibm-mq-mcp-server",
                                "{\"result\":{\"serverInfo\":{\"name\":\"ibm-mq-mcp-server\"}}}",
                                response, JSONCompareMode.LENIENT);

        String sessionId = httpRequest.getResponseHeader(MCP_SESSION_ID);
        if (sessionId != null) {
            new HttpRequest(server, "/" + APP_NAME + endpoint)
                                                              .requestProp(MCP_SESSION_ID, sessionId)
                                                              .method("DELETE")
                                                              .run(String.class);
        }
    }

    /**
     * Setup E:
     * An application with <mcp path="..."> is configured and started before mcp-1.0 is enabled.
     * When mcp-1.0 is subsequently added to the feature manager, the MCP endpoint must become available.
     */
    @Test
    @AllowedFFDC("java.lang.NoClassDefFoundError")
    public void testMcpPathAppliedWhenOnlyTheFeatureIsAddedAfterStartup() throws Exception {
        String endpoint = "/dynamic-mcp-updated";

        ServerConfiguration config = server.getServerConfiguration();
        EnterpriseApplication app = new EnterpriseApplication();
        app.setLocation(APP_NAME + ".ear");
        Mcp mcp = new Mcp();
        mcp.setPath(endpoint);
        app.getMcps().add(mcp);
        config.getEnterpriseApplications().add(app);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), false);

        assertEndpointNotFound(APP_NAME, endpoint);

        config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("mcp-1.0");

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        assertEndpointLive(APP_NAME, endpoint);
    }

    /**
     * Setup G:
     * Plain <application> element without ibm:extends (matched by parent PID).
     * Server starts with no feature and no app. Adding mcp-1.0 and a plain <application> with
     * <mcp path="..."> dynamically must serve the endpoint.
     */
    @Test
    public void testMcpPathAppliedToPlainApplicationAddedAfterStartup() throws Exception {
        String endpoint = "/dynamic-mcp-updated";

        assertEndpointNotFound(APP_NAME, endpoint);

        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("mcp-1.0");
        Application app = new Application();
        app.setLocation(APP_NAME + ".ear");
        Mcp mcp = new Mcp();
        mcp.setPath(endpoint);
        app.getMcps().add(mcp);
        config.getApplications().add(app);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton(APP_NAME), true);

        assertEndpointLive(APP_NAME, endpoint);
    }

    /**
     * Setup H:
     * Multiple applications added together in a single dynamic update.
     * Two <enterpriseApplication> elements and one plain <application> element each with distinct
     * <mcp path="..."> configurations must each be served at their respective paths.
     */
    @Test
    public void testEachAppServedAtItsOwnPathWhenFeatureAndAppsAreAddedTogether() throws Exception {
        assertEndpointNotFound(APP_ONE, "/mcp-one");
        assertEndpointNotFound(APP_TWO, "/mcp-two");
        assertEndpointNotFound(APP_THREE, "/mcp-three");

        ServerConfiguration config = server.getServerConfiguration();
        config.getFeatureManager().getFeatures().add("mcp-1.0");

        EnterpriseApplication app1 = new EnterpriseApplication();
        app1.setLocation(APP_ONE + ".ear");
        Mcp mcp1 = new Mcp();
        mcp1.setPath("/mcp-one");
        app1.getMcps().add(mcp1);
        config.getEnterpriseApplications().add(app1);

        EnterpriseApplication app2 = new EnterpriseApplication();
        app2.setLocation(APP_TWO + ".ear");
        Mcp mcp2 = new Mcp();
        mcp2.setPath("/mcp-two");
        app2.getMcps().add(mcp2);
        config.getEnterpriseApplications().add(app2);

        Application app3 = new Application();
        app3.setLocation(APP_THREE + ".ear");
        Mcp mcp3 = new Mcp();
        mcp3.setPath("/mcp-three");
        app3.getMcps().add(mcp3);
        config.getApplications().add(app3);

        server.setMarkToEndOfLog();
        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(Set.of(APP_ONE, APP_TWO, APP_THREE), true);

        assertEndpointLive(APP_ONE, "/mcp-one");
        assertEndpointLive(APP_TWO, "/mcp-two");
        assertEndpointLive(APP_THREE, "/mcp-three");
    }
}
