/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.metaKeyApp;

import java.util.HashMap;
import java.util.Map;

import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools used by {@code MetaKeyValidationTest} to exercise _meta key validation
 */
@ApplicationScoped
public class MetaKeyTools {

    /**
     * Calls {@code putMetadata} with a valid key. The key and value are echoed
     * back in the tool response text so the FAT test can assert them.
     */
    @Tool(name = "putValidMetaKey",
          title = "Put valid _meta key",
          description = "Puts a valid _meta key and returns the value from the response metadata")
    public ToolResponse putValidMetaKey(@ToolArg(name = "key", description = "valid _meta key") String key,
                                        @ToolArg(name = "value", description = "value to store") String value) {
        return ToolResponse.builder()
                           .addContent(TextContent.of("OK"))
                           .putMetadata(key, value)
                           .build();
    }

    /**
     * Calls {@code putMetadata} with an invalid key. The expected outcome is an
     * {@link IllegalArgumentException} thrown before the response is built,
     * which causes the tool to return an error response.
     */
    @Tool(name = "putInvalidMetaKey",
          title = "Put invalid _meta key",
          description = "Attempts to put an invalid _meta key; expects IllegalArgumentException")
    public ToolResponse putInvalidMetaKey(@ToolArg(name = "key", description = "invalid _meta key") String key) {
        try {
            ToolResponse.builder()
                        .addContent(TextContent.of("should not reach here"))
                        .putMetadata(key, "value")
                        .build();
            return ToolResponse.builder()
                               .addContent(TextContent.of("ERROR: no exception thrown for key: " + key))
                               .build();
        } catch (IllegalArgumentException e) {
            return ToolResponse.builder()
                               .addContent(TextContent.of("REJECTED: " + e.getMessage()))
                               .build();
        }
    }

    /**
     * Calls {@code setMetadata} with a map containing only valid keys.
     * Returns "OK" on success.
     */
    @Tool(name = "setValidMetadata",
          title = "Set valid _meta map",
          description = "Sets a metadata map with valid keys and returns OK")
    public ToolResponse setValidMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("com.example/key", "value1");
        meta.put("plainName", "value2");
        meta.put("com.example.mcp/notReserved", "value3"); // second label is "example", not reserved

        return ToolResponse.builder()
                           .addContent(TextContent.of("OK"))
                           .setMetadata(meta)
                           .build();
    }

    /**
     * Calls {@code setMetadata} with a map that contains a reserved key.
     * Returns "REJECTED: ..." on success (i.e. exception was thrown).
     */
    @Tool(name = "setReservedMetadata",
          title = "Set reserved _meta map",
          description = "Attempts setMetadata with a reserved key; expects IllegalArgumentException")
    public ToolResponse setReservedMetadata() {
        Map<String, Object> meta = new HashMap<>();
        meta.put("io.mcp/reservedKey", "value");

        try {
            ToolResponse.builder()
                        .addContent(TextContent.of("should not reach here"))
                        .setMetadata(meta)
                        .build();
            return ToolResponse.builder()
                               .addContent(TextContent.of("ERROR: no exception thrown"))
                               .build();
        } catch (IllegalArgumentException e) {
            return ToolResponse.builder()
                               .addContent(TextContent.of("REJECTED: " + e.getMessage()))
                               .build();
        }
    }
}
