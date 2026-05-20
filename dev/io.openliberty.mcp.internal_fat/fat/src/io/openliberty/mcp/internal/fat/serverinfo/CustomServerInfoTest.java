/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.serverinfo;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Test that verifies multiple applications can be deployed with separate serverInfo configurations.
 * Deploys two applications on the same server:
 * 1. partialServerInfoTest.war - with partial/null serverInfo fields (only name set)
 * 2. customServerInfoTest.war - with full custom serverInfo fields
 */
@RunWith(FATRunner.class)
public class CustomServerInfoTest {

    private static final Class<?> c = CustomServerInfoTest.class;

    @Rule
    public McpClient partialClient = new McpClient(server, "/partialServerInfoTest");

    @Rule
    public McpClient customClient = new McpClient(server, "/fullyCustomServerInfoTest");

    @Server("mcp-server-custom-info")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Deploy first app with partial serverInfo
        WebArchive partialWar = ShrinkWrap.create(WebArchive.class, "partialServerInfoTest.war")
                                          .addPackage(BasicTools.class.getPackage());

        // Deploy second app with custom serverInfo
        WebArchive customWar = ShrinkWrap.create(WebArchive.class, "fullyCustomServerInfoTest.war")
                                         .addPackage(BasicTools.class.getPackage());

        ShrinkHelper.exportAppToServer(server, partialWar, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, customWar, SERVER_ONLY);

        server.startServer();
        // Wait for both applications to be fully deployed and MCP endpoints to be available
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/partialServerInfoTest/mcp$"));
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/fullyCustomServerInfoTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * Test that verifies partial serverInfo with defaults applied correctly:
     * name: "partial-server" (explicitly configured)
     * version: "1.0.0" (default as not explicitly configured)
     * title and description are optional values and are not expected as they were not explicitly configured
     */
    @Test
    @Mode(TestMode.FULL)
    public void testPartialServerInfoHandling() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            },
                            "capabilities": {}
                          }
                        }
                        """;

        String response = partialClient.callMCP(request);

        // Verify partial serverInfo with defaults applied correctly:
        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "tools": {
                                "listChanged": false
                              }
                            },
                            "serverInfo": {
                              "name": "partial-server",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);
    }

    /**
     * Test that verifies custom mcpServerInfo configuration is properly applied
     * when specified in server.xml
     */
    @Test
    @Mode(TestMode.FULL)
    public void testFullyCustomServerInfoHandling() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "clientInfo": {
                              "name": "test-client",
                              "version": "1.0"
                            },
                            "capabilities": {}
                          }
                        }
                        """;

        String response = customClient.callMCP(request);

        // Verify custom serverInfo values from server.xml
        String expectedResponse = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "result": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {
                              "tools": {
                                "listChanged": false
                              }
                            },
                            "serverInfo": {
                              "name": "my-custom-server",
                              "title": "My Custom MCP Server",
                              "version": "2.5.0",
                              "description": "My custom description"
                            }
                          }
                        }
                        """;

        JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.STRICT);
    }

}
