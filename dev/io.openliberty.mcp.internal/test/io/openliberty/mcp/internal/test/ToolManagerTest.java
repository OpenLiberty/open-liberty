/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.test;

import static io.openliberty.mcp.internal.security.SecurityRequirement.SecurityAnnotation.NONE;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.assertThrows;
import static io.openliberty.mcp.internal.test.exception.ExceptionAssertions.exception;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import io.openliberty.mcp.internal.ToolMetadata;
import io.openliberty.mcp.internal.ToolRegistry;
import io.openliberty.mcp.internal.schemas.SchemaRegistry;
import io.openliberty.mcp.internal.security.SecurityRequirement;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import io.openliberty.mcp.tools.ToolManager.ToolArguments;
import io.openliberty.mcp.tools.ToolManager.ToolDefinition;
import io.openliberty.mcp.tools.ToolManager.ToolInfo;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

/**
 *
 */
public class ToolManagerTest {

    private ToolManager toolManager;
    private SchemaRegistry schemaRegistry;
    private Jsonb jsonb;

    @Before
    public void setup() {
        schemaRegistry = new SchemaRegistry();
        jsonb = JsonbBuilder.create();
        toolManager = new ToolRegistry(schemaRegistry, jsonb);
    }

    @Test
    public void testToolAddition() throws Exception {
        Function<ToolArguments, ToolResponse> handler = args -> {
            String input = (String) args.args().get("input");
            boolean isFancy = (boolean) args.args().get("isFancy");
            if (isFancy) {
                return ToolResponse.success("Good morning, I am delighted to make your aquaintance " + input);
            } else {
                return ToolResponse.success("Hi " + input);
            }
        };

        Instant before = Instant.now();
        ToolInfo newTool = toolManager.newTool("test_tool")
                                      .setTitle("Test Tool")
                                      .setDescription("A tool to test tool addition")
                                      .addArgument("input", null, true, String.class)
                                      .addArgument("isFancy", "whether to use fancy language", false, Boolean.class, "false")
                                      .setHandler(handler)
                                      .register();
        Instant after = Instant.now();

        assertEquals("name", "test_tool", newTool.name());
        assertEquals("title", "Test Tool", newTool.title());
        assertEquals("description", "A tool to test tool addition", newTool.description());
        assertFalse("isMethod", newTool.isMethod());
        assertEquals("annotations", Optional.empty(), newTool.annotations());
        assertThat("createdAt", newTool.createdAt(), both(greaterThanOrEqualTo(before)).and(lessThanOrEqualTo(after)));

        ToolArgument inputArg = newTool.arguments()
                                       .stream()
                                       .filter(a -> a.name().equals("input"))
                                       .findAny()
                                       .orElseThrow();

        assertNull("input.description", inputArg.description());
        assertTrue("input.required", inputArg.required());
        assertEquals("input.type", String.class, inputArg.type());
        assertNull("input.defaultValue", inputArg.defaultValue());

        ToolArgument isFancyArg = newTool.arguments()
                                         .stream()
                                         .filter(a -> a.name().equals("isFancy"))
                                         .findAny()
                                         .orElseThrow();

        assertEquals("isFancy.description", "whether to use fancy language", isFancyArg.description());
        assertFalse("isFancy.required", isFancyArg.required());
        assertEquals("isFancy.type", Boolean.class, isFancyArg.type());
        assertEquals("isFancy.defaultValue", "false", isFancyArg.defaultValue());

        // Verify internal fields
        ToolMetadata newToolMetadata = (ToolMetadata) newTool;
        assertEquals("handler", handler, newToolMetadata.handler());
        assertNull("asyncHandler", newToolMetadata.asyncHandler());
        assertFalse("returnsCompletionStage", newToolMetadata.returnsCompletionStage());
        assertEquals("methodMetadata", Optional.empty(), newToolMetadata.methodMetadata());
        assertEquals("securityRequirement", new SecurityRequirement(NONE, emptyList()), newToolMetadata.securityRequirement());
        assertNotNull("inputSchema", newToolMetadata.inputSchema());
        assertNull("outputSchema", newToolMetadata.outputSchema());

        var toolList = StreamSupport.stream(toolManager.spliterator(), false)
                                    .toList();
        assertEquals("registry.size", 1, toolList.size());
    }

    @Test
    public void testAsyncHandler() throws Exception {
        Function<ToolArguments, CompletionStage<ToolResponse>> asyncHandler = args -> {
            return CompletableFuture.completedFuture(ToolResponse.success("hello"));
        };

        toolManager.newTool("async-tool")
                   .setAsyncHandler(asyncHandler)
                   .register();

        ToolInfo newTool = toolManager.getTool("async-tool");
        assertEquals("name", "async-tool", newTool.name());

        ToolMetadata newToolMetadata = (ToolMetadata) newTool;
        assertEquals("asyncHandler", asyncHandler, newToolMetadata.asyncHandler());
        assertNull("handler", newToolMetadata.handler());
    }

