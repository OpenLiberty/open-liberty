/*******************************************************************************
 * Copyright (c) contributors to https://github.com/quarkiverse/quarkus-mcp-server
 * Copyright (c) 2025 IBM Corporation and others
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
package io.openliberty.mcp.internal.tools;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.features.FeatureManager;
import io.openliberty.mcp.internal.tools.ToolManager.ToolInfo;
import io.openliberty.mcp.tools.ToolResponse;

/**
 * This manager can be used to obtain metadata and register a new tool programmatically.
 */
public interface ToolManager extends FeatureManager<ToolInfo> {

    /**
     *
     * @param name
     * @return the tool with the given name, or {@code null}
     */
    ToolInfo getTool(String name);

    /**
     *
     * @param name The name must be unique
     * @return a new definition builder
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

        String title();

        List<ToolArgument> arguments();

        Optional<ToolAnnotations> annotations();

    }

    /**
     * {@link ToolInfo} definition.
     * <p>
     * This construct is not thread-safe and should not be reused.
     */
    interface ToolDefinition extends FeatureDefinition<ToolInfo, ToolArguments, ToolResponse, ToolDefinition> {

        /**
         *
         * @param name
         * @param description
         * @param required
         * @param type
         * @return self
         */
        default ToolDefinition addArgument(String name, String description, boolean required, java.lang.reflect.Type type) {
            return addArgument(name, description, required, type, null);
        }

        /**
         *
         * @param name
         * @param description
         * @param required
         * @param type
         * @param defaultValue
         * @return self
         */
        ToolDefinition addArgument(String name, String description, boolean required, java.lang.reflect.Type type,
                                   String defaultValue);

        /**
         *
         * @param annotations
         * @return self
         */
        ToolDefinition setAnnotations(ToolAnnotations annotations);

        /**
         *
         * @param title
         * @return self
         */
        ToolDefinition setTitle(String title);

        /**
         * Generate the output schema for structured content from the given class.
         *
         * @param outputSchema
         * @return self
         */
        ToolDefinition generateOutputSchema(Class<?> from);

        /**
         * @param schema
         * @return self
         */
        ToolDefinition setOutputSchema(Object schema);

        /**
         * If not set the input schema is generated automatically.
         *
         * @param schema
         * @return self
         */
        ToolDefinition setInputSchema(Object schema);

        /**
         * @return the tool info
         * @throws IllegalArgumentException if a tool with the given name already exits
         */
        @Override
        ToolInfo register();

    }

    public interface ToolArguments extends RequestFeatureArguments {

        Map<String, Object> args();

    }

    record ToolArgument(String name, String description, boolean required, java.lang.reflect.Type type, String defaultValue) {}

    /**
     * @see Tool#annotations()
     */
    record ToolAnnotations(String title, boolean readOnlyHint, boolean destructiveHint, boolean idempotentHint,
                           boolean openWorldHint) {}
}