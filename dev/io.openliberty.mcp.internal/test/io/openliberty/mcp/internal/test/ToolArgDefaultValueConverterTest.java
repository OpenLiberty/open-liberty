/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
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
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.Literals;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.requests.McpRequest;
import io.openliberty.mcp.internal.requests.McpRequestIdDeserializer;
import io.openliberty.mcp.internal.requests.McpRequestIdSerializer;
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolManager.ToolInfo;
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
        ToolRegistry registry = new ToolRegistry(null, jsonb);
        ToolRegistry.set(registry);

        Tool defaultValueIntArgTestTool = Literals.tool("defaultValueInt", "Default Value Int", "ToolArg with a default value of a integer type");
        List<ToolArgument> defaultValIntToolArgs = List.of(new ToolArgument("year", "Integer value", false, Integer.class, "2025"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueIntArgTestTool, defaultValIntToolArgs, Collections.emptyList()));

        Tool defaultValueStringArgTestTool = Literals.tool("defaultValueString", "Default Value String", "ToolArg with a default value of a String type");
        List<ToolArgument> defaultValStringToolArgs = List.of(new ToolArgument("planet", "String value", false, String.class, "Jupiter"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueStringArgTestTool, defaultValStringToolArgs, Collections.emptyList()));

        Tool defaultValueCharArgTestTool = Literals.tool("defaultValueChar", "Default Value Char", "ToolArg with a default value of a Char type");
        List<ToolArgument> defaultValCharToolArgs = List.of(new ToolArgument("initial", "Char value", false, Character.class, "H"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueCharArgTestTool, defaultValCharToolArgs, Collections.emptyList()));

        Tool defaultValueInvalidArgTestTool = Literals.tool("defaultValueInvalidChar", "Default Value Invalid Char", "ToolArg with an invalid default value of a Char type");
        List<ToolArgument> defaultValInvalidToolArgs = List.of(new ToolArgument("initial", "Char value", false, Character.class, "HH"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueInvalidArgTestTool, defaultValInvalidToolArgs, Collections.emptyList()));

        Tool defaultValueInvalidLongArgTestTool = Literals.tool("defaultValueInvalidLong", "Default Value Invalid Long", "ToolArg with an invalid default value of a Long type");
        List<ToolArgument> defaultValInvalidLongToolArgs = List.of(new ToolArgument("count", "Long value", false, Long.class, "notANumber"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueInvalidLongArgTestTool, defaultValInvalidLongToolArgs, Collections.emptyList()));

        Tool defaultValueBoolArgTestTool = Literals.tool("defaultValueBool", "Default Value Bool", "ToolArg with a default value of a Bool type");
        List<ToolArgument> defaultValBoolToolArgs = List.of(new ToolArgument("bool", "Bool value", false, Boolean.class, "true"));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueBoolArgTestTool, defaultValBoolToolArgs, Collections.emptyList()));

        Tool defaultValueObjArgTestTool = Literals.tool("defaultValueObj", "Default Value Obj", "ToolArg with a default value of a Obj type");
        List<ToolArgument> defaultValObjToolArgs = List.of(new ToolArgument("city", "City value", false, City.class, "true"));
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "year");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "planet");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "initial");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "initial");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "count");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "bool");
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
        ToolArgument argMetadata = getArgument(toolCallRequest.getMetadata(), "city");
        assertThrows(() -> McpToolCallParams.convertDefaultValueToArgType(toolCallRequest.getMetadata(), argMetadata),
                     exception()
                                .ofType(IllegalArgumentException.class));
    }

    private static ToolArgument getArgument(ToolInfo toolInfo, String argName) {
        return toolInfo.arguments()
                       .stream()
                       .filter(arg -> arg.name().equals(argName))
                       .findAny()
                       .orElseThrow(() -> new IllegalArgumentException("No argument named " + argName));
    }

}
