/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.timeout;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;

/**
 * Test that verifies the configurable async timeout functionality.
 * Tests two scenarios:
 * 1. Short timeout (2 seconds) - should timeout quickly
 * 2. Default timeout (30 seconds) - uses the default value
 */
@RunWith(FATRunner.class)
public class ConfigurableAsyncTimeoutTest {

    private static final Class<?> c = ConfigurableAsyncTimeoutTest.class;

    @Rule
    public McpClient shortTimeoutClient = new McpClient(server, "/shortTimeoutTest");

    @Rule
    public McpClient defaultTimeoutClient = new McpClient(server, "/defaultTimeoutTest");

    @Server("mcp-server-configurable-async-timeout")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Deploy app with short timeout (2 seconds)
        WebArchive shortTimeoutWar = ShrinkWrap.create(WebArchive.class, "shortTimeoutTest.war")
                                               .addPackage(AsyncTools.class.getPackage())
                                               .addPackage(ToolStatus.class.getPackage());

        // Deploy app with default timeout (30 seconds)
        WebArchive defaultTimeoutWar = ShrinkWrap.create(WebArchive.class, "defaultTimeoutTest.war")
                                                 .addPackage(AsyncTools.class.getPackage())
                                                 .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportAppToServer(server, shortTimeoutWar, SERVER_ONLY);
        ShrinkHelper.exportAppToServer(server, defaultTimeoutWar, SERVER_ONLY);

        server.startServer();
        // Wait for both applications to be fully deployed and MCP endpoints to be available
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/shortTimeoutTest/mcp$"));
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/defaultTimeoutTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    /**
     * Test that verifies a tool that never completes will timeout after the configured
     * short timeout period (2 seconds). This test expects an exception to be thrown
     * when the timeout is reached.
     */
    @Test
    public void testShortAsyncTimeout() throws Exception {
        long startTime = System.currentTimeMillis();

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "timeout-test-1",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncToolThatNeverCompletes",
                            "arguments": {
                              "input": "This should timeout"
                            }
                          }
                        }
                        """;

        try {
            shortTimeoutClient.callMCP(request);
            fail("Expected timeout exception was not thrown");
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertTrue("Timeout should occur within 5 seconds, but took " + elapsedTime + "ms",
                       elapsedTime < 5000);
            assertTrue("Timeout should take at least 2 seconds, but took " + elapsedTime + "ms",
                       elapsedTime >= 2000);
        }
    }

    /**
     * Test that verifies the default timeout (30 seconds) is used when not explicitly configured.
     * This test calls a tool that completes quickly to verify the endpoint works with default timeout.
     */
    @Test
    public void testDefaultAsyncTimeout() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "timeout-test-2",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncEcho",
                            "arguments": {
                              "input": "Test with default timeout"
                            }
                          }
                        }
                        """;

        String response = defaultTimeoutClient.callMCP(request);

        // Verify the tool executed successfully
        assertTrue("Response should contain the echoed input",
                   response.contains("Test with default timeout"));
    }

    /**
     * Test that verifies a tool that completes quickly works fine even with a short timeout.
     */
    @Test
    public void testQuickToolWithShortTimeout() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "timeout-test-3",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncEcho",
                            "arguments": {
                              "input": "Quick response"
                            }
                          }
                        }
                        """;

        String response = shortTimeoutClient.callMCP(request);

        // Verify the tool executed successfully even with short timeout
        assertTrue("Response should contain the echoed input",
                   response.contains("Quick response"));
    }

}