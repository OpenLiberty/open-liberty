/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertNotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolApp.AsyncTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AsyncToolCancellationTest extends FATServletClient {

    @Server("mcp-server-async")
    public static LibertyServer server;
    private static ExecutorService executor;
    private static final String EXPECTED_ERROR = "OperationCancellationException";

    @Rule
    public McpClient client = new McpClient(server, "/asyncToolCancellationTest");

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/asyncToolCancellationTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolCancellationTest.war")
                                   .addPackage(AsyncTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
        executor = Executors.newSingleThreadExecutor();
        assertNotNull(server.waitForStringInLog("MCP server endpoint: .*/mcp$"));
    }

    @AfterClass
    public static void teardown() throws Exception {
        executor.shutdown();
        server.stopServer(EXPECTED_ERROR);
    }

    @Test
    public void testCancellationToolAsync() throws Exception {
        final String latchName = "testCancellationToolAsync";

        Callable<String> threadCallingTool = () -> {
            try {
                String request = """
                                  {
                                  "jsonrpc": "2.0",
                                  "id": "2",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "asyncCancellationTool",
                                    "arguments": {
                                      "latchName": "testCancellationToolAsync"
                                    }
                                  }
                                }
                                """;

                return client.callMCP(request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<String> future = executor.submit(threadCallingTool);

        String cancellationRequestNotification = """
                          {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "2",
                            "reason": "no longer needed"
                          }
                        }
                        """;

        toolStatus.awaitStarted(latchName);

        client.callMCPNotification(server, "/asyncToolCancellationTest", cancellationRequestNotification);

        String response = future.get(10, TimeUnit.SECONDS);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"CWMCM0011E: An internal server error occurred while running the tool.", "type":"text"}],"isError":true}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }
}
