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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

import com.ibm.websphere.simplicity.ShrinkHelper;
import io.openliberty.mcp.internal.fat.tool.mixedEarApp.McpModuleStartupBean;
import io.openliberty.mcp.internal.fat.tool.mixedEarApp.NonMcpBean;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * Tests that in an EAR with multiple web modules, only the module with MCP tools
 * initializes the MCP servlet and endpoint.
 */
@RunWith(FATRunner.class)
public class MixedEarModuleTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive mcpModuleWar = ShrinkWrap.create(WebArchive.class, "mcpModule.war")
                                            .addClass(McpModuleStartupBean.class);
        WebArchive nonMcpModuleWar = ShrinkWrap.create(WebArchive.class, "nonMcpModule.war")
                                               .addClass(NonMcpBean.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "mixedEarApp.ear")
                                          .addAsModule(mcpModuleWar)
                                          .addAsModule(nonMcpModuleWar);

        ShrinkHelper.exportDropinAppToServer(server, ear, SERVER_ONLY);
        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("SRVE0190E");
    }

    @Test
    public void testMcpModuleEndpointAndTool() throws Exception {
        assertNotNull("Expected MCP server endpoint for mcpModule was not logged",
                      server.waitForStringInLogUsingMark("MCP server endpoint: .*/mcpModule/mcp$"));

        try (McpClient client = McpClient.builder(server, "/mcpModule").build()) {
            client.initialize();
            String tools = client.listAllTools();
            assertTrue("tools/list in mcpModule should contain mcpModuleTool but was: " + tools,
                       tools.contains("mcpModuleTool"));

            String toolCall = """
                            {
                              "jsonrpc": "2.0",
                              "id": "1",
                              "method": "tools/call",
                              "params": {
                                "name": "mcpModuleTool"
                              }
                            }
                            """;
            String response = client.callMCP(toolCall);
            assertTrue("tools/call in mcpModule should contain expected response but was: " + response,
                       response.contains("mcpModule response"));
        }
    }

    @Test
    public void testNonMcpModuleNoEndpoint() throws Exception {
        assertNull("MCP server endpoint for nonMcpModule should not be logged",
                   server.waitForStringInLogUsingMark("MCP server endpoint: .*/nonMcpModule/mcp", 0));

        try (McpClient client = McpClient.builder(server, "/nonMcpModule").build()) {
            String initRequest = """
                            {
                              "jsonrpc": "2.0",
                              "id": "1",
                              "method": "initialize",
                              "params": {
                                "protocolVersion": "2025-11-25",
                                "capabilities": {},
                                "clientInfo": {
                                  "name": "fat-test-client",
                                  "version": "1.0.0"
                                }
                              }
                            }
                            """;
            client.callMCPRaw(initRequest).expectedStatus(404).run();
        }
    }
}
