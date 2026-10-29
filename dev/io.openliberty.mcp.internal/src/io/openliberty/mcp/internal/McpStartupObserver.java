/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Retained as an {@code @ApplicationScoped} bean so that
 * {@link McpCdiExtensionMetadata} does not need to be changed.
 * Deferred MCP endpoint registration is now handled entirely by
 * {@link McpDeferredEndpointListener}, which is added as a
 * {@link jakarta.servlet.ServletContextListener} from
 * {@link McpServletInitializer#onStartup} and fires after all user
 * {@code @Startup} observers have completed.
 */
@ApplicationScoped
public class McpStartupObserver {
}
