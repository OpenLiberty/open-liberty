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

    /** Config value which selects the latest supported protocol version */
    public static final String LATEST = "latest";

    /** Oldest version accepted when the runtime is not beta (and the default minimum) */
    public static final McpProtocolVersion GA_MINIMUM = V_2025_03_26;

    /** Newest version accepted when the runtime is not beta */
    public static final McpProtocolVersion GA_MAXIMUM = V_2025_11_25;

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
     * Parses a protocol version from a configuration value. In addition to the version strings
     * understood by {@link #parse(String)}, the special value {@value #LATEST} selects {@link #latest()}.
     *
     * @param value the configuration value
     * @return the protocol version
     * @throws NoSuchElementException if the value is not {@value #LATEST} or a supported protocol version
     */
    public static McpProtocolVersion parseConfigValue(String value) {
        if (LATEST.equals(value)) {
            return latest();
        }
        return parse(value);
    }

    /**
     * @return the newest protocol version available on this runtime
     */
    public static McpProtocolVersion latest() {
        return availableValues()[0];
    }

    /**
     * @return the oldest protocol version available on this runtime
     */
    public static McpProtocolVersion oldest() {
        McpProtocolVersion[] all = availableValues();
        return all[all.length - 1];
    }

    /**
     * The minimum protocol version accepted when none is configured, or when the configuration is ignored
     * because the runtime is not beta.
     *
     * @return the default minimum protocol version
     */
    public static McpProtocolVersion defaultMinimum() {
        return GA_MINIMUM;
    }

    /**
     * The maximum protocol version accepted when none is configured, or when the configuration is ignored
     * because the runtime is not beta.
     * <p>
     * The beta runtime defaults to the latest version. Other runtimes are restricted to {@link #GA_MAXIMUM}.
     *
     * @return the default maximum protocol version
     */
    public static McpProtocolVersion defaultMaximum() {
        return ProductInfo.getBetaEdition() ? latest() : GA_MAXIMUM;
    }

    /**
     * @param min the minimum version (inclusive)
     * @param max the maximum version (inclusive)
     * @return true if this version is between min and max, inclusive
     */
    public boolean isBetween(McpProtocolVersion min, McpProtocolVersion max) {
        // Versions are ISO dates, so string order is chronological order
        return version.compareTo(min.version) >= 0 && version.compareTo(max.version) <= 0;
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
