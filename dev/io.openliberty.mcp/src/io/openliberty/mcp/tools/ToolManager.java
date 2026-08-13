/*******************************************************************************
 * Copyright (c) contributors to https://github.com/quarkiverse/quarkus-mcp-server
 * Copyright (c) 2025, 2026 IBM Corporation and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/ToolManager.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.features.FeatureManager;
import io.openliberty.mcp.tools.ToolManager.ToolInfo;
import jakarta.inject.Inject;

/**
 * Allows registering new tools, removing tools and getting information about registered tools.
 * <p>
 * An instance can be obtained by injecting with {@link Inject @Inject}.
 * <p>
 * In an enterprise application, each web module has its own {@code ToolManager}. Calling methods on the {@code ToolManager} will affect the current web module.
 */
public interface ToolManager extends FeatureManager<ToolInfo> {

    /**
     * Gets information about a tool.
     *
     * @param name the name of the tool
     * @return the tool with the given name, or {@code null}
     */
    ToolInfo getTool(String name);

    /**
     * Adds a tool.
     *
     * @param name the name of the tool. Must be unique.
     * @return a new tool definition builder
     * @throws IllegalArgumentException if a tool with the given name already exits
     * @see ToolDefinition#register()
     */
    ToolDefinition newTool(String name);

    /**
     * Removes a tool previously added with {@link #newTool(String)}.
     *
     * @return the removed tool or {@code null} if no such tool existed
     */
    ToolInfo removeTool(String name);

    /**
     * Tool info.
     */
    interface ToolInfo extends FeatureManager.FeatureInfo {

        /**
         * The user-readable title for the tool
         *
         * @return the title
         */
        String title();

        /**
         * Information about the arguments that the tool expects
         *
         * @return the tool arguments
         */
        List<ToolArgument> arguments();

        /**
         * The <a href="https://modelcontextprotocol.org/specification/2025-11-25/schema#toolannotations">MCP annotations</a> of the tool
         *
         * @return the tool annotations
         */
        Optional<ToolAnnotations> annotations();

    }

    /**
     * A builder to define a new tool. Created with {@link ToolManager#newTool(String)}.
     * <p>
     * This construct is not thread-safe and should not be reused.
     */
    interface ToolDefinition extends FeatureDefinition<ToolInfo, ToolArguments, ToolResponse, ToolDefinition> {

        /**
         * Adds an argument to the tool.
         *
         * @param name the argument name
         * @param description the argument description
         * @param required whether the argument is required
         * @param type the argument type
         * @return self this builder
         */
        default ToolDefinition addArgument(String name, String description, boolean required, java.lang.reflect.Type type) {
            return addArgument(name, description, required, type, null);
        }

        /**
         * Adds an argument to the tool.
         *
         * @param name the argument name
         * @param description the argument description
         * @param required whether the argument is required
         * @param type the argument type
         * @param defaultValue the default value for the argument
         * @return self this builder
         */
        ToolDefinition addArgument(String name, String description, boolean required, java.lang.reflect.Type type,
                                   String defaultValue);

        /**
         * Sets the annotations of the tool
         *
         * @param annotations the tool annotations
         * @return self this builder
         */
        ToolDefinition setAnnotations(ToolAnnotations annotations);

        /**
         * Sets the human-readable title of the tool
         *
         * @param title the title
         * @return self this builder
         */
        ToolDefinition setTitle(String title);

        /**
         * Sets the class to use to generate the output schema for structured content.
         * <p>
         * The class will be inspected and a schema generated which will match the JSON which JSON-B would generate for a class of this type.
         * <p>
         * Ignored if {@link #setOutputSchema(Object)} is called.
         *
         * @param from the class to use to generate the schema
         * @return self this builder
         */
        ToolDefinition generateOutputSchema(Class<?> from);

        /**
         * Sets the output schema for structured content.
         * <p>
         * JSON-B will be used to convert {@code schema} to JSON.
         *
         * @param schema the output schema
         * @return self this builder
         */
        ToolDefinition setOutputSchema(Object schema);

        /**
         * Sets the input schema for this tool.
         * <p>
         * If not set, the input schema is generated automatically from the tool argument types.
         * <p>
         * JSON-B will be used to convert {@code schema} to JSON.
         *
         * @param schema the input schema
         * @return self this builder
         */
        ToolDefinition setInputSchema(Object schema);

        /**
         * Add the new tool to the tool manager.
         *
         * @return the tool info for the newly created tool
         * @throws IllegalArgumentException if a tool with the given name already exits
         */
        @Override
        ToolInfo register();

    }

    /**
     * The input to a tool handler, providing access to the arguments.
     */
    public interface ToolArguments extends RequestFeatureArguments {

        /**
         * The tool arguments.
         * <p>
         * Every required argument will be present in the map and each argument can be safely cast to {@link ToolArgument#type()}.
         *
         * @return a map from the argument names to the argument values
         */
        Map<String, Object> args();

    }

    /**
     * Information about a tool argument
     *
     * @param name the tool name
     * @param description the tool description
     * @param required whether the tool argument is required
     * @param type the argument type
     * @param defaultValue the default value
     */
    record ToolArgument(String name, String description, boolean required, java.lang.reflect.Type type, String defaultValue) {}

    /**
     * The <a href="https://modelcontextprotocol.org/specification/2025-11-25/schema#toolannotations">MCP annotations</a> of a tool.
     */
    record ToolAnnotations(String title, boolean readOnlyHint, boolean destructiveHint, boolean idempotentHint,
                           boolean openWorldHint) {}
}