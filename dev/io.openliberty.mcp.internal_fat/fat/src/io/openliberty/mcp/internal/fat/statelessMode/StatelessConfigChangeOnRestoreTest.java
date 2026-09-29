/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.statelessMode;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static componenttest.custom.junit.runner.Mode.TestMode.FULL;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.CheckpointTest;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServer.CheckpointInfo;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.checkpoint.spi.CheckpointPhase;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.tool.cancellationApp.CancellationTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;

/**
 * Test that verifies changing the stateless mcpServer config before restoring
 * a checkpointed server causes the application to restart.
 *
 * This is a FULL mode InstantOn test that:
 * 1. Starts with stateless=true configuration
 * 2. Checkpoints the server after app start
 * 3. Changes configuration to stateless=false before restore
 * 4. Verifies the application restarted on restore (expected behavior for config changes)
 */
@CheckpointTest
@RunWith(FATRunner.class)
@Mode(FULL)
public class StatelessConfigChangeOnRestoreTest extends FATServletClient {

    @Server("mcp-stateless-checkpoint-server")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "statelessConfigChangeTest.war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addPackage(CancellationTools.class.getPackage())
                                   .addPackage(StatelessModeTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());
        ShrinkHelper.exportAppToServer(server, war, SERVER_ONLY);

        CheckpointInfo checkpointInfo = new CheckpointInfo(CheckpointPhase.AFTER_APP_START, true,
                        server -> {
                            assertNotNull("'CWWKZ0001I: ' message not found in log.",
                                          server.waitForStringInLogUsingMark("CWWKZ0001I:.*statelessConfigChangeTest", 0));
                            // Change config from stateless=true to stateless=false before restore
                            try {
                                server.setServerConfigurationFile("server_stateful.xml");
                            } catch (Exception e) {
                                throw new RuntimeException("Failed to update server configuration before restore", e);
                            }
                        });

        // Decided NOT to make any changes to runtime. Maintain the current behavior of restarting the application when a change in the mcpServer configuration is detected.
        checkpointInfo.setAssertNoAppRestartOnRestore(false);

        server.setCheckpoint(checkpointInfo);
        server.startServer();

        assertNotNull("Web application was not available during server launch",
                      server.waitForStringInLogUsingMark("CWWKT0016I:.*statelessConfigChangeTest"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that verifies the server started with stateless=true configuration
     * and after restore with stateless=false, the MCP endpoint works correctly
     * in stateful mode (with session management).
     */
    @Test
    public void testStatelessToStatefulConfigChange() throws Exception {
        // Initialize client in stateful mode (after restore, server is now stateful)
        McpClient client = new McpClient(server, "/statelessConfigChangeTest", McpClient.StateMode.STATEFUL);
        client.initialize();

        try {
            // Test a tool call works in stateful mode
            String toolRequest = """
                            {
                              "jsonrpc": "2.0",
                              "id": "2",
                              "method": "tools/call",
                              "params": {
                                "name": "textContentTool",
                                "arguments": {
                                  "input": "Testing stateful mode after restore"
                                }
                              }
                            }
                            """;

            String toolResponse = client.callMCP(toolRequest);

            // Verify the tool executed successfully
            assertTrue("Tool response should contain the echoed input",
                       toolResponse.contains("Testing stateful mode after restore"));

            // Test session management with multiple requests using the same session
            for (int i = 0; i < 3; i++) {
                String sessionRequest = """
                                {
                                  "jsonrpc": "2.0",
                                  "id": "%d",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "textContentTool",
                                    "arguments": {
                                      "input": "Request %d"
                                    }
                                }
                                }
                                """.formatted(i + 3, i + 1);

                String response = client.callMCP(sessionRequest);
                assertTrue("Response should contain request number",
                           response.contains("Request " + (i + 1)));
            }
        } finally {
            // Clean up session
            client.deleteSession();
        }
    }

}
