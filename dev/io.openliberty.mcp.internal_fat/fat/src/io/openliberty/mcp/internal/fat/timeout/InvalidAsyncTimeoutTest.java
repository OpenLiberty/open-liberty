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
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;

/**
 * Test that verifies invalid asyncTimeout configuration handling.
 * Tests that an invalid timeout value ("sheep") is properly rejected with appropriate error messages.
 */
@RunWith(FATRunner.class)
public class InvalidAsyncTimeoutTest {

    private static final Class<?> c = InvalidAsyncTimeoutTest.class;

    @Rule
    public McpClient invalidTimeoutClient = new McpClient(server, "/invalidTimeoutTest");

    @Server("mcp-server-invalid-async-timeout")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        // Deploy app with invalid timeout ("sheep")
        WebArchive invalidTimeoutWar = ShrinkWrap.create(WebArchive.class, "invalidTimeoutTest.war")
                                                 .addPackage(AsyncTools.class.getPackage())
                                                 .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportAppToServer(server, invalidTimeoutWar, SERVER_ONLY);

        server.startServer();
        // Wait for application to be deployed and MCP endpoint to be available
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/invalidTimeoutTest/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        // Allow the FFDC for invalid configuration
        server.stopServer("CWWKG0075E");
    }

    /**
     * Test that verifies an invalid asyncTimeout value ("sheep") is properly rejected.
     * The test expects:
     * 1. FFDC error CWWKG0075E to be logged
     * 2. Error message indicating the value "sheep" is not valid for asyncTimeout
     * 3. The default timeout (30 seconds) to be used instead
     */
    @Test
    @Mode(TestMode.FULL)
    public void testInvalidAsyncTimeout() throws Exception {
        // Look for the expected error message about invalid configuration
        String expectedMessage = "The value sheep is not valid for attribute Asynchronous request timeout";

        assertNotNull("Expected error message about invalid asyncTimeout value not found",
                      server.waitForStringInLog(expectedMessage));

        // Verify that CWWKG0075E error was logged
        assertNotNull("Expected CWWKG0075E error not found in logs",
                      server.waitForStringInLog("CWWKG0075E"));

        // Verify that the application still works with the default timeout (30 seconds)
        // by calling a tool that completes quickly
        long startTime = System.currentTimeMillis();

        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": "invalid-timeout-test-1",
                          "method": "tools/call",
                          "params": {
                            "name": "asyncToolThatNeverCompletes",
                            "arguments": {
                              "input": "This should timeout with default 30s"
                            }
                          }
                        }
                        """;

        try {
            invalidTimeoutClient.callMCP(request);
            fail("Expected timeout exception was not thrown");
        } catch (Exception e) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            // Should use default timeout of 30 seconds since "sheep" is invalid
            assertTrue("Timeout should occur within 35 seconds (default timeout), but took " + elapsedTime + "ms",
                       elapsedTime < 35000);
            assertTrue("Timeout should take at least 30 seconds (default timeout), but took " + elapsedTime + "ms",
                       elapsedTime >= 30000);
        }
    }

}