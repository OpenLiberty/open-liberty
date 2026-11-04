/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.config;

import java.time.Duration;
import java.util.NoSuchElementException;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

/**
 * Holds configuration data for an application-level MCPserver defined in the server.xml
 *
 * @param stateless The boolean value indicating if the sever is running in stateless mode
 * @param moduleName The name of the module the application is running in (optional, can be null)
 * @param path The endpoint path for the mcp server
 * @param servicePid The service PID
 * @param sessionTimeout The session timeout duration
 * @param serverInfo The server info configuration
 * @param asyncTimeout The timeout in seconds for asynchronous tool calls
 * @param minimumProtocolVersion The oldest accepted protocol version (optional, can be null)
 * @param maximumProtocolVersion The newest accepted protocol version (optional, can be null)
 */
public record McpServerConfigProps(boolean stateless,
                                   String moduleName,
                                   String path,
                                   String servicePid,
                                   Duration sessionTimeout,
                                   ServerInfo serverInfo,
                                   long asyncTimeoutMs,
                                   McpProtocolVersion minimumProtocolVersion,
                                   McpProtocolVersion maximumProtocolVersion) implements McpConfig {
    /** Config value which selects the latest protocol version available on this runtime */
    public static final String LATEST = "latest";

    public static final String FALLBACK_PATH = "/mcp";
    public static final int DEFAULT_ASYNC_TIMEOUT_MS = 30_000;

    // Default serverInfo values from metatype.xml
    public static final String DEFAULT_SERVER_NAME = "mcp-server";
    public static final String DEFAULT_SERVER_VERSION = "1.0.0";

    // Default ServerInfo with defaults (name and version only, title and description are null)
    public static final ServerInfo DEFAULT_SERVER_INFO = new ServerInfo(DEFAULT_SERVER_NAME, null, DEFAULT_SERVER_VERSION, null);

    public static final McpServerConfigProps DEFAULT_CONFIG = new McpServerConfigProps(false, null, FALLBACK_PATH, null, Duration.ofMinutes(10), null, DEFAULT_ASYNC_TIMEOUT_MS,
                                                                                       null, null);

    /**
     * Compact constructor that validates and applies defaults for required fields.
     * Only name and version are required in ServerInfo (title and description are optional).
     * If serverInfo is null, DEFAULT_SERVER_INFO is used.
     * If serverInfo is provided but name or version are null, defaults are applied.
     * If a protocol version is null, the default for that bound is used.
     */
    public McpServerConfigProps {
        if (minimumProtocolVersion == null) {
            minimumProtocolVersion = defaultMinimum();
        }
        if (maximumProtocolVersion == null) {
            maximumProtocolVersion = defaultMaximum();
        }

        // If serverInfo is null, use DEFAULT_SERVER_INFO
        if (serverInfo == null) {
            serverInfo = DEFAULT_SERVER_INFO;
        } else {
            String name = serverInfo.name();
            String version = serverInfo.version();

            // Apply defaults if required fields are null
            if (name == null || version == null) {
                serverInfo = new ServerInfo(name != null ? name : DEFAULT_SERVER_NAME, serverInfo.title(), version != null ? version : DEFAULT_SERVER_VERSION,
                                            serverInfo.description());
            }
        }
    }

    /**
     * Parses a protocol version from a configuration value. In addition to the version strings
     * understood by {@link McpProtocolVersion#parse(String)}, the special value {@value #LATEST} selects the
     * newest version available on this runtime.
     *
     * @param value the configuration value
     * @return the protocol version
     * @throws NoSuchElementException if the value is not {@value #LATEST} or a supported protocol version
     */
    public static McpProtocolVersion parseProtocolVersion(String value) {
        if (LATEST.equals(value)) {
            return McpProtocolVersion.availableValues()[0];
        }
        return McpProtocolVersion.parse(value);
    }

    /**
     * The minimum protocol version accepted when none is configured, or when the configuration is ignored
     * because the runtime is not beta.
     *
     * @return the default minimum protocol version
     */
    public static McpProtocolVersion defaultMinimum() {
        return McpProtocolVersion.V_2025_03_26;
    }

    /**
     * The maximum protocol version accepted when none is configured, or when the configuration is ignored
     * because the runtime is not beta. This is the newest version available on this runtime, so beta
     * versions are excluded on a non-beta runtime.
     *
     * @return the default maximum protocol version
     */
    public static McpProtocolVersion defaultMaximum() {
        return McpProtocolVersion.availableValues()[0];
    }

    /**
     * Checks whether a protocol version is within the configured range.
     *
     * @param version the version to check
     * @param min the minimum version (inclusive)
     * @param max the maximum version (inclusive)
     * @return true if the version is between min and max, inclusive
     */
    public static boolean isBetween(McpProtocolVersion version, McpProtocolVersion min, McpProtocolVersion max) {
        // Versions are declared newest first, so a newer version has a lower ordinal
        return version.ordinal() <= min.ordinal() && version.ordinal() >= max.ordinal();
    }

    @Override
    public ServerInfo serverInfo() {
        // serverInfo is never null after construction (defaults applied in constructor)
        return serverInfo;
    }
}
