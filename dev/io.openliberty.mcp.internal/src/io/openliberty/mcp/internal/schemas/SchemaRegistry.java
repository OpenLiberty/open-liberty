/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.schemas;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;

import io.openliberty.mcp.internal.McpCdiExtension;
import io.openliberty.mcp.internal.ToolMetadata.ToolMethodArgument;
import io.openliberty.mcp.internal.schemas.SchemaGenerator.AnnotatedToolArgument;
import io.openliberty.mcp.tools.ToolManager.ToolArgument;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonObject;

/**
 *
 */
public class SchemaRegistry {

    private HashMap<SchemaKey, JsonObject> schemaCache = new HashMap<>();

    private SchemaCreationBlueprintRegistry blueprintRegistry = new SchemaCreationBlueprintRegistry();

    private static SchemaRegistry staticInstance = null;

    public static SchemaRegistry get() {
        if (staticInstance != null) {
            return staticInstance;
        }
        return CDI.current().select(McpCdiExtension.class).get().getSchemaRegistry();
    }

    // used for testing
    public static void set(SchemaRegistry staticInstance) {
        SchemaRegistry.staticInstance = staticInstance;
    }

    /**
     * Gets the JSON schema for a class
     *
     * @param cls .class to generate schema for
     * @param direction whether to get an input or output schema
     * @return the json schema
     */
    public JsonObject getSchema(Class<?> cls, SchemaDirection direction) {
        ClassKey ck = new ClassKey(cls, direction);
        JsonObject schema;
        if (!schemaCache.containsKey(ck)) {
            schema = SchemaGenerator.generateSchema(cls, direction, blueprintRegistry);
            schemaCache.put(ck, schema);

        } else {
            schema = schemaCache.get(ck);

        }
        return schema;

    }

    /**
     * Gets the input JSON schema for a tool
     *
     * @param arguments the tool method arguments
     * @return the json schema
     */
    public JsonObject getToolInputSchema(List<ToolMethodArgument> arguments) {
        SchemaKey key = new ToolInputKey(arguments);
        return schemaCache.computeIfAbsent(key, k -> {
            var annotatedArguments = arguments.stream()
                                              .map(a -> new AnnotatedToolArgument(a.argument(), a.parameter().getJavaParameter()))
                                              .toList();
            return SchemaGenerator.generateToolInputSchema(annotatedArguments, blueprintRegistry);
        });
    }

    /**
     * Gets the input JSON schema for a programatically registered tool
     *
     * @param arguments the tool arguments
     * @return the json schema
     */
    public JsonObject getProgrammaticToolInputSchema(List<ToolArgument> arguments) {
        SchemaKey key = new ProgrammaticToolInputKey(arguments);
        return schemaCache.computeIfAbsent(key, k -> {
            var annotatedArguments = arguments.stream()
                                              .map(a -> new AnnotatedToolArgument(a))
                                              .toList();
            return SchemaGenerator.generateToolInputSchema(annotatedArguments, blueprintRegistry);
        });
    }

    /**
     * Gets the output JSON schema for a tool
     *
     * @param toolMethod the tool to get the schema for, or {@code null} for a tool without a method
     * @param toolOutputType the unwrapped and resolved return type of the method.
     * @return the json schema
     */
    public JsonObject getToolOutputSchema(AnnotatedMethod<?> toolMethod, Type toolOutputType) {
        SchemaKey key = new ToolOutputKey(toolMethod, toolOutputType);
        return schemaCache.computeIfAbsent(key, k -> SchemaGenerator.generateToolOutputSchema(toolMethod, toolOutputType, blueprintRegistry));
    }

    /**
     * Used to access String result from cache regardless if tool or POJO.
     */
    public interface SchemaKey {}

    public record ClassKey(Class<?> cls, SchemaDirection direction) implements SchemaKey {};

    public record ToolInputKey(List<ToolMethodArgument> arguments) implements SchemaKey {};

    public record ProgrammaticToolInputKey(List<ToolArgument> arguments) implements SchemaKey {};

    public record ToolOutputKey(AnnotatedMethod<?> tool, Type toolOutputType) implements SchemaKey {};

}
