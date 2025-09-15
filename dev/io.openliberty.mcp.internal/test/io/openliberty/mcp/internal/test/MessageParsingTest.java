/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.equalTo;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Capabilities.ClientCapabilities;
import io.openliberty.mcp.internal.Capabilities.Elicitation;
import io.openliberty.mcp.internal.Capabilities.Roots;
import io.openliberty.mcp.internal.Capabilities.Sampling;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.RequestMethod;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.exceptions.jsonrpc.JSONRPCException;
import io.openliberty.mcp.internal.requests.McpInitializeParams;
import io.openliberty.mcp.internal.requests.McpInitializeParams.ClientInfo;
import io.openliberty.mcp.internal.requests.McpNotificationParams;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import jakarta.json.JsonException;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class MessageParsingTest {

    @BeforeClass
    public static void setup() {
        ToolRegistry registry = new ToolRegistry();
        ToolRegistry.set(registry);

        Tool testTool = Literals.tool("echo", "Echo", "Echos the input");
        Map<String, ArgumentMetadata> arguments = Map.of("input", new ArgumentMetadata(String.class, 0, "", true, false));
        registry.addTool(ToolMetadataTestUtility.createFrom(testTool, arguments, Collections.emptyList()));

        Tool addTestTool = Literals.tool("add", "Add", "Addition calculator");
        Map<String, ArgumentMetadata> additionArgs = Map.of("num1", new ArgumentMetadata(Integer.class, 0, "", true, false),
                                                            "num2", new ArgumentMetadata(Integer.class, 1, "", true, false));
        registry.addTool(ToolMetadataTestUtility.createFrom(addTestTool, additionArgs, Collections.emptyList()));

        Tool toogleTestTool = Literals.tool("toggle", "Toggle", "Toggle a boolean");
        Map<String, ArgumentMetadata> booleanArgs = Map.of("input", new ArgumentMetadata(Boolean.class, 0, "boolean value", true, false));
        registry.addTool(ToolMetadataTestUtility.createFrom(toogleTestTool, booleanArgs, Collections.emptyList()));
    }

    @Test
    public void parseToolCallMethod() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        assertThat(((Number) request.id()).intValue(), equalTo(2));
        assertThat(request.getRequestMethod(), equalTo(RequestMethod.TOOLS_CALL));
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        assertThat(toolCallRequest.getArguments(jsonb), arrayContaining("Hello"));
    }

    @Test
    public void parseStringIdType() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        assertThat(request.id(), equalTo("2"));
    }

    @Test(expected = JSONRPCException.class)
    public void validateFalseIdType() throws JsonException, JSONRPCException {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": false,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);

        McpRequest.createValidMCPRequest(reader);
    }

    @Test(expected = JSONRPCException.class)
    public void validateInvalidJSONRPCType() throws JsonException, JSONRPCException {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "1.0",
                          "id": 2,
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);

        McpRequest.createValidMCPRequest(reader);
    }

    @Test(expected = JSONRPCException.class)
    public void validateEmptyId() throws JsonException, JSONRPCException {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "",
                          "method": "tools/call",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);

        McpRequest.createValidMCPRequest(reader);
    }

    @Test(expected = JSONRPCException.class)
    public void validateMissingMethod() throws JsonException, JSONRPCException {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "",
                          "method": "",
                          "params": {
                            "name": "echo",
                            "arguments": {
                              "input": "Hello"
                            }
                          }
                        }
                        """);
        McpRequest.createValidMCPRequest(reader);
    }

    @Test
    public void parseInitilizationMessage() {
        Jsonb jsonb = JsonbBuilder.create();
        var reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "1",
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2024-11-05",
                            "capabilities": {
                              "roots": {
                                "listChanged": true
                              },
                              "sampling": {},
                              "elicitation": {}
                            },
                            "clientInfo": {
                              "name": "ExampleClient",
                              "title": "Example Client Display Name",
                              "version": "1.0.0"
                            }
                          }
                        }
                        """);

        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        assertThat(request.id(), equalTo("1"));
        assertThat(request.getRequestMethod(), equalTo(RequestMethod.INITIALIZE));
        McpInitializeParams params = request.getParams(McpInitializeParams.class, jsonb);
        assertThat(params.getProtocolVersion(), equalTo("2024-11-05"));
        assertThat(params.getCapabilities(),
                   equalTo(ClientCapabilities.of(new Roots(true),
                                                 new Sampling(),
                                                 new Elicitation())));
        assertThat(params.getClientInfo(),
                   equalTo(new ClientInfo("ExampleClient",
                                          "Example Client Display Name",
                                          "1.0.0")));
    }

    @Test
    public void parseInitializedNotification() throws Exception {
        McpRequest request;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            var reader = new StringReader("""
                            {
                              "jsonrpc": "2.0",
                              "method": "notifications/initialized"
                            }
                            """);

            request = jsonb.fromJson(reader, McpRequest.class);
            assertThat(request.getRequestMethod(), equalTo(RequestMethod.INITIALIZED));
        }
    }

    @Test
    public void parseCancelledNotification() throws Exception {
        McpRequest request;
        try (Jsonb jsonb = JsonbBuilder.create()) {
            var reader = new StringReader("""
                            {
                               "jsonrpc": "2.0",
                               "method": "notifications/cancelled",
                               "params": {
                                 "requestId": "123",
                                 "reason": "User requested cancellation"
                               }
                             }
                            """);
            request = jsonb.fromJson(reader, McpRequest.class);
            assertThat(request.getRequestMethod(), equalTo(RequestMethod.CANCELLED));

            McpNotificationParams notificationRequest = request.getParams(McpNotificationParams.class, jsonb);
            assertThat(notificationRequest.getRequestId(), equalTo("123"));
            assertThat(notificationRequest.getReason(), equalTo("User requested cancellation"));
        }
    }

    @Test
    public void parseIntArgumentType() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "add",
                            "arguments": {
                              "num1": 111,
                              "num2": 222
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        assertThat(toolCallRequest.getArguments(jsonb), arrayContaining(111, 222));
    }

    @Test
    public void parseBooleanArgumentType() {
        Jsonb jsonb = JsonbBuilder.create();
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "toggle",
                            "arguments": {
                              "input": true
                            }
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        assertThat(toolCallRequest.getArguments(jsonb), arrayContaining(true));
    }

}