    @Test
    public void testSchemasFromJsonObject() {
        JsonObject inputSchema = Json.createObjectBuilder()
                                     .add("type", "object")
                                     .add("properties", Json.createObjectBuilder()
                                                            .add("input", Json.createObjectBuilder()
                                                                              .add("type", "string")
                                                                              .build())
                                                            .build())
                                     .add("required", Json.createArrayBuilder()
                                                          .add("input")
                                                          .build())
                                     .build();

        JsonObject outputSchema = Json.createObjectBuilder()
                                      .add("type", "object")
                                      .add("additionalProperties", false)
                                      .build();

        toolManager.newTool("schemas")
                   .setHandler(a -> ToolResponse.success("OK"))
                   .setInputSchema(inputSchema)
                   .setOutputSchema(outputSchema)
                   .register();

        var newTool = toolManager.getTool("schemas");

        ToolMetadata newToolMetadata = (ToolMetadata) newTool;
        String expectedInputSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "input": {"type": "string"}
                            },
                            "required": ["input"]
                        }
                        """;
        assertSchema(expectedInputSchema, newToolMetadata.inputSchema());

        String expectedOutputSchema = """
                        {
                            "type": "object",
                            "additionalProperties": false
                        }
                        """;
        assertSchema(expectedOutputSchema, newToolMetadata.outputSchema());
    }

    @Test
    public void testSchemasFromClass() {
        record OutputObject(String category, List<String> items) {};

        toolManager.newTool("class-schemas")
                   .setHandler(a -> ToolResponse.structuredSuccess(new OutputObject("test", List.of("foo", "bar"))))
                   .generateOutputSchema(OutputObject.class)
                   .register();

        var newTool = toolManager.getTool("class-schemas");
        ToolMetadata newToolMetadata = (ToolMetadata) newTool;

        String expectedOutputSchema = """
                        {
                            "type": "object",
                            "properties": {
                                "category": {
                                    "type": "string"
                                },
                                "items": {
                                    "type": "array",
                                    "items": {
                                        "type": "string"
                                    }
                                }
                            },
                            "required": [
                                "category",
                                "items"
                            ]
                        }
                        """;
        assertSchema(expectedOutputSchema, newToolMetadata.outputSchema());
    }

    private static void assertSchema(String expectedSchema, JsonObject schema) {
        JSONAssert.assertEquals(expectedSchema, schema.toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void testDuplicateToolName() {
        // Test check on newTool
        toolManager.newTool("foo").setHandler(a -> ToolResponse.success("ok")).register();

        assertThrows(() -> toolManager.newTool("foo"),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0026E: An MCP tool with the name foo already exists."));

        // Test check on register (tool is not a duplicate when newTool is called, but is a duplicate when register is called)
        ToolDefinition def1 = toolManager.newTool("bar").setHandler(a -> ToolResponse.success("ok"));
        ToolDefinition def2 = toolManager.newTool("bar").setHandler(a -> ToolResponse.success("ok"));

        def1.register();
        assertThrows(() -> def2.register(),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0026E: An MCP tool with the name bar already exists."));

        // Test calling register twice
        ToolDefinition def3 = toolManager.newTool("baz").setHandler(a -> ToolResponse.success("OK"));
        def3.register();
        assertThrows(() -> def3.register(),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0026E: An MCP tool with the name baz already exists."));
    }

    @Test
    public void testNoHandler() {
        var def = toolManager.newTool("foo");

        assertThrows(() -> def.register(),
                     exception().ofType(IllegalStateException.class)
                                .messageIncludes("CWMCM0027E: Either the setHandler method or the setAsyncHandler method must be called for the foo MCP tool."));
    }

    @Test
    public void testBothHandlers() {
        var def = toolManager.newTool("foo")
                             .setHandler(a -> ToolResponse.success("ok"))
                             .setAsyncHandler(a -> CompletableFuture.completedFuture(ToolResponse.success("ok")));

        assertThrows(() -> def.register(),
                     exception().ofType(IllegalStateException.class)
                                .messageIncludes("CWMCM0027E: Either the setHandler method or the setAsyncHandler method must be called for the foo MCP tool."));
    }

    @Test
    public void testNullName() {
        assertThrows(() -> toolManager.newTool(null),
                     exception().ofType(NullPointerException.class));
    }

    @Test
    public void testBlankName() {
        assertThrows(() -> toolManager.newTool(""),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0029E: The  MCP tool name is not valid. Tool names must be between 1 and 128 characters in length, inclusive."));
    }

    @Test
    public void testNameTooLong() {
        String longName = Stream.generate(() -> "a").limit(129).collect(joining());
        assertThrows(() -> toolManager.newTool(longName),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0029E: The " + longName
                                                 + " MCP tool name is not valid. Tool names must be between 1 and 128 characters in length, inclusive."));
    }

    @Test
    public void testBlankArgName() {
        var def = toolManager.newTool("foo");
        assertThrows(() -> def.addArgument("", null, false, String.class),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0030E: Cannot add an argument with a blank name to the foo MCP tool."));
    }

    @Test
    public void testNoDefaultValueConverter() {
        record MyBean(String value) {};
        var def = toolManager.newTool("foo");

        // Argument with no default value should not throw exception
        def.addArgument("bar", null, false, MyBean.class);

        assertThrows(() -> def.addArgument("baz", null, false, MyBean.class, "defaultValue"),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0031E: The baz argument of the foo MCP tool does not have a converter to change its default value into an object of type "
                                                 + MyBean.class.getName() + "."));
    }

    @Test
    public void testDefaultValueConversionError() {
        var def = toolManager.newTool("foo");

        assertThrows(() -> def.addArgument("bar", null, false, int.class, "abc"),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0020E: The default value of the bar argument of the foo MCP tool cannot be converted to the int type. The value is abc. The error is ")
                                .messageIncludes(NumberFormatException.class.getName())
                                .withCause(exception().ofType(NumberFormatException.class)));
    }

    @Test
    public void testDuplicateArgName() {
        var def = toolManager.newTool("foo")
                             .addArgument("bar", null, false, String.class);

        assertThrows(() -> def.addArgument("bar", null, false, String.class),
                     exception().ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0032E: Cannot add a second argument with the bar name to the foo MCP tool."));
    }
}
