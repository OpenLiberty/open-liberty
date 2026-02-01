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

import static com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions.SERVER_ONLY;
import static org.junit.Assert.assertTrue;

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
import io.openliberty.mcp.internal.fat.tool.cancellationApp.CancellationTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.StateMode;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AuthCancellationTest extends FATServletClient {

    @Server("mcp-server-auth")
    public static LibertyServer server;
    private static ExecutorService executor;

    @Rule
    public McpClient client = new McpClient(server, "/cancellationTest", StateMode.STATEFUL, "BobTheAdmin", "testpassword");

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/cancellationTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "cancellationTest.war")
                                   .addPackage(CancellationTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0010E" //Internal server error
        );
    }

    @AfterClass
    public static void shutdownExecutor() {
        executor.shutdown();
    }

    @Test
    public void testCancellationToolFailWithMismatchUserWithSession() throws Exception {
        final String LATCH_NAME = "numId1";

        Callable<String> threadCallingToolA = () -> {
            try {
                String request = """
                                  {
                                  "jsonrpc": "2.0",
                                  "id": "2",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "cancellationTool",
                                    "arguments": {
                                      "latchName": "numId1"
                                    }
                                  }
                                }
                                """;

                return client.callMCPwithBasicAuth(request, "BobTheAdmin", "testpassword");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Future<String> futureA = executor.submit(threadCallingToolA);

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

        toolStatus.awaitStarted(LATCH_NAME);

        client.callMCPNotificationWithBasicAuthForbiddenErrorExpected(cancellationRequestNotification, "testuser", "testpassword");

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"If this String is returned, then the tool was not cancelled","type":"text"}],"isError":false}}
                        """;

        String responseA = futureA.get();
        JSONAssert.assertEquals(expectedResponseString, responseA, true);

        assertTrue(!server.findStringsInLogs("Exception: The tool cannot be cancelled as the calling user is not authorized.").isEmpty());

    }

    @Test
    public void testCancellationToolWithAuthorizedUserWithSession() throws Exception {
        final String LATCH_NAME = "numId2";

        Callable<String> threadCallingToolA = () -> {
            try {
                String request = """
                                  {
                                  "jsonrpc": "2.0",
                                  "id": "2",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "cancellationTool",
                                    "arguments": {
                                      "latchName": "numId2"
                                    }
                                  }
                                }
                                """;

                return client.callMCPwithBasicAuth(request, "BobTheAdmin", "testpassword");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        Future<String> futureA = executor.submit(threadCallingToolA);

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
        // Call AwaitToolServlet to wait for the tool to start running. Adds path param "numId" to specify which countdown latch to use
        toolStatus.awaitStarted(LATCH_NAME);

        client.callMCPNotificationWithBasicAuth(cancellationRequestNotification, "BobTheAdmin", "testpassword");

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"An internal server error occurred while running the tool.", "type":"text"}],"isError":true}}
                                        """;

        String responseA = futureA.get(10, TimeUnit.SECONDS);
        JSONAssert.assertEquals(expectedResponseString, responseA, true);

    }

}
