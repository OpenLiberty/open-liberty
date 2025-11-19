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
import static io.openliberty.mcp.internal.fat.utils.TestConstants.NEGATIVE_TIMEOUT;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
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
import componenttest.topology.utils.HttpRequest;
import io.openliberty.mcp.internal.fat.tool.basicToolApp.BasicTools;
import io.openliberty.mcp.internal.fat.tool.cancellationApp.CancellationTools;
import io.openliberty.mcp.internal.fat.utils.ToolStatus;
import io.openliberty.mcp.internal.fat.utils.ToolStatusClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class StatelessModeTest extends FATServletClient {

    private static final String ACCEPT_HEADER = "application/json, text/event-stream";
    private static final String MCP_PROTOCOL_HEADER = "MCP-Protocol-Version";
    private static final String MCP_PROTOCOL_VERSION = "2025-06-18";

    @Server("mcp-stateless-server")
    public static LibertyServer server;
    private static ExecutorService executor;

    @Rule
    public ToolStatusClient toolStatus = new ToolStatusClient(server, "/statelessModeTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "statelessModeTest.war")
                                   .addPackage(BasicTools.class.getPackage())
                                   .addPackage(CancellationTools.class.getPackage())
                                   .addPackage(StatelessModeTools.class.getPackage())
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

    private static final String ENDPOINT = "/statelessModeTest/mcp";

    @Test
    public void testInitializeDoesNotReturnSessionIdInStatelessMode() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """;

        HttpRequest httpRequest = new HttpRequest(server, ENDPOINT)
                                                                   .requestProp("Accept", ACCEPT_HEADER)
                                                                   .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                   .jsonBody(request)
                                                                   .method("POST")
                                                                   .expectCode(200);

        String response = httpRequest.run(String.class);

        assertFalse("Expected no session ID in response body", response.contains("Mcp-Session-Id"));

        String sessionIdHeader = httpRequest.getResponseHeader("Mcp-Session-Id");
        assertNull("Expected no session ID header in stateless mode", sessionIdHeader);
    }

    @Test
    public void testToolCallSucceedsWithoutSessionIdInStatelessMode() throws Exception {
        String request = """
                            {
                                "jsonrpc": "2.0",
                                "id": "1",
                                "method": "tools/call",
                                "params": {
                                    "name": "textContentTool",
                                    "arguments": {
                                      "input": "hello"
                                      }
                                }
                            }
                        """;

        String response = new HttpRequest(server, ENDPOINT)
                                                           .requestProp("Accept", ACCEPT_HEADER)
                                                           .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                           .jsonBody(request)
                                                           .method("POST")
                                                           .run(String.class);

        assertTrue("Tool response should contain echoed text", response.contains("hello"));
    }

    @Test
    public void testCancellationToolWithCancellableParameterAndCancellationRequestWithStringId_inStatelessMode() throws Exception {
        final String LATCH_NAME = "strId";

        Callable<String> threadCallingTool = () -> {

            try {
                String request = """
                                    {
                                      "jsonrpc": "2.0",
                                      "id": "1",
                                      "method": "tools/call",
                                      "params": {
                                        "name": "cancellationToolForStatelessMinimalWait",
                                        "arguments": {
                                          "latchName": "strId"
                                        }
                                      }
                                    }
                                """;
                return new HttpRequest(server, ENDPOINT)
                                                        .requestProp("Accept", ACCEPT_HEADER)
                                                        .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                        .jsonBody(request)
                                                        .method("POST")
                                                        .expectCode(200)
                                                        .run(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<String> future = executor.submit(threadCallingTool);

        toolStatus.awaitStarted(LATCH_NAME);

        new HttpRequest(server, ENDPOINT)
                                         .requestProp("Accept", ACCEPT_HEADER)
                                         .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                         .jsonBody("""
                                                               {
                                                               "jsonrpc": "2.0",
                                                               "method": "notifications/cancelled",
                                                               "params": {
                                                                 "requestId": "2",
                                                                 "reason": "no longer needed"
                                                               }
                                                             }
                                                         """)
                                         .method("POST")
                                         .expectCode(202)
                                         .run(String.class);

        // Short delay to allow the server time to process/ignore cancellation
        TimeUnit.NANOSECONDS.sleep(NEGATIVE_TIMEOUT.toNanos());

        // Manually release the tool latch — since cancellation won't do it in stateless mode
        toolStatus.signalShouldEnd(LATCH_NAME);

        // Tool should complete normally
        String response = future.get(15, TimeUnit.SECONDS);

        String expected = """
                            {"id":"1","jsonrpc":"2.0","result":{"content":[{"type":"text", "text": "If this String is returned, then the tool was not cancelled"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testCancellationNotificationAcceptedButIgnored_inStatelessMode() throws Exception {
        final String LATCH_NAME = "strId";

        Callable<String> toolCall = () -> {
            try {
                String request = """
                                {
                                  "jsonrpc": "2.0",
                                  "id": "1",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "cancellationToolForStatelessMinimalWait",
                                    "arguments": {
                                      "latchName": "strId"
                                    }
                                  }
                                }
                                """;
                return new HttpRequest(server, ENDPOINT)
                                                        .requestProp("Accept", ACCEPT_HEADER)
                                                        .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                        .jsonBody(request)
                                                        .method("POST")
                                                        .expectCode(200)
                                                        .run(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<String> future = executor.submit(toolCall);

        toolStatus.awaitStarted(LATCH_NAME);

        new HttpRequest(server, ENDPOINT)
                                         .requestProp("Accept", ACCEPT_HEADER)
                                         .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                         .jsonBody("""
                                                               {
                                                               "jsonrpc": "2.0",
                                                               "method": "notifications/cancelled",
                                                               "params": {
                                                                 "requestId": "2",
                                                                 "reason": "no longer needed"
                                                               }
                                                             }
                                                         """)
                                         .method("POST")
                                         .expectCode(202)
                                         .run(String.class);

        // Short delay to allow the server time to process/ignore cancellation
        TimeUnit.NANOSECONDS.sleep(NEGATIVE_TIMEOUT.toNanos());

        // Now release the tool manually
        toolStatus.signalShouldEnd(LATCH_NAME);

        String response = future.get(15, TimeUnit.SECONDS);

        String expected = """
                            {"id":"1","jsonrpc":"2.0","result":{"content":[{"type":"text", "text": "If this String is returned, then the tool was not cancelled"}],"isError":false}}
                        """;

        JSONAssert.assertEquals(expected, response, true);
    }

    @Test
    public void testInvalidCancellationNotificationReturns202_inStatelessMode() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "method": "notifications/cancelled",
                          "params": {
                            "requestId": "non-existent-id",
                            "reason": "testing"
                          }
                        }
                        """;

        String response = new HttpRequest(server, ENDPOINT)
                                                           .requestProp("Accept", ACCEPT_HEADER)
                                                           .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                           .jsonBody(request)
                                                           .method("POST")
                                                           .expectCode(202)
                                                           .run(String.class);

        assertTrue("Cancellation for unknown ID should return 202", response == null || response.isEmpty());
    }

    @Test
    public void testSessionDeleteReturns404InStatelessMode() throws Exception {
        String response = new HttpRequest(server, ENDPOINT)
                                                           .requestProp("Accept", ACCEPT_HEADER)
                                                           .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                           .method("DELETE")
                                                           .expectCode(404)
                                                           .run(String.class);

        assertThat("Expected error about session not found",
                   response, containsString("Error 404: Session not found"));

    }

    @Test
    public void testRequestWithSessionIdReturns404InStatelessMode() throws Exception {
        String payload = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "echoTool",
                            "arguments": {
                              "message": "test"
                            }
                          }
                        }
                        """;

        String response = new HttpRequest(server, ENDPOINT)
                                                           .requestProp("Accept", ACCEPT_HEADER)
                                                           .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                           .requestProp("Mcp-Session-Id", "dummy-session-id")
                                                           .jsonBody(payload)
                                                           .method("POST")
                                                           .expectCode(404)
                                                           .run(String.class);

        assertThat("Expected error about invalid session",
                   response, containsString("Invalid or Expired Session Id"));

    }

    @Test
    public void testDuplicateRequestIdAllowedInStatelessMode() throws Exception {
        final String LATCH_NAME = "statelessDuplicate";

        // Prepare async thread for the first request
        Callable<String> firstCall = () -> {
            try {
                String firstPayload = """
                                {
                                  "jsonrpc": "2.0",
                                  "id": "1",
                                  "method": "tools/call",
                                  "params": {
                                    "name": "blockingEcho",
                                    "arguments": {
                                      "input": "stateless-mode",
                                      "latchName": "statelessDuplicate"
                                    }
                                  }
                                }
                                """;

                return new HttpRequest(server, ENDPOINT)
                                                        .requestProp("Accept", ACCEPT_HEADER)
                                                        .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                        .jsonBody(firstPayload)
                                                        .method("POST")
                                                        .expectCode(200)
                                                        .run(String.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Future<String> future = executor.submit(firstCall);

        // Wait until first request has started running
        toolStatus.awaitStarted(LATCH_NAME);

        // Second request with SAME ID while first one is still blocked
        String secondPayload = """
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "tools/call",
                          "params": {
                            "name": "blockingEcho",
                            "arguments": {
                              "input": "stateless-mode",
                              "latchName": "statelessDuplicate"
                            }
                          }
                        }
                        """;

        // Send second request
        Future<String> secondFuture = executor.submit(() -> new HttpRequest(server, ENDPOINT)
                                                                                             .requestProp("Accept", ACCEPT_HEADER)
                                                                                             .requestProp(MCP_PROTOCOL_HEADER, MCP_PROTOCOL_VERSION)
                                                                                             .jsonBody(secondPayload)
                                                                                             .method("POST")
                                                                                             .expectCode(200)
                                                                                             .run(String.class));

        // Immediately release latch so both can complete
        toolStatus.signalShouldEnd(LATCH_NAME);

        // Then validate responses
        String expectedResponseString = """
                        {"id":"1","jsonrpc":"2.0","result":{"content":[{"type":"text","text":"stateless-mode"}],"isError":false}}
                        """;

        String secondResponse = secondFuture.get(10, TimeUnit.SECONDS);
        JSONAssert.assertEquals(expectedResponseString, secondResponse, true);

        String firstResponse = future.get(10, TimeUnit.SECONDS);
        JSONAssert.assertEquals(expectedResponseString, firstResponse, true);
    }

}