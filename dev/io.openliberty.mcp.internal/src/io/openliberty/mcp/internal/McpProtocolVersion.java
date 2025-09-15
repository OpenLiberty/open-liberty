/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.NoSuchElementException;

import io.openliberty.mcp.internal.McpProtocolVersion.McpProtocolVersionAdapter;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 *
 */
@JsonbTypeAdapter(McpProtocolVersionAdapter.class)
public enum McpProtocolVersion {
    V_2025_06_18("2025-06-18"),
    V_2025_03_26("2025-03-26");

    private final String version;

    private McpProtocolVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static McpProtocolVersion parse(String versionString) {
        for (McpProtocolVersion version : values()) {
            if (version.version.equals(versionString)) {
                return version;
            }
        }
        throw new NoSuchElementException();
    }

    public static class McpProtocolVersionAdapter implements JsonbAdapter<McpProtocolVersion, String> {

        @Override
        public McpProtocolVersion adaptFromJson(String string) throws Exception {
            return McpProtocolVersion.parse(string);
        }

        @Override
        public String adaptToJson(McpProtocolVersion version) throws Exception {
            return version.getVersion();
        }

    }
}
