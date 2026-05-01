/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.monitor;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 * FAT test for MCP Monitor feature
 * Tests operation and session metrics collection via MBeans and metrics adapters
 */
@RunWith(FATRunner.class)
public class McpMonitorTest extends FATServletClient {

    private final static String APP_NAME = "mcpMonitorTest";

    @Server("mcp-server-monitor")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/" + APP_NAME);

    private static final String ECHO_TOOL_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 1,
                      "method": "tools/call",
                      "params": {
                        "name": "echo",
                        "arguments": {"message": "test message"}
                      }
                    }
                    """;

    private static final String ADD_TOOL_REQUEST = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "add",
                        "arguments": {"a": 5, "b": 3}
                      }
                    }
                    """;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addPackage(McpMonitorMetricsServlet.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);
        server.startServer();

        // Wait for server to be ready
        assertNotNull("Server should start successfully",
                      server.waitForStringInLog("CWWKF0011I"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that operation metrics are recorded when MCP tools are called
     */
    @Test
    public void testOperationMetricsRecorded() throws Exception {
        // Call MCP tool
        String response = client.callMCP(ECHO_TOOL_REQUEST);
        assertNotNull("Response should not be null", response);
        assertTrue("Response should contain result", response.contains("result"));

        // Verify operation metrics were recorded via servlet
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testOperationMetricsRecorded");
    }

    /**
     * Test that multiple operations are tracked correctly
     */
    @Test
    public void testMultipleOperationsTracked() throws Exception {
        // Call multiple tools
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify metrics
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testMultipleOperationsTracked");
    }

    /**
     * Test that session metrics are recorded
     */
    @Test
    public void testSessionMetricsRecorded() throws Exception {
        // Perform operations within session
        client.callMCP(ECHO_TOOL_REQUEST);
        client.callMCP(ADD_TOOL_REQUEST);

        // End session to trigger session metrics
        client.deleteSession();

        // Verify session metrics
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testSessionMetricsRecorded");
    }

    /**
     * Test that MBeans are registered for MCP operations
     */
    @Test
    public void testMBeanRegistration() throws Exception {
        // Call tool to trigger MBean creation
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify MBean registration
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testMBeanRegistration");
    }

    /**
     * Test that operation duration is tracked
     */
    @Test
    public void testOperationDurationTracking() throws Exception {
        // Call tool
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify duration was recorded
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testOperationDurationTracking");
    }

    /**
     * Test that protocol version attributes are captured
     */
    @Test
    public void testProtocolVersionAttributes() throws Exception {
        // Call tool
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify protocol attributes
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testProtocolVersionAttributes");
    }

    /**
     * Test that tool name is captured in metrics
     */
    @Test
    public void testToolNameAttribute() throws Exception {
        // Call specific tool
        client.callMCP(ECHO_TOOL_REQUEST);

        // Verify tool name attribute
        FATServletClient.runTest(server, APP_NAME + "/McpMonitorMetricsServlet",
                                 "testToolNameAttribute");
    }
}
