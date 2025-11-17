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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/ToolResponse.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.tools;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.meta.MetaKey;

/**
 * Response to a {@code tools/call} request from the client.
 *
 * @param isError {@code true} if the tool call ended in an error
 * @param content the list of content items (must not be {@code null})
 * @param structuredContent the optional structured result of the tool call
 * @param _meta the optional metadata
 */
public record ToolResponse(boolean isError, List<? extends Content> content, Object structuredContent, Map<MetaKey, Object> _meta) {

    /**
     * @param <C> the content type
     * @param content the content
     * @return a successful response with the specified content items
     */
    @SafeVarargs
    public static <C extends Content> ToolResponse success(C... content) {
        return new ToolResponse(false, Arrays.asList(content));
    }

    /**
     * @param <C> the content type
     * @param content the content
     * @return a successful response with the specified content items
     */
    public static <C extends Content> ToolResponse success(List<C> content) {
        return new ToolResponse(false, content);
    }

    /**
     * @param message the error message
     * @return an unsuccessful response with single text content item
     */
    public static ToolResponse error(String message) {
        return new ToolResponse(true, List.of(new TextContent(message)));
    }

    /**
     * @param message the message to include as text content in the response
     * @return a successful response with single text content item
     */
    public static ToolResponse success(String message) {
        return new ToolResponse(false, List.of(new TextContent(message)));
    }

    /**
     * @param structuredContent the structured content to include in the response
     * @return an unsuccessful response with structured content
     */
    public static ToolResponse structuredError(Object structuredContent) {
        return new ToolResponse(true, null, structuredContent, null);
    }

    /**
     * @param message a message, returned as unstructured text content
     * @param structuredContent the structured content
     * @return a successful response with structured content
     */
    public static ToolResponse structuredSuccess(String message, Object structuredContent) {
        return new ToolResponse(false, List.of(new TextContent(message)), structuredContent, null);
    }

    public ToolResponse(boolean isError, List<? extends Content> content, Map<MetaKey, Object> _meta) {
        this(isError, content, null, _meta);
    }

    public ToolResponse(boolean isError, List<? extends Content> content) {
        this(isError, content, null);
    }

    public ToolResponse {
        if (content == null && structuredContent == null) {
            throw new IllegalArgumentException("content and structuredContent must not both be null");
        }
    }

}
