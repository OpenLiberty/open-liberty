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
import java.util.Arrays;
import java.util.List;

import io.openliberty.mcp.internal.McpProtocolVersion;
import io.openliberty.mcp.internal.responses.McpInitializeResult.ServerInfo;

public interface McpConfig {
    boolean stateless();

    String moduleName();

    String path();

    String servicePid();

    Duration sessionTimeout();

    /**
     * Returns the ServerInfo object with the configured values (or defaults)
     *
     * @return ServerInfo with name, title, version, and description
     */
    ServerInfo serverInfo();

    /**
     * Returns the timeout in milliseconds for asynchronous tool executions
     *
     * @return the async timeout in milliseconds
     */
    long asyncTimeoutMs();

    /**
     * Returns the oldest MCP protocol version which requests may use (inclusive)
     *
     * @return the minimum protocol version
     */
    McpProtocolVersion minimumProtocolVersion();

    /**
     * Returns the newest MCP protocol version which requests may use (inclusive)
     *
     * @return the maximum protocol version
     */
    McpProtocolVersion maximumProtocolVersion();

    /**
     * Returns the protocol versions which requests may use, newest first
     *
     * @return the protocol versions between the minimum and maximum, inclusive
     */
    default List<McpProtocolVersion> acceptedProtocolVersions() {
        return Arrays.stream(McpProtocolVersion.values())
                     .filter(v -> v.isBetween(minimumProtocolVersion(), maximumProtocolVersion()))
                     .toList();
    }
}
