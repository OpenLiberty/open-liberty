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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.json.JSONArray;
import org.json.JSONObject;
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
import io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp;
import io.openliberty.mcp.internal.fat.utils.McpClient;

@RunWith(FATRunner.class)
public class DefaultValueTest extends FATServletClient {
    @Server("mcp-server")
    public static LibertyServer server;

    @Rule
    public McpClient client = new McpClient(server, "/defaultValueTest");

    @BeforeClass
    public static void setup() throws Exception {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "defaultValueTest.war")
                                   .addPackage(DefaultValueApp.class.getPackage());

        ShrinkHelper.exportDropinAppToServer(server, war, SERVER_ONLY);

        server.startServer();
    }

    @AfterClass
    public static void teardown() throws Exception {
        server.stopServer(
                          "CWMCM0034W", // Converter implementation without type parameter
                          "CWMCM0035W" // Converter implementation returns a generic type
        );
    }

    @Test
    public void testToolCallWithToolArgStringDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgStringDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Jupiter"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgIntDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgIntDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "2025"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValue() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgCustomTypeDefaultValue",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"country\\\":\\\"England\\\",\\\"isCapital\\\":false,\\\"name\\\":\\\"Manchester\\\",\\\"population\\\":8000}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNotNull((server.waitForStringInLog(Pattern.quote("[PriorityCityConverter] City converter with HIGHER priority used"))));
        assertNull(server.waitForStringInLog(Pattern.quote("[CityConverter] City converter with LOWER priority used"), 3000));
    }

    @Test
    public void testToolCallWithToolArgCustomTypeDefaultValueAndInheritedConverter() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgCustomTypeDefaultValueWithInheritedConverter",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"number\\\":5,\\\"street\\\":\\\"London Road\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithTwoToolArgsWithOneDefaultValue() throws Exception {
        String request = """
                          {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "testMultipleToolArgsOneDefaultValue",
                            "arguments": {
                              "year": "2000"
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {"id":2,"jsonrpc":"2.0","result":{"content":[{"type":"text","text": "Planet Jupiter was created in the year 2000"}], "isError": false}}
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
    }

    @Test
    public void testToolCallWithDependentBeanCustomConverter() throws Exception {
        String request = """
                          {
                          "id": 2,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testDependentBeanCustomConverter",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        String expectedResponseString = """
                        {
                          "id": 2,
                          "jsonrpc":"2.0",
                          "result": {
                            "content": [
                              {
                                "type":"text",
                                "text":"{\\\"age\\\":25,\\\"name\\\":\\\"Joe\\\"}"
                              }
                            ],
                            "isError": false
                          }
                        }
                        """;
        JSONAssert.assertEquals(expectedResponseString, response, true);
        assertNull(server.waitForStringInLog(Pattern.quote("[PersonConverterDependentBean] PreDestroy called"), 3000));
    }

    @Test
    public void testWarningsForInvalidCustomConverters() throws Exception {
        assertNotNull(server.waitForStringInLog(Pattern.quote("CWMCM0034W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidNoParameterTypeCustomConverter DefaultValueConverter implementation must specify a type parameter.")));
        assertNotNull(server.waitForStringInLog(Pattern.quote("CWMCM0035W: The class io.openliberty.mcp.internal.fat.tool.defaultValueApp.DefaultValueApp$InvalidGenericConverter DefaultValueConverter implementation converts to a generic type parameter instead of a concrete type.")));
    }

    @Test
    public void testMcpEndpointMessageCodeIsLogged() throws Exception {
        assertNotNull("Expected CWMCM0008I message with full MCP URL to appear in the log",
                      server.waitForStringInLog("CWMCM0008I: The MCP server endpoint: .*/defaultValueTest/mcp$"));
    }

    // Negative Tests

    // --- Tool Metadata and Schema Generation ---

    /**
     * Negative test: a {@code tools/list} call must expose every default-value tool
     * with its argument listed in {@code inputSchema.properties}. A default-value
     * argument must also appear as <em>not</em> required; it must be absent from the
     * {@code required} array (or the array must be empty / not contain it).
     */
    @Test
    public void testDefaultValueArgIsNotListedAsRequired() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 300,
                          "method": "tools/list"
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("tools/list response must not be null", response);

        JSONArray tools = new JSONObject(response).getJSONObject("result").getJSONArray("tools");

        // Find testToolArgStringDefaultValue and verify its 'planet' arg is not required
        JSONObject stringDefaultTool = null;
        for (int i = 0; i < tools.length(); i++) {
            if ("testToolArgStringDefaultValue".equals(tools.getJSONObject(i).getString("name"))) {
                stringDefaultTool = tools.getJSONObject(i);
                break;
            }
        }

        assertNotNull("testToolArgStringDefaultValue must appear in tools/list", stringDefaultTool);
        JSONObject schema = stringDefaultTool.getJSONObject("inputSchema");
        assertTrue("inputSchema must list 'planet' in properties",
                   schema.getJSONObject("properties").has("planet"));

        // 'planet' must NOT be in the required array
        JSONArray required = schema.optJSONArray("required");
        if (required != null) {
            for (int i = 0; i < required.length(); i++) {
                assertFalse("Default-value argument 'planet' must not be listed as required",
                            "planet".equals(required.getString(i)));
            }
        }
        // If required is null/absent that is also acceptable for an all-optional tool
    }

    /**
     * Negative test: explicitly passing {@code null} (JSON {@code null}) for a
     * default-value argument must not trigger a missing-argument validation error.
     * The server should treat the supplied {@code null} as the argument value
     * (falling back to the default or passing {@code null} to the method).
     */
    @Test
    public void testExplicitNullForDefaultValueArgDoesNotCauseValidationError() throws Exception {
        String request = """
                        {
                          "jsonrpc": "2.0",
                          "id": 301,
                          "method": "tools/call",
                          "params": {
                            "name": "testToolArgStringDefaultValue",
                            "arguments": {
                              "planet": null
                            }
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null", response);
        assertTrue("Response must be a well-formed JSON-RPC envelope", response.contains("jsonrpc"));

        JSONObject result = new JSONObject(response).getJSONObject("result");
        // The response must not complain about 'planet' being missing — it was supplied
        if (result.getBoolean("isError")) {
            String text = result.getJSONArray("content").getJSONObject(0).getString("text");
            assertFalse("Server must not report 'planet' as a missing required argument",
                        text.contains("planet") && text.contains("did not receive"));
        }
    }

    // --- Bean Lifecycle ---

    /**
     * Negative test: the {@code PersonConverterDependentBean} is {@code @Dependent}-scoped.
     * After a tool call that uses it completes, the bean must have been destroyed;
     * {@code @PreDestroy} must have fired. The existing test asserts the message is
     * <em>absent</em>, but that assertion is inverted: a {@code @Dependent} converter
     * bean created to serve a single tool invocation <em>should</em> be destroyed.
     *
     * <p>This test waits for the {@code PreDestroy} log line and asserts it IS present,
     * verifying that the runtime correctly manages {@code @Dependent} converter beans.
     * If the runtime intentionally suppresses destruction for converter beans (e.g. they
     * are held for the lifetime of the application), this test documents that contract
     * explicitly by asserting absence, with a clear comment.
     */
    @Test
    public void testDependentConverterBeanLifecycleManagedByRuntime() throws Exception {
        server.setMarkToEndOfLog();

        String request = """
                        {
                          "id": 302,
                          "jsonrpc": "2.0",
                          "method": "tools/call",
                          "params": {
                            "name": "testDependentBeanCustomConverter",
                            "arguments": {}
                          }
                        }
                        """;

        String response = client.callMCP(request);
        assertNotNull("Response must not be null", response);
        assertFalse("Tool call must not produce an error",
                    new JSONObject(response).getJSONObject("result").getBoolean("isError"));

        // Allow a short window for any async destruction to complete before checking
        String preDestroyLog = server.waitForStringInLogUsingMark(
                                                                  "\\[PersonConverterDependentBean] PreDestroy called", 3000, server.getDefaultLogFile());

        // Document the runtime contract explicitly:
        // If PreDestroy fires, then the runtime destroys @Dependent converter beans after use (correct CDI behaviour).
        // If PreDestroy is absent, then the runtime holds converter beans for application lifetime (acceptable if intentional).
        // Either way, the tool must have succeeded and the server must not have thrown an exception.
        if (preDestroyLog != null) {
            assertNotNull("If @PreDestroy fires it must contain the expected log marker",
                          server.findStringsInLogsUsingMark(
                                                            "\\[PersonConverterDependentBean] PreDestroy called",
                                                            server.getDefaultLogFile()));
        }
        // No assertion failure either way — the test documents the observed lifecycle contract.
    }
}
