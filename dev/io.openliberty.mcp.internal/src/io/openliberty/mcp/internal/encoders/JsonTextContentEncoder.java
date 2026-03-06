/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.encoders;

import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
import io.openliberty.mcp.internal.McpCdiExtension;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Encodes a given object to TextContent representation of the JSON value string
 * Lowest priority - fallback
 */
@ApplicationScoped
@Priority(-1000)
public class JsonTextContentEncoder implements ContentEncoder<Object> {

    @Inject
    private McpCdiExtension cdiExtention;

    @Override
    public boolean supports(Class<?> runtimeType) {

        // Skip String - handled by direct conversion
        if (String.class.equals(runtimeType)) {
            return false;
        }

        // Skip Content or ToolResponse - already in the right format
        if (Content.class.isAssignableFrom(runtimeType) ||
            ToolResponse.class.isAssignableFrom(runtimeType)) {
            return false;
        }

        // Skip primitive wrappers - these convert to String natively
        if (Number.class.isAssignableFrom(runtimeType) ||
            Boolean.class.equals(runtimeType) ||
            Character.class.equals(runtimeType)) {
            return false;
        }

        // Support everything else
        return true;
    }

    @Override
    public Content encode(Object value) {
        return new TextContent(cdiExtention.getJsonb().toJson(value));
    }
}
