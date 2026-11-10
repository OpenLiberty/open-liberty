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

        public static EnableEndpoint fromString(String value) {
            if (value == null) {
                return AUTO;
            }
            return switch (value.trim().toLowerCase()) {
                case "true" -> TRUE;
                case "false" -> FALSE;
                default -> AUTO;
            };
        }
    }
}
