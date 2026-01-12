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
import static org.junit.Assert.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
                                "text": "CWMCM0011E: An internal server error occurred while running the tool."
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
