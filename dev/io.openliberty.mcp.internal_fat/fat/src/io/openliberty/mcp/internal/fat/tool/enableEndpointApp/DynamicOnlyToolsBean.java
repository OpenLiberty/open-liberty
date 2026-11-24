/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.enableEndpointApp;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

/**
 * Registers a tool programmatically via {@link ToolManager} during startup.
 * This bean intentionally has no {@code @Tool}-annotated methods in order to
 * reproduce the bug where the MCP endpoint is not opened when only dynamic tool
 * registration is used.
 */
@ApplicationScoped
public class DynamicOnlyToolsBean {

    @Inject
    ToolManager toolManager;

    @SuppressWarnings("unused")
    private void registerDynamicTool(@Observes Startup startup) {
        toolManager.newTool("dynamic-only-tool")
                   .setDescription("A tool registered only via ToolManager, no @Tool annotation")
                   .setHandler(args -> ToolResponse.ofText("dynamic-ok"))
                   .register();
    }
}
