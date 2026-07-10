/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.spi;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.mcpjava.server.MetaCarrier;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Base class for builders for objects which implement {@link MetaCarrier}
 *
 * @param <THIS> the actual builder class
 */
public class MetaCarrierBuilderImpl<THIS extends MetaCarrierBuilderImpl<THIS>> {

    private static final TraceComponent tc = Tr.register(MetaCarrierBuilderImpl.class);

    /**
     * A label in a prefix: starts with a letter, ends with a letter or digit,
     * interior characters may be letters, digits, or hyphens.
     * Single-character labels (letter only) are also valid.
     */
    private static final Pattern LABEL_PATTERN = Pattern.compile("[a-zA-Z]([a-zA-Z0-9-]*[a-zA-Z0-9])?");

    /**
     * The name segment: must begin and end with an alphanumeric character;
     * interior characters may be alphanumerics, hyphens, underscores, or dots.
     * A single alphanumeric character is also valid.
     */
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9]([a-zA-Z0-9_.\\-]*[a-zA-Z0-9])?");

    public Map<String, Object> metadata = new HashMap<>();

    @SuppressWarnings("unchecked")
    public THIS putMetadata(String key, Object value) {
        validateMetaKey(key);
        metadata.put(key, value);
        return (THIS) this;
    }

    @SuppressWarnings("unchecked")
    public THIS setMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            this.metadata = new HashMap<>();
        } else {
            for (String key : metadata.keySet()) {
                validateMetaKey(key);
            }
            this.metadata = new HashMap<>(metadata);
        }
        return (THIS) this;
    }

    /**
     * Validates a {@code _meta} key against the MCP specification rules.
     *
     * A key has the form {@code [prefix/]name} where:
     *
     * - prefix (optional): one or more dot-separated labels followed by a slash ({@code /}).
     * Each label must start with a letter and end with a letter or digit; interior
     * characters may be letters, digits, or hyphens. A prefix whose second label is
     * {@code modelcontextprotocol} or {@code mcp} is reserved for MCP use and must
     * not be used by implementations.
     *
     * - name: unless empty, must begin and end with an alphanumeric character; interior
     * characters may be alphanumerics, hyphens, underscores, or dots.
     *
     * @param key the metadata key to validate
     * @throws IllegalArgumentException if the key violates any MCP spec rule
     */
    public static void validateMetaKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException(
                                               Tr.formatMessage(tc, "CWMCM0036E.null.meta.key"));
        }

        int slashIndex = key.indexOf('/');

        if (slashIndex == -1) {
            // No prefix — validate the name segment only (may be empty per spec)
            String name = key;
            if (!name.isEmpty() && !NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException(
                                                   Tr.formatMessage(tc, "CWMCM0037E.invalid.meta.key.name", name));
            }
        } else {
            // Has a prefix — validate prefix labels, then the name segment
            String prefix = key.substring(0, slashIndex);
            String name = key.substring(slashIndex + 1);

            if (prefix.isEmpty()) {
                throw new IllegalArgumentException(
                                                   Tr.formatMessage(tc, "CWMCM0038E.empty.meta.key.prefix", key));
            }

            String[] labels = prefix.split("\\.", -1);
            for (String label : labels) {
                if (!LABEL_PATTERN.matcher(label).matches()) {
                    throw new IllegalArgumentException(
                                                       Tr.formatMessage(tc, "CWMCM0039E.invalid.meta.key.label", key, label));
                }
            }

            // Check for reserved second label
            if (labels.length >= 2) {
                String secondLabel = labels[1].toLowerCase();
                if ("modelcontextprotocol".equals(secondLabel) || "mcp".equals(secondLabel)) {
                    throw new IllegalArgumentException(
                                                       Tr.formatMessage(tc, "CWMCM0040E.reserved.meta.key", key));
                }
            }

            if (!name.isEmpty() && !NAME_PATTERN.matcher(name).matches()) {
                throw new IllegalArgumentException(
                                                   Tr.formatMessage(tc, "CWMCM0037E.invalid.meta.key.name", key));
            }
        }
    }
}