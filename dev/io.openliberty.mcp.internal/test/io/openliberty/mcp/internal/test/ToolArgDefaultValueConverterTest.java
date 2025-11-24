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
import io.openliberty.mcp.internal.requests.McpToolCallParams;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

public class ToolArgDefaultValueConverterTest {
    public record City(String name, String country, int population, boolean isCapital) {};

    private static Jsonb jsonb;

    @BeforeClass
    public static void setup() {
        jsonb = JsonbBuilder.create();
        ToolRegistry registry = new ToolRegistry();
        ToolRegistry.set(registry);

        Tool defaultValueIntArgTestTool = Literals.tool("defaultValueInt", "Default Value Int", "ToolArg with a default value of a integer type");
        Map<String, ArgumentMetadata> defaultValIntToolArgs = Map.of("year", new ArgumentMetadata(Integer.class, 0, "Integer value", false, "2025", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueIntArgTestTool, defaultValIntToolArgs, Collections.emptyList()));

        Tool defaultValueStringArgTestTool = Literals.tool("defaultValueString", "Default Value String", "ToolArg with a default value of a String type");
        Map<String, ArgumentMetadata> defaultValStringToolArgs = Map.of("planet", new ArgumentMetadata(String.class, 0, "String value", false, "Jupiter", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueStringArgTestTool, defaultValStringToolArgs, Collections.emptyList()));

        Tool defaultValueBoolArgTestTool = Literals.tool("defaultValueBool", "Default Value Bool", "ToolArg with a default value of a Bool type");
        Map<String, ArgumentMetadata> defaultValBoolToolArgs = Map.of("bool", new ArgumentMetadata(Boolean.class, 0, "Bool value", false, "true", false));
        registry.addTool(ToolMetadataTestUtility.createFrom(defaultValueBoolArgTestTool, defaultValBoolToolArgs, Collections.emptyList()));

        Tool defaultValueObjArgTestTool = Literals.tool("defaultValueObj", "Default Value Obj", "ToolArg with a default value of a Obj type");
        Map<String, ArgumentMetadata> defaultValObjToolArgs = Map.of("city", new ArgumentMetadata(City.class, 0, "City value", false, "true", false));
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
        assertThat(toolCallRequest.convert("year"), equalTo(2025));
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
        assertThat(toolCallRequest.convert("planet"), equalTo("Jupiter"));
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
        assertThat(toolCallRequest.convert("bool"), equalTo(true));
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
        assertThrows(() -> toolCallRequest.convert("city"),
                     exception()
                                .ofType(IllegalArgumentException.class)
                                .messageIncludes("CWMCM0019E")
                                .messageIncludes("defaultValueObj")
                                .messageIncludes("city"));
    }

}
