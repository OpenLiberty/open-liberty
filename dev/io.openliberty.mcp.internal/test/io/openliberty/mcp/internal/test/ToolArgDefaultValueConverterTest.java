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

import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.ToolMetadata.ArgumentMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpRequestIdDeserializer;
import io.openliberty.mcp.internal.requests.McpRequestIdSerializer;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;

public class ToolArgDefaultValueConverterTest {
    public record City(String name, String country, int population, boolean isCapital) {};

    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        JsonbConfig jsonbConfig = new JsonbConfig().withSerializers(new McpRequestIdSerializer())
                                                   .withDeserializers(new McpRequestIdDeserializer());
        jsonb = JsonbBuilder.create(jsonbConfig);
        ToolRegistry registry = new ToolRegistry();
        ToolRegistry.set(registry);

        Tool defaultValueIntArgTestTool = Literals.tool("defaultValueInt", "Default Value Int", "ToolArg with a default value of a integer type");
        Map<String, ArgumentMetadata> defaultValIntToolArgs = Map.of("year", new ArgumentMetadata("year", Integer.class, 0, "Integer value", false, "2025", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueIntArgTestTool, defaultValIntToolArgs, Collections.emptyList()));

        Tool defaultValueStringArgTestTool = Literals.tool("defaultValueString", "Default Value String", "ToolArg with a default value of a String type");
        Map<String, ArgumentMetadata> defaultValStringToolArgs = Map.of("planet", new ArgumentMetadata("planet", String.class, 0, "String value", false, "Jupiter", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueStringArgTestTool, defaultValStringToolArgs, Collections.emptyList()));

        Tool defaultValueCharArgTestTool = Literals.tool("defaultValueChar", "Default Value Char", "ToolArg with a default value of a Char type");
        Map<String, ArgumentMetadata> defaultValCharToolArgs = Map.of("initial", new ArgumentMetadata("initial", Character.class, 0, "Char value", false, "H", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueCharArgTestTool, defaultValCharToolArgs, Collections.emptyList()));

        Tool defaultValueInvalidArgTestTool = Literals.tool("defaultValueInvalidChar", "Default Value Invalid Char", "ToolArg with an invalid default value of a Char type");
        Map<String, ArgumentMetadata> defaultValInvalidToolArgs = Map.of("initial", new ArgumentMetadata("initial", Character.class, 0, "Char value", false, "HH", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueInvalidArgTestTool, defaultValInvalidToolArgs, Collections.emptyList()));

        Tool defaultValueInvalidLongArgTestTool = Literals.tool("defaultValueInvalidLong", "Default Value Invalid Long", "ToolArg with an invalid default value of a Long type");
        Map<String, ArgumentMetadata> defaultValInvalidLongToolArgs = Map.of("count", new ArgumentMetadata("count", Long.class, 0, "Long value", false, "notANumber", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueInvalidLongArgTestTool, defaultValInvalidLongToolArgs, Collections.emptyList()));

        Tool defaultValueBoolArgTestTool = Literals.tool("defaultValueBool", "Default Value Bool", "ToolArg with a default value of a Bool type");
        Map<String, ArgumentMetadata> defaultValBoolToolArgs = Map.of("bool", new ArgumentMetadata("bool", Boolean.class, 0, "Bool value", false, "true", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueBoolArgTestTool, defaultValBoolToolArgs, Collections.emptyList()));

        Tool defaultValueObjArgTestTool = Literals.tool("defaultValueObj", "Default Value Obj", "ToolArg with a default value of a Obj type");
        Map<String, ArgumentMetadata> defaultValObjToolArgs = Map.of("city", new ArgumentMetadata("city", City.class, 0, "City value", false, "true", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueObjArgTestTool, defaultValObjToolArgs, Collections.emptyList()));
    }

    @Test
    public void testArgumentDefaultValueIntTypeConversion() {

        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueInt",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("year");
        assertThat(McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata), equalTo(2025));
    }

    @Test
    public void testArgumentDefaultValueStringTypeConversion() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueString",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("planet");
        assertThat(McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata), equalTo("Jupiter"));
    }

    @Test
    public void testArgumentDefaultValueCharTypeConversion() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueChar",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("initial");
        assertThat(McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata), equalTo('H'));
    }

    @Test
    public void testArgumentDefaultValueInvalidCharTypeConversion() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueInvalidChar",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("initial");
        assertThrows(() -> McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata),
                     exception()
                                .ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0020E: The default value of the initial argument of the defaultValueInvalidChar MCP tool cannot be converted to the class java.lang.Character type. The value is HH. The error is java.lang.IllegalArgumentException: CWMCM0021E: A character default value must be exactly one character, but was HH."));
    }

    @Test
    public void testArgumentDefaultValueInvalidLongTypeConversion() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueInvalidLong",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("count");
        assertThrows(() -> McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata),
                     exception()
                                .ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0020E: The default value of the count argument of the defaultValueInvalidLong MCP tool cannot be converted to the class java.lang.Long type. The value is notANumber. The error is java.lang.NumberFormatException: For input string: \"notANumber\""));
    }

    @Test
    public void testArgumentDefaultValueBooleanTypeConversion() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueBool",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("bool");
        assertThat(McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata), equalTo(true));
    }

    @Test
    public void testArgumentDefaultValueWithoutConverter() {
        StringReader reader = new StringReader("""
                        {
                          "jsonrpc": "2.0",
                          "id": "2",
                          "method": "tools/call",
                          "params": {
                            "name": "defaultValueObj",
                            "arguments": {}
                          }
                        }
                        """);
        McpRequest request = jsonb.fromJson(reader, McpRequest.class);
        McpToolCallParams toolCallRequest = request.getParams(McpToolCallParams.class, jsonb);
        ArgumentMetadata argMetadata = toolCallRequest.getMetadata().arguments().get("city");
        assertThrows(() -> McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata),
                     exception()
                                .ofType(IllegalArgumentException.class));
    }

}
