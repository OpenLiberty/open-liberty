/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.dynamicOnlyToolApp;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

/**
 * An application bean that registers tools exclusively via the programmatic
 * {@link ToolManager} API during the CDI {@link Startup} event.
 *
 * <p>This bean deliberately has <em>no</em> {@code @Tool}-annotated methods.
 * Its purpose is to verify that the deferred Startup observer in
 * {@code McpCdiExtension} correctly detects the dynamically-registered tools
 * and opens the MCP servlet endpoint even when CDI annotation scanning finds
 * no tools at deploy time.
 */
@ApplicationScoped
public class DynamicOnlyStartupBean {

    @Inject
    ToolManager toolManager;

    void registerTools(@Observes Startup startup) {
        toolManager.newTool("dynamic-echo")
                   .addArgument("input", "text to echo", true, String.class)
                   .setHandler(args -> ToolResponse.ofText((String) args.args().get("input")))
                   .register();
    }
}
