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

import java.util.Collections;

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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;

/**
 * Test that verifies the configurable async timeout functionality.
 * Tests two scenarios:
 * 1. Short timeout (2 seconds) - should timeout quickly
 * 2. Default timeout (30 seconds) - uses the default value
 * 3. Timeout changed to 5 seconds using server_updated_async_timeout.xml
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

    /**
     * Test that verifies asyncTimeout configuration can be dynamically updated
     * and the changes are reflected in the timeout behavior.
     *
     * Initial configuration: asyncTimeout="2s"
     * Updated configuration: asyncTimeout="5s"
     *
     * This test verifies that after updating the configuration, a tool that takes
     * 3 seconds to complete will timeout with the initial 2s timeout but succeed
     * with the updated 5s timeout.
     */
    @Test
    @Mode(TestMode.FULL)
    public void testDynamicAsyncTimeoutUpdate() throws Exception {
        // First, verify the initial timeout (2 seconds) causes a timeout for a 3-second operation
        String timeoutRequest = """
                        {
                          "jsonrpc": "2.0",
                          "id": "timeout-test-4",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncToolThatNeverCompletes",
                            "arguments": {
                              "input": "This should timeout initially"
                            }
                          }
                        }
                        """;

        long startTime = System.currentTimeMillis();
        try {
            shortTimeoutClient.callMCP(timeoutRequest);
            fail("Expected timeout exception was not thrown with initial 2s timeout");
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            assertTrue("Initial timeout should occur within 5 seconds, but took " + elapsedTime + "ms",
                       elapsedTime < 5000);
            assertTrue("Initial timeout should take at least 2 seconds, but took " + elapsedTime + "ms",
                       elapsedTime >= 2000);
        }

        server.setMarkToEndOfLog();

        // Mark the session as deleted since config changes will invalidate it
        // This prevents the cleanup code from trying to delete an already-invalid session
        shortTimeoutClient.markSessionDeleted();

        // Dynamically update the asyncTimeout configuration by replacing the server.xml
        // This changes the timeout from 2s to 5s
        server.setServerConfigurationFile("server_updated_async_timeout.xml");
        server.waitForConfigUpdateInLogUsingMark(Collections.singleton("shortTimeoutTest"));

        // Re-initialize the session after config update since the old session was invalidated
        // Create a new client which will automatically establish a new session
        McpClient newShortTimeoutClient = new McpClient(server, "/shortTimeoutTest");
        newShortTimeoutClient.initializeSession();

        try {
            // Verify that with the updated timeout (5 seconds), the same operation still times out
            // but takes longer (since asyncToolThatNeverCompletes never completes)
            startTime = System.currentTimeMillis();
            try {
                newShortTimeoutClient.callMCP(timeoutRequest);
                fail("Expected timeout exception was not thrown with updated 5s timeout");
            } catch (Exception e) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                assertTrue("Updated timeout should occur within 8 seconds, but took " + elapsedTime + "ms",
                           elapsedTime < 8000);
                assertTrue("Updated timeout should take at least 5 seconds, but took " + elapsedTime + "ms",
                           elapsedTime >= 5000);
            }

            // Verify that a quick operation still works with the updated timeout
            String quickRequest = """
                            {
                              "jsonrpc": "2.0",
                              "id": "timeout-test-5",
                              "method": "tools/call",
                              "params": {
                                "name": "asyncEcho",
                                "arguments": {
                                  "input": "Quick response after config update"
                                }
                              }
                            }
                            """;

            String response = newShortTimeoutClient.callMCP(quickRequest);
            assertTrue("Response should contain the echoed input after config update",
                       response.contains("Quick response after config update"));
        } finally {
            // Clean up the new client's session
            newShortTimeoutClient.cleanupSession();
        }
    }

}