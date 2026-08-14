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

import org.mcpjava.server.ContentEncoder;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;

import io.openliberty.mcp.internal.McpCdiExtension;
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
    public Class<Object> getType() {
        return Object.class;
    }

    @Override
    public ContentBlock encode(Object value) {
        return TextContent.of(cdiExtention.getJsonb().toJson(value));
    }
}
