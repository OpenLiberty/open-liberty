/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.dynamicOnlyToolManagerApp;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

/**
 * Registers tools exclusively via ToolManager at startup.
 * <p>
 * Intentionally contains NO {@code @Tool}-annotated methods. This reproduces
 * the bug reported in issue #36103, where the MCP servlet is not initialized
 * when only dynamic ToolManager tools are present at startup.
 */
@ApplicationScoped
public class DynamicOnlyStartupBean {

    @Inject
    ToolManager toolManager;

    @SuppressWarnings("unused")
    private void registerTools(@Observes Startup startup) {
        toolManager.newTool("dynamic-only-tool")
                   .setDescription("A tool registered only via ToolManager, with no @Tool methods in the app")
                   .setHandler(a -> ToolResponse.ofText("dynamic-only-tool response"))
                   .register();
    }

}
