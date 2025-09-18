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
 * Based on https://github.com/quarkiverse/quarkus-mcp-server/blob/main/core/runtime/src/main/java/io/quarkiverse/mcp/server/MetaKey.java
 * Modifications have been made.
 *******************************************************************************/
package io.openliberty.mcp.meta;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A key for additional metadata defined in the {@code _meta} part of the message.
 * <p>
 * {@code _meta} keys have two segments: an optional prefix, and a name.
 */
public record MetaKey(String prefix, String name) {

    /**
     * Create a new key from the specified string value, i.e. from {@code foo.bar/myKey}.
     *
     * @param value
     * @return the key
     */
    public static MetaKey from(String value) {
        if (!value.contains("/")) {
            return new MetaKey(null, value);
        }
        int slashIdx = value.indexOf('/');
        return new MetaKey(value.substring(0, slashIdx + 1), value.substring(slashIdx + 1));
    }

    /**
     * Create a new key with the specified name but without a prefix.
     *
     * @param name
     * @return the key
     */
    public static MetaKey of(String name) {
        return new MetaKey(null, name);
    }

    /**
     * Create a new key with the specified name and the prefix is built from the supplied labels.
     * <p>
     * Note that {@code modelcontextprotocol} and {@code mcp} labels are reserved for MCP spec.
     *
     * @param name
     * @return the key
     */
    public static MetaKey of(String name, String... prefixLabels) {
        return new MetaKey(Arrays.stream(prefixLabels).collect(Collectors.joining(".")) + "/", name);
    }

    public MetaKey {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (!isValidName(name)) {
            throw new IllegalArgumentException("name %s is not valid".formatted(name));
        }
        if (prefix != null && !isValidPrefix(prefix)) {
            throw new IllegalArgumentException("prefix %s is not valid".formatted(prefix));
        }
    }

    @Override
    public String toString() {
        if (prefix == null) {
            return name;
        }
        return prefix + name;
    }

    static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]*[a-zA-Z0-9]");
    static final Pattern PREFIX_PATTERN = Pattern
                                                 .compile("([a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9])+(\\.[a-zA-Z][a-zA-Z0-9-]*[a-zA-Z0-9])*/");

    public static boolean isValidName(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        return NAME_PATTERN.matcher(value).matches();
    }

    public static boolean isValidPrefix(String value) {
        if (value == null
            || value.isBlank()
            || !value.endsWith("/")) {
            return false;
        }
        return PREFIX_PATTERN.matcher(value).matches();
    }
}