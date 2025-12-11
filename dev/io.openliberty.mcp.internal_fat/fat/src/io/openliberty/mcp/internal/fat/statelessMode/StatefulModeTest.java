/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.statelessMode;

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;

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
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class StatefulModeTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;
    private static ExecutorService executor;

    @Rule
    public McpClient client = new McpClient(server, "/statefulModeTest");

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/statefulModeTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "statefulModeTest.war")
                                   .addPackage(StatelessModeTools.class.getPackage())
                                   .addPackage(BasicTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer();
    }

    @AfterClass
    public static void shutdownExecutor() {
        executor.shutdown();
    }

    // Add a test with same id and it should pass

    // so this test should run the request parallely and the way cancellation test uses latches to delay the request for 10 second to cancell.
    // I need similar functionality but in my case I need an end latch and my first request shoud wait for the end latch (while there is a duplicate request running while this is happening

    @Test
    public void testDuplicateRequestIdNotAllowedInStatefulMode() throws Exception {
        final String LATCH_NAME = "statefulDuplicate1";

        // First request: blocks on tool latch
        String firstPayload = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "blockingEcho",
                            "arguments": {
                              "input": "stateful-mode",
                              "latchName": "%s"
                            }
                          }
                        }
                        """, LATCH_NAME);

        Future<String> future = executor.submit(() -> client.callMCP(firstPayload));

        toolStatus.awaitStarted(LATCH_NAME);

        // Second request with SAME ID
        String secondPayload = """
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "stateful-mode"
                            }
                          }
                        }
                        """;

        String expectedSecondResponse = """
                        {
                            "id":"2",
                            "jsonrpc":"2.0",
                            "error": {
                                "code": -32602,
                                "message": "Invalid params",
                                "data": "CWMCM0008E: A request with the 2 ID is in progress."
                            }
                        }
                        """;
        String secondResponse = client.callMCP(secondPayload);
        JSONAssert.assertEquals(expectedSecondResponse, secondResponse, true);

        toolStatus.signalShouldEnd(LATCH_NAME);

        String expectedResponse = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"stateful-mode"}],"isError":false}}
                        """;
        String firstResponse = future.get(10, TimeUnit.SECONDS);
        JSONAssert.assertEquals(expectedResponse, firstResponse, true);
    }
}