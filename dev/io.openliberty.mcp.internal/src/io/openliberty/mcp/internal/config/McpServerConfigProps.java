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
            minimumProtocolVersion = McpProtocolVersion.defaultMinimum();
        }
        if (maximumProtocolVersion == null) {
            maximumProtocolVersion = McpProtocolVersion.defaultMaximum();
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

    @Override
    public ServerInfo serverInfo() {
        // serverInfo is never null after construction (defaults applied in constructor)
        return serverInfo;
    }
}
