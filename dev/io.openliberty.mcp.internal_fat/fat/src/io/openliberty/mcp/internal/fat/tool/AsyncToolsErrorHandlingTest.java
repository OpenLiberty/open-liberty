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

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.ShrinkHelper.DeployOptions;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.FATServletClient;
import io.openliberty.mcp.internal.fat.tool.asyncToolErrorHandlingApp.AsyncErrorHandlingTools;
import io.openliberty.mcp.internal.fat.tool.asyncToolErrorHandlingApp.NonBusinessException;
import io.openliberty.mcp.internal.fat.utils.McpClient;

/**
 *
 */
@RunWith(FATRunner.class)
public class AsyncToolsErrorHandlingTest extends FATServletClient {

    @Server("mcp-server-async")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/asyncToolErrorHandling");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "asyncToolErrorHandling.war")
                                   .addPackage(AsyncErrorHandlingTools.class.getPackage());
        ShrinkHelper.exportDropinAppToServer(server, war, DeployOptions.SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer("CWMCM0010E"); // Tool threw non-business exception
    }

    @Before
    public void markBeforeEachTest() throws Exception {
        server.setMarkToEndOfLog();
    }

    // Negative Tests

    // --- Tool Metadata and Schema Generation ---

    /**
     * Negative test: a {@code tools/list} call must report that both {@code exception}
     * and {@code failureMechanism} are required arguments in {@code asyncErrorTool}'s
     * {@code inputSchema}; neither may be absent from the {@code required} array.
     */
    @Test
    public void testAsyncErrorToolSchemaListsBothArgumentsAsRequired() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 100,
                          "method": "tools/list"
                        }
                        """;

        String response = client.callMCP(request);
        assertFalse("tools/list response must not be empty", response.isEmpty());

        JSONObject jsonResponse = new JSONObject(response);
        JSONArray tools = jsonResponse.getJSONObject("result").getJSONArray("tools");

        JSONObject asyncErrorToolDescriptor = null;
        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            if ("asyncErrorTool".equals(tool.getString("name"))) {
                asyncErrorToolDescriptor = tool;
                break;
            }
        }

        assertNotNull("asyncErrorTool must appear in tools/list", asyncErrorToolDescriptor);

        JSONObject schema = asyncErrorToolDescriptor.getJSONObject("inputSchema");
        assertTrue("inputSchema must contain a 'required' array", schema.has("required"));

        JSONArray required = schema.getJSONArray("required");
        boolean foundException = false;
        boolean foundMechanism = false;
        for (int i = 0; i < required.length(); i++) {
            String arg = required.getString(i);
            if ("exception".equals(arg))
                foundException = true;
            if ("failureMechanism".equals(arg))
                foundMechanism = true;
        }
        assertTrue("'exception' must be listed as required in asyncErrorTool's inputSchema", foundException);
        assertTrue("'failureMechanism' must be listed as required in asyncErrorTool's inputSchema", foundMechanism);
    }

    // --- Monitoring and Management ---

    /**
     * Negative test: supplying an unknown {@code exception} value (not one of the
     * recognised class names) must return a well-formed {@code isError:true} JSON-RPC
     * result; the server must not crash or produce an HTTP 500.
     * The tool's default branch throws a {@link io.openliberty.mcp.tools.ToolCallException},
     * so the response content must be the user-visible exception message.
     */
    @Test
    public void testUnknownExceptionTypeReturnsUserErrorNotServerCrash() throws Exception {
        String response = callTool("UnknownExceptionType", "THROWN");

        assertFalse("Response must not be empty", response.isEmpty());
        assertTrue("Response must be a well-formed JSON-RPC envelope", response.contains("jsonrpc"));

        JSONObject result = new JSONObject(response).getJSONObject("result");
        assertTrue("Result must be flagged as isError:true", result.getBoolean("isError"));

        String text = result.getJSONArray("content").getJSONObject(0).getString("text");
        assertFalse("Error text must not be an HTTP-level error page", text.contains("<html"));
        // The tool throws ToolCallException("Invalid exception type: …") for unknown names
        assertTrue("Error text must contain the unrecognised exception name",
                   text.contains("UnknownExceptionType") || text.contains("Invalid exception type"));
    }

    // --- Bean Lifecycle ---

    /**
     * Negative test: when the async tool fails via {@code FAILED} (returns a
     * {@code CompletableFuture.failedStage}), the CDI container must not keep any
     * stale bean state; a second call with a different failure mechanism must still
     * succeed independently. This is verified by calling twice in succession and
     * asserting both produce well-formed {@code isError:true} responses (no session
     * contamination or cached bean failure).
     */
    @Test
    public void testSuccessiveFailedStageCallsRemainIsolated() throws Exception {
        String firstResponse = callTool("BusinessException", "FAILED");
        String secondResponse = callTool("ToolCallException", "FAILED");

        assertUserError(firstResponse, "BusinessException");
        assertUserError(secondResponse, "ToolCallException");
    }

    @Test
    public void testAsyncToolThrowsExceptionWrapped() throws Exception {
        String response = callTool("BusinessException", "THROWN");
        assertUserError(response, "BusinessException");
    }

    @Test
    public void testAsyncToolThrowsExceptionWrappedBySuperclass() throws Exception {
        String response = callTool("SpecificBusinessException", "THROWN");
        assertUserError(response, "SpecificBusinessException");
    }

    @Test
    public void testAsyncToolThrowsExceptionNotWrapped() throws Exception {
        String response = callTool("NonBusinessException", "THROWN");
        assertInternalError(response);
        assertErrorLogged(NonBusinessException.class, "NonBusinessException");
    }

    @Test
    public void testAsyncToolThrowsToolCallException() throws Exception {
        String response = callTool("ToolCallException", "THROWN");
        assertUserError(response, "ToolCallException");
    }

    @Test
    public void testAsyncToolThrowsToolCallUnauthorizedException() throws Exception {
        callToolExpect403("ToolCallUnauthorizedException", "THROWN");
    }

    @Test
    public void testAsyncToolFailsWithToolCallUnauthorizedException() throws Exception {
        callToolExpect403("ToolCallUnauthorizedException", "FAILED");
    }

    @Test
    public void testAsyncToolFailsWithDelayedToolCallUnauthorizedException() throws Exception {
        callToolExpect403("ToolCallUnauthorizedException", "FAILED_DELAY");
    }

    @Test
    public void testAsyncToolMultistageFailsWithToolCallUnauthorizedException() throws Exception {
        callToolExpect403("ToolCallUnauthorizedException", "FAILED_MULTISTAGE");
    }

    @Test
    public void testAsyncToolFailsWithExceptionWrapped() throws Exception {
        String response = callTool("BusinessException", "FAILED");
        assertUserError(response, "BusinessException");
    }

    @Test
    public void testAsyncToolFailsWithExceptionWrappedBySuperclass() throws Exception {
        String response = callTool("SpecificBusinessException", "FAILED");
        assertUserError(response, "SpecificBusinessException");
    }

    @Test
    public void testAsyncToolFailsWithExceptionNotWrapped() throws Exception {
        String response = callTool("NonBusinessException", "FAILED");
        assertInternalError(response);
        assertErrorLogged(NonBusinessException.class, "NonBusinessException");
    }

    @Test
    public void testAsyncToolFailsWithToolCallException() throws Exception {
        String response = callTool("ToolCallException", "FAILED");
        assertUserError(response, "ToolCallException");
    }

    @Test
    public void testAsyncToolFailsWithDelayedExceptionWrapped() throws Exception {
        String response = callTool("BusinessException", "FAILED_DELAY");
        assertUserError(response, "BusinessException");
    }

    @Test
    public void testAsyncToolFailsWithDelayedExceptionWrappedBySuperclass() throws Exception {
        String response = callTool("SpecificBusinessException", "FAILED_DELAY");
        assertUserError(response, "SpecificBusinessException");
    }

    @Test
    public void testAsyncToolFailsWithDelayedExceptionNotWrapped() throws Exception {
        String response = callTool("NonBusinessException", "FAILED_DELAY");
        assertInternalError(response);
        assertErrorLogged(NonBusinessException.class, "NonBusinessException");
    }

    @Test
    public void testAsyncToolFailsWithDelayedToolCallException() throws Exception {
        String response = callTool("NonBusinessException", "FAILED_DELAY");
        assertInternalError(response);
        assertErrorLogged(NonBusinessException.class, "NonBusinessException");
    }

    @Test
    public void testAsyncToolMultistageFailsWithExceptionWrapped() throws Exception {
        String response = callTool("BusinessException", "FAILED_MULTISTAGE");
        assertUserError(response, "BusinessException");
    }

    @Test
    public void testAsyncToolMultistageFailsWithExceptionWrappedBySuperclass() throws Exception {
        String response = callTool("SpecificBusinessException", "FAILED_MULTISTAGE");
        assertUserError(response, "SpecificBusinessException");
    }

    @Test
    public void testAsyncToolMultistageFailsWithExceptionNotWrapped() throws Exception {
        String response = callTool("NonBusinessException", "FAILED_MULTISTAGE");
        assertInternalError(response);
        assertErrorLogged(NonBusinessException.class, "NonBusinessException");
    }

    @Test
    public void testAsyncToolMultistageFailsWithToolCallException() throws Exception {
        String response = callTool("ToolCallException", "FAILED_MULTISTAGE");
        assertUserError(response, "ToolCallException");
    }

    private static enum FailureMechanism {
        /** Method throws exception */
        THROW,
        /** Method returns failed CompletionStage */
        FAIL,
        /** Method returns CompletionStage which later fails */
        FAIL_DELAYED
    };

    private void doTest(Class<?> exceptionToThrow, FailureMechanism failureMechanism) {}

    private void callToolExpect403(String exceptionToThrow, String failureMechanism) throws Exception {
        String request = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncErrorTool",
                            "arguments": {
                              "exception": "%s",
                              "failureMechanism": "%s"
                            }
                          }
                        }
                        """,
                                       exceptionToThrow,
                                       failureMechanism);
        McpClient.McpDetailedAuthResponse response = client.callMCPAuthorisationErrorDetailed(request);
        Log.info(AsyncToolsErrorHandlingTest.class, "callToolExpect403", response.toString());

        assertEquals(403, response.statusCode());
        assertTrue("Content-Type must be text/plain", response.contentType().contains("text/plain"));
        assertTrue("Response body must contain the exception message",
                   response.body().contains("ToolCallUnauthorizedException"));
        assertFalse("Response must not be a JSON-RPC envelope", response.body().contains("jsonrpc"));
    }

    private void assertUserError(String response, String errorMessage) {
        String expected = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "%s"
                              }
                            ],
                            "isError": true
                          }
                        }""",
                                        errorMessage);
        JSONAssert.assertEquals(expected, response, JSONCompareMode.STRICT);
    }

    private void assertInternalError(String response) {
        String expected = """
                          {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "result": {
                            "content": [
                              {
                                "type": "text",
                                "text": "An internal server error occurred while running the tool."
                              }
                            ],
                            "isError": true
                          }
                        }""";
        JSONAssert.assertEquals(expected, response, JSONCompareMode.STRICT);
    }

    private void assertErrorLogged(Class<?> exceptionClass, String error) {
        String logLine = server.waitForStringInLogUsingMark("CWMCM0010E");
        assertThat(logLine,
                   containsString("CWMCM0010E: The asyncErrorTool tool method threw an unexpected exception. The exception is " + exceptionClass.getName() + ": " + error));
    }

    private String callTool(String exceptionToThrow, String failureMechanism) throws Exception {
        String request = String.format("""
                        {
                          "jsonrpc": "2.0",
                          "id": 3,
                          "method": "tools/call",
                          "params": {
                            "name": "asyncErrorTool",
                            "arguments": {
                              "exception": "%s",
                              "failureMechanism": "%s"
                            }
                          }
                        }
                        """,
                                       exceptionToThrow,
                                       failureMechanism);

        String response = client.callMCP(request);
        Log.info(AsyncToolsErrorHandlingTest.class, "callTool", response);
        return response;
    }

}
