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

import static io.openliberty.mcp.internal.fat.suite.McpAuthServerSuite.serverLifecycle;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
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

import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.cancellationApp.CancellationTools;
import io.openliberty.mcp.internal.fat.utils.McpClient;
import io.openliberty.mcp.internal.fat.utils.McpClient.StateMode;
import io.openliberty.mcp.internal.fat.utils.TestConstants;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

@RunWith(FATRunner.class)
public class AuthCancellationTest extends FATServletClient {

    private static final String APP_NAME = "cancellationTest";

    // Server is managed by McpAuthServerSuite — do NOT add @Server or copy the field here.
    private static ExecutorService executor;

    @Rule
    public McpClient client = new McpClient(serverLifecycle.getServer(), "/" + APP_NAME, StateMode.STATEFUL, "BobTheAdmin", "testpassword");

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(serverLifecycle.getServer(), "/" + APP_NAME);

    @BeforeClass
    public static void setup() throws Exception {
        serverLifecycle.getServer().addIgnoredErrors(List.of("CWMCM0010E"));
        serverLifecycle.getServer().setMarkToEndOfLog();
        WebArchive war = ShrinkWrap.create(WebArchive.class, APP_NAME + ".war")
                                   .addPackage(CancellationTools.class.getPackage())
                                   .addPackage(ToolStatus.class.getPackage());

        serverLifecycle.deployDropinApp(war);
        assertNotNull(serverLifecycle.getServer().waitForStringInLogUsingMark("MCP server endpoint: .*/mcp$"));

        executor = Executors.newSingleThreadExecutor();
    }

    @AfterClass
    public static void teardown() throws Exception {
        serverLifecycle.undeployDropinApp(APP_NAME);
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

        // Send cancellation with the wrong user — expect 403 Forbidden
        client.callMCPNotificationWithBasicAuthForbiddenErrorExpected(cancellationRequestNotification, "testuser", "testpassword");

        // Wait briefly to confirm the tool is still running (it was not cancelled)
        TimeUnit.MILLISECONDS.sleep(TestConstants.NEGATIVE_TIMEOUT_MS);

        // Test is done — signal the tool to exit its polling loop
        toolStatus.signalShouldEnd(LATCH_NAME);

        String expectedResponseString = """
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"If this String is returned, then the tool was not cancelled","type":"text"}],"isError":false}}
                        """;

        String responseA = futureA.get(TestConstants.POSITIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        JSONAssert.assertEquals(expectedResponseString, responseA, true);

        assertTrue(!serverLifecycle.getServer().findStringsInLogs("Exception: The tool cannot be cancelled as the calling user is not authorized.").isEmpty());

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
                        {"id":"2","jsonrpc":"2.0","result":{"content":[{"text":"Operation was cancelled", "type":"text"}],"isError":true}}
                                        """;

        String responseA = futureA.get(TestConstants.POSITIVE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        JSONAssert.assertEquals(expectedResponseString, responseA, true);

    }

}
