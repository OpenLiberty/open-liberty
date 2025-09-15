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

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.cancellationApp.CancellationTools;
import io.openliberty.mcp.internal.fat.utils.HttpTestUtils;

@RunWith(FATRunner.class)
public class CancellationTest extends FATServletClient {

    @Server("mcp-server")
    public static LibertyServer server;
    private static ExecutorService executor;

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cancellationTest.war").addPackage(CancellationTools.class.getPackage());

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

    @Test
    public void testCancellationToolWithCancellableParameterAndCancellationRequest() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);

        Callable<String> threadCallingTool = () -> {
            try {
                String request = """
                                  {
                                  "jsonrpc": "2.0",
                                  "id": "2",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "cancellationTool",
                                    "arguments": {}
                                  }
                                }
                                """;
                //make sure this tread executes first
                latch.countDown();
                return HttpTestUtils.callMCP(server, "/cancellationTest", request);
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
        //make sure the tool call request has started
        latch.await();

        // Call AwaitToolServlet to wait for the tool to start running
        new HttpRequest(server, "/cancellationTest/awaitTool").run(String.class);

        HttpTestUtils.callMCPNotification(server, "/cancellationTest", cancellationRequestNotification);

        String response = future.get(10, TimeUnit.SECONDS);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"Internal server error", "type":"text"}],"isError":true}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testCancellationToolDoesNotCancelIfCancellationIsNotRequested() throws Exception {

        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": "3",
                          "method": "tools/call",
                          "params": {
                            "name": "cancellationToolNoWait",
                            "arguments": {}
                          }
                        }
                        """;

        String response = HttpTestUtils.callMCP(server, "/cancellationTest", request);

        String expectedResponseString = """
                        {"id":"3","jsonrpc":"2.0","result":{"content":[{"type":"text", "text": "If this String is returned, then the tool was not cancelled"}],"isError":false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

}
