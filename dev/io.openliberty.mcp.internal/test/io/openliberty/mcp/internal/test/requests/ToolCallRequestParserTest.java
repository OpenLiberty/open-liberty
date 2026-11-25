/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test.requests;

import static io.openliberty.mcp.internal.McpProtocolVersion.V_2025_11_25;
import static io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCErrorCode.INVALID_PARAMS;
import static io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser.MCP_PROTOCOL_VERSION_HEADER;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

import io.openliberty.mcp.internal.ConverterRegistry;
import io.openliberty.mcp.internal.McpRequestTracker;
import io.openliberty.mcp.internal.McpTransport;
import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.config.McpConfig;
import io.openliberty.mcp.internal.exceptions.jsonrpc.HttpResponseException;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpOperationRequest;
import io.openliberty.mcp.internal.requests.parsing.McpOperationRequestParser;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.testutils.MockHttpServletRequest;
import io.openliberty.mcp.internal.testutils.MockHttpServletResponse;
import io.openliberty.mcp.internal.testutils.MockMcpConfig;
import io.openliberty.mcp.internal.testutils.TestUtils;
import io.openliberty.mcp.internal.tools.parsing.ToolCallRequestParser;
import io.openliberty.mcp.tools.ToolCallException;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.json.bind.Jsonb;
import jakarta.servlet.http.HttpServletRequest;

public class ToolCallRequestParserTest {

    private static final String ACCEPT = "Accept";
    private static final String ACCEPT_VALUE = "application/json,text/event-stream";
    private static final long TRANSPORT_TIMEOUT_MS = 30000;
    private static final Jsonb jsonb = TestUtils.createJsonb();

    private McpOperationRequestParser requestParser;
    private ToolCallRequestParser toolCallParser;
    private ToolRegistry toolRegistry;

    @Before
    public void setup() {
        McpConfig config = MockMcpConfig.createStateless();
        McpRequestTracker requestTracker = new McpRequestTracker(config);
        McpSessionStore sessionStore = new McpSessionStore(requestTracker, config);
        requestParser = new McpOperationRequestParser(sessionStore, config, jsonb);
        SchemaRegistry schemaRegistry = new SchemaRegistry();
        SchemaRegistry.set(schemaRegistry);
        toolRegistry = new ToolRegistry(schemaRegistry, jsonb);
        ConverterRegistry converterRegistry = TestUtils.createTestConverterRegistry();
        toolCallParser = new ToolCallRequestParser(toolRegistry, converterRegistry, jsonb);
    }

    @After
    public void tearDown() {
        // Clear the static override so it doesn't leak into other tests
        SchemaRegistry.set(null);
    }

    private McpOperationRequest parseRequest(String json) {
        MockHttpServletRequest req = mockRequestFor(json);
        return parseRequest(req);
    }

    private McpOperationRequest parseRequest(HttpServletRequest req) {
        try {
            return requestParser.parse(requestParser.preParse(req));
        } catch (Exception ex) {
            throw new AssertionError("Unexpected operation parsing exception: " + ex, ex);
        }
    }

    private MockHttpServletRequest mockRequestFor(String json) {
        MockHttpServletRequest req = new MockHttpServletRequest(json);
        req.addHeader(ACCEPT, ACCEPT_VALUE);
        req.addHeader(MCP_PROTOCOL_VERSION_HEADER, V_2025_11_25.getVersion());
        return req;
    }

    private McpTransport newTransport(HttpServletRequest req) {
        try {
            return new McpTransport(req, new MockHttpServletResponse(), jsonb, TRANSPORT_TIMEOUT_MS);
        } catch (Exception ex) {
            throw new AssertionError("Unexpected transport setup exception: " + ex, ex);
        }
    }

    private ToolMetadata addTool(String methodName) {
        ToolMetadata tool = TestUtils.findTool(ToolCallRequestParserTest.class, methodName);
        toolRegistry.addTool(tool);
        return tool;
    }

    @Tool
    public String echo(@ToolArg(name = "input") String input) {
        return input;
    }

