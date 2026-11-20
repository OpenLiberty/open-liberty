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
import java.util.NoSuchElementException;

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
    McpProtocolVersion minProtocolVersion();

    /**
     * Returns the newest MCP protocol version which requests may use (inclusive)
     *
     * @return the maximum protocol version
     */
    McpProtocolVersion maxProtocolVersion();

    /**
     * Returns whether requests may use the given protocol version. A version is accepted if it is available
     * on this runtime and is between the minimum and maximum protocol versions, inclusive.
     *
     * @param version the protocol version to check
     * @return true if requests may use the version
     */
    default boolean isAccepted(McpProtocolVersion version) {
        return version.isAvailable() && version.isBetween(minProtocolVersion(), maxProtocolVersion());
    }

    /**
     * Returns the protocol versions which requests may use, newest first
     *
     * @return the protocol versions for which {@link #isAccepted(McpProtocolVersion)} is true
     */
    default List<McpProtocolVersion> acceptedProtocolVersions() {
        return Arrays.stream(McpProtocolVersion.values())
                     .filter(this::isAccepted)
                     .toList();
    }

    /**
     * Controls whether the MCP endpoint is registered for this module.
     * <ul>
     *   <li>{@code true} – always register the endpoint. Applications that register tools only dynamically
     *       (via {@code ToolManager}) must set this, because dynamic registration happens too late for the
     *       endpoint to be detected automatically.</li>
     *   <li>{@code auto} – register the endpoint only when {@code @Tool}-annotated methods are discovered
     *       in the module (default).</li>
     *   <li>{@code false} – never register the endpoint.</li>
     * </ul>
     *
     * @return the configured endpoint-enable mode
     */
    EnableEndpoint enableEndpoint();

    /** The three allowed values for the {@code enableEndpoint} configuration attribute. */
    enum EnableEndpoint {
        /** Always register the MCP endpoint. Required for applications that only add tools dynamically via {@code ToolManager}. */
        TRUE,
        /** Register the endpoint only when {@code @Tool}-annotated methods are present (default). */
        AUTO,
        /** Never register the MCP endpoint for this module. */
        FALSE;

        /**
         * Converts a configuration value to an {@code EnableEndpoint}. Surrounding whitespace and case are ignored.
         *
         * @param value the configuration value
         * @return the matching value
         * @throws NoSuchElementException if the value is not {@code true}, {@code auto} or {@code false}
         */
        public static EnableEndpoint fromString(String value) {
            return switch (value.trim().toLowerCase()) {
                case "true" -> TRUE;
                case "auto" -> AUTO;
                case "false" -> FALSE;
                default -> throw new NoSuchElementException(value);
            };
        }
    }
}
