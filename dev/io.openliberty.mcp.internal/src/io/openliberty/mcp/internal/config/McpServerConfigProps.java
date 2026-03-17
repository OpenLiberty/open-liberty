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

/**
 * Holds configuration data for an application-level MCPserver defined in the server.xml
 *
 * @param stateless The boolean value indicating if the sever is running in stateless mode
 * @param moduleName The name of the module the application is running in
 * @param path The endpoint path for the mcp server
 * @param servicePid The service PID
 */
public record McpServerConfigProps(boolean stateless,
                                   String moduleName,
                                   String path,
                                   String servicePid) implements McpConfig {
    public static final String FALLBACK_PATH = "/mcp";
    public static final McpServerConfigProps DEFAULT_CONFIG = new McpServerConfigProps(false, null, FALLBACK_PATH, null);
}