    @Test
    public void testParseToolCall() {
        var echoTool = addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            },
                            "_meta": {
                              "foo": "bar",
                              "baz": 7
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "echo", partialToolCallRequest.toolName());
        assertEquals("tool", echoTool, partialToolCallRequest.tool());

        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertEquals("opRequest", opRequest, toolCallRequest.request());
        assertEquals("tool", echoTool, toolCallRequest.tool());
        assertEquals("args", Map.of("input", "Hello"), toolCallRequest.args());
        var expectedMeta = Map.of("foo", "bar",
                                  "baz", new BigDecimal(7));
        assertEquals("_meta", expectedMeta, toolCallRequest.meta());
    }

    @Test
    public void testPreParseMissingName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertNull("toolName", partialToolCallRequest.toolName());
        assertNull("tool", partialToolCallRequest.tool());
    }

    @Test
    public void testPreParseNonStringName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": 4,
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertNull("toolName", partialToolCallRequest.toolName());
        assertNull("tool", partialToolCallRequest.tool());
    }

    @Test
    public void testPreParseMissingArgs() {
        var echoTool = addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo"
                          }
                        }
                        """);
        // PreParse doesn't look at arguments
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "echo", partialToolCallRequest.toolName());
        assertEquals("tool", echoTool, partialToolCallRequest.tool());
    }

    @Test
    public void testPreParseNonObjectArgs() {
        var echoTool = addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": true
                          }
                        }
                        """);
        // PreParse doesn't look at arguments
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "echo", partialToolCallRequest.toolName());
        assertEquals("tool", echoTool, partialToolCallRequest.tool());
    }

    @Test
    public void testPreParseNoSuchTool() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "notATool",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "notATool", partialToolCallRequest.toolName());
        assertNull("tool", partialToolCallRequest.tool());
    }

    @Test
    public void testParseNonExistentTool() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "notATool",
                            "arguments": {
                              "input": "foo"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS)))
                                .and(hasProperty("data", contains("Method notATool not found"))));
    }

    @Test
    public void testParseInvalidToolName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": {},
                            "arguments": {
                              "input": "foo"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS)))
                                .and(hasProperty("data", contains(containsString("Unable to deserialize property 'name'")))));
    }

    // ---------------
    // Argument parsing tests
    // ---------------

    @Tool
    public String toolWithPrimitiveArguments(@ToolArg(name = "int") int a,
                                             @ToolArg(name = "long") long b,
                                             @ToolArg(name = "short") short c,
                                             @ToolArg(name = "byte") byte d,
                                             @ToolArg(name = "char") char e,
                                             @ToolArg(name = "float") float f,
                                             @ToolArg(name = "double") double g,
                                             @ToolArg(name = "boolean") boolean h) {
        return "OK";
    }

    @Test
    public void testParsePrimitiveArguments() {
        addTool("toolWithPrimitiveArguments");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "toolWithPrimitiveArguments",
                            "arguments": {
                              "int": 1,
                              "long": 2,
                              "short": 3,
                              "byte": 4,
                              "char": "A",
                              "float": 3.4,
                              "double": 5.6,
                              "boolean": true
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        var expectedArgs = Map.of("int", 1,
                                  "long", 2L,
                                  "short", (short) 3,
                                  "byte", (byte) 4,
                                  "char", 'A',
                                  "float", 3.4f,
                                  "double", 5.6d,
                                  "boolean", true);
        assertEquals("args", expectedArgs, toolCallRequest.args());
    }

    public static record MyObj(int a, String b) {}

    @Tool
    public String toolWithObjectArgument(@ToolArg(name = "obj") MyObj obj) {
        return "OK";
    }

    @Test
    public void testParseObjectArgument() {
        addTool("toolWithObjectArgument");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "toolWithObjectArgument",
                            "arguments": {
                              "obj": {
                                "a": 1,
                                "b": "two"
                              }
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertEquals("args", Map.of("obj", new MyObj(1, "two")), toolCallRequest.args());
    }

    @Test
    public void testParseArgOfWrongType() {
        var echoTool = addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": {}
                            }
                          }
                        }
                        """);
        // Preparse should still return tool data
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "echo", partialToolCallRequest.toolName());
        assertEquals("tool", echoTool, partialToolCallRequest.tool());

        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The input argument cannot be converted to the java.lang.String type."));
    }

    @Test
    public void testParseNonObjectArguments() {
        var echoTool = addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": "foo"
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertEquals("toolName", "echo", partialToolCallRequest.toolName());
        assertEquals("tool", echoTool, partialToolCallRequest.tool());
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS)))
                                .and(hasProperty("data", contains("Internal error: argument type mismatch"))));
    }

    @Test
    public void testParseMissingRequiredArgument() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {}
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testParseMissingArgumentsObject() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo"
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testParseExtraArgument() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello",
                              "bogus": 1
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The following arguments were passed but were not found in the method: [bogus]."));
    }

    @Test
    public void testParseExtraAndMissingArguments() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "bogus": 1
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The following arguments were passed but were not found in the method: [bogus]. "
                                                 + "The method expected the following arguments but did not receive them: [input]."));
    }

    @Test
    public void testParseNullArgument() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": null
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertTrue("args contains input", toolCallRequest.args().containsKey("input"));
        assertNull("input", toolCallRequest.args().get("input"));
    }

    @Tool
    public String optionalArgTool(@ToolArg(name = "value", required = false) String value) {
        return value;
    }

    @Test
    public void testParseOptionalArgumentOmitted() {
        addTool("optionalArgTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "optionalArgTool",
                            "arguments": {}
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        // No default value, so the argument is present in the map with a null value
        assertTrue("args contains value", toolCallRequest.args().containsKey("value"));
        assertNull("value", toolCallRequest.args().get("value"));
    }

    @Test
    public void testParseOptionalArgumentProvided() {
        addTool("optionalArgTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "optionalArgTool",
                            "arguments": {
                              "value": "given"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertEquals("args", Map.of("value", "given"), toolCallRequest.args());
    }

    @Tool
    public String defaultValueTool(@ToolArg(name = "planet", required = false, defaultValue = "Jupiter") String planet,
                                   @ToolArg(name = "year", required = false, defaultValue = "2025") int year) {
        return planet + year;
    }

    @Test
    public void testParseDefaultValuesApplied() {
        addTool("defaultValueTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueTool",
                            "arguments": {}
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertEquals("args", Map.of("planet", "Jupiter", "year", 2025), toolCallRequest.args());
    }

    @Test
    public void testParseDefaultValueOverriddenByProvidedValue() {
        addTool("defaultValueTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueTool",
                            "arguments": {
                              "planet": "Mars"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        assertEquals("args", Map.of("planet", "Mars", "year", 2025), toolCallRequest.args());
    }

    @Tool
    public String primitiveAndOptionalTypeTool(@ToolArg(name = "year", required = false) int year,
                                               @ToolArg(name = "name") Optional<String> name) {
        return name.orElse("none") + year;
    }

    @Test
    public void testParsePrimitiveAndOptionalTypeOmitted() {
        addTool("primitiveAndOptionalTypeTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "primitiveAndOptionalTypeTool",
                            "arguments": {}
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        // The primitive gets its Java default and the Optional gets Optional.empty()
        assertEquals("args", Map.of("year", 0, "name", Optional.empty()), toolCallRequest.args());
    }

    @Tool
    public String intTool(@ToolArg(name = "value") int value) {
        return "OK";
    }

    @Test
    public void testParseNonNumericIntArgument() {
        addTool("intTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "intTool",
                            "arguments": {
                              "value": "abc"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The value argument cannot be converted to the int type."));
    }

    @Tool
    public String genericArgsTool(@ToolArg(name = "list") List<String> list,
                                  @ToolArg(name = "map") Map<String, Integer> map) {
        return "OK";
    }

    @Test
    public void testParseGenericArguments() {
        addTool("genericArgsTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "genericArgsTool",
                            "arguments": {
                              "list": ["a", "b"],
                              "map": {
                                "x": 1,
                                "y": 2
                              }
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, null);
        var expectedArgs = Map.of("list", List.of("a", "b"),
                                  "map", Map.of("x", 1, "y", 2));
        assertEquals("args", expectedArgs, toolCallRequest.args());
    }

    @Test
    public void testParseGenericArgumentWithWrongElementType() {
        addTool("genericArgsTool");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "genericArgsTool",
                            "arguments": {
                              "list": ["a"],
                              "map": {
                                "x": "notAnInt"
                              }
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(ToolCallException.class)
                                .messageIncludes("The map argument cannot be converted to the java.util.Map<java.lang.String, java.lang.Integer> type."));
    }

    @Test
    public void testParseMissingName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS))));
    }

    @Test
    public void testParseNonStringName() {
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": 4,
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS))));
    }

    @Test
    public void testParseNonObjectMeta() {
        addTool("echo");
        var opRequest = parseRequest("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            },
                            "_meta": "notAnObject"
                          }
                        }
                        """);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, null),
                     exception().ofType(JSONRPCException.class)
                                .and(hasProperty("errorCode", equalTo(INVALID_PARAMS)))
                                .and(hasProperty("data", contains(startsWith("Unable to deserialize property '_meta'")))));
    }

    // ---------------
    // Authentication tests
    // ---------------

    @Tool
    @RolesAllowed("adminRole")
    public String restrictedTool() {
        return "OK";
    }

    @Test
    public void testParseRestrictedToolAuthorized() {
        var restrictedTool = addTool("restrictedTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "restrictedTool"
                          }
                        }
                        """);
        req.setUser("admin", "adminRole", "userRole");
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);

        assertEquals("toolName", "restrictedTool", partialToolCallRequest.toolName());
        assertEquals("tool", restrictedTool, partialToolCallRequest.tool());
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, transport);
        assertEquals("tool", restrictedTool, toolCallRequest.tool());
        assertEquals("args", Map.of(), toolCallRequest.args());
        assertEquals("_meta", Map.of(), toolCallRequest.meta());
    }

    @Test
    public void testParseRestrictedToolUnauthorized() {
        var restrictedTool = addTool("restrictedTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "restrictedTool"
                          }
                        }
                        """);
        req.setUser("user", "userRole");
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        assertEquals("toolName", "restrictedTool", partialToolCallRequest.toolName());
        assertEquals("tool", restrictedTool, partialToolCallRequest.tool());
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, transport),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_FORBIDDEN))));
    }

    @Test
    public void testParseRestrictedToolUnauthenticated() {
        addTool("restrictedTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "restrictedTool"
                          }
                        }
                        """);
        // No user set, so we expect 401 rather than 403
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, transport),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_UNAUTHORIZED))));
    }

    @Tool
    @DenyAll
    public String denyAllTool() {
        return "OK";
    }

    @Test
    public void testParseDenyAllToolAuthenticated() {
        addTool("denyAllTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "denyAllTool"
                          }
                        }
                        """);
        req.setUser("admin", "adminRole", "userRole");
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, transport),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_FORBIDDEN))));
    }

    @Test
    public void testParseDenyAllToolUnauthenticated() {
        addTool("denyAllTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "denyAllTool"
                          }
                        }
                        """);
        // @DenyAll is always 403, even when there is no user
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        assertThrows(() -> toolCallParser.parse(opRequest, partialToolCallRequest, transport),
                     exception().ofType(HttpResponseException.class)
                                .and(hasProperty("statusCode", equalTo(SC_FORBIDDEN))));
    }

    @Tool
    @PermitAll
    public String permitAllTool() {
        return "OK";
    }

    @Test
    public void testParsePermitAllToolUnauthenticated() {
        var permitAllTool = addTool("permitAllTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "permitAllTool"
                          }
                        }
                        """);
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, transport);
        assertEquals("tool", permitAllTool, toolCallRequest.tool());
    }

    @Tool
    public String unannotatedTool() {
        return "OK";
    }

    @Test
    public void testParseUnannotatedToolUnauthenticated() {
        var unannotatedTool = addTool("unannotatedTool");
        var req = mockRequestFor("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "unannotatedTool"
                          }
                        }
                        """);
        var opRequest = parseRequest(req);
        var partialToolCallRequest = toolCallParser.preParse(opRequest);
        var transport = newTransport(req);
        var toolCallRequest = toolCallParser.parse(opRequest, partialToolCallRequest, transport);
        assertEquals("tool", unannotatedTool, toolCallRequest.tool());
    }

}
