/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import java.util.Arrays;
import java.util.NoSuchElementException;

import com.ibm.ws.kernel.productinfo.ProductInfo;

import io.openliberty.mcp.internal.McpProtocolVersion.McpProtocolVersionAdapter;
import jakarta.json.bind.adapter.JsonbAdapter;
import jakarta.json.bind.annotation.JsonbTypeAdapter;

/**
 *
 */
@JsonbTypeAdapter(McpProtocolVersionAdapter.class)
public enum McpProtocolVersion {
    V_2026_07_28("2026-07-28", true),
    V_2025_11_25("2025-11-25"),
    V_2025_06_18("2025-06-18"),
    V_2025_03_26("2025-03-26");

    private final String version;
    private final boolean beta;

    private McpProtocolVersion(String version) {
        this(version, false);
    }

    private McpProtocolVersion(String version, boolean beta) {
        this.version = version;
        this.beta = beta;
    }

    public String getVersion() {
        return version;
    }

    /**
     * Returns true if this protocol version can be used on the current runtime.
     * <p>
     * Beta protocol versions are only available when running a beta runtime.
     *
     * @return true if this version is available
     */
    public boolean isAvailable() {
        return !beta || ProductInfo.getBetaEdition();
    }

    /**
     * Returns the protocol versions which are available on the current runtime, in declaration order.
     * <p>
     * Use this in preference to {@link #values()}, which also includes beta versions that may not be available.
     *
     * @return the available protocol versions
     */
    public static McpProtocolVersion[] availableValues() {
        return Arrays.stream(values())
                     .filter(McpProtocolVersion::isAvailable)
                     .toArray(McpProtocolVersion[]::new);
    }

    /**
     * Parses a protocol version string.
     *
     * @param versionString the protocol version string
     * @return the matching protocol version
     * @throws NoSuchElementException if the version is unknown or is not available on the current runtime
     */
    public static McpProtocolVersion parse(String versionString) {
        for (McpProtocolVersion version : availableValues()) {
            if (version.version.equals(versionString)) {
                return version;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Return true if this protocol version supports structured content in tool responses and outputSchema
     * in tool descriptions. Structured content and outputSchema are only supported in protocol version
     * 2025-06-18 and later
     *
     * @return true if structured content and output schema are supported
     */
    public boolean supportsStructuredContent() {
        return this != V_2025_03_26;
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
