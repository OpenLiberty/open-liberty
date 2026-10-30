/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.mixedEarApp.toolModule;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

/**
 * Web module that registers one tool dynamically via {@link ToolManager}.
 * This module <em>should</em> receive an MCP endpoint.
 */
@ApplicationScoped
public class ToolModuleBean {

    @Inject
    ToolManager toolManager;

    void registerTools(@Observes Startup startup) {
        toolManager.newTool("ear-tool")
                   .setHandler(args -> ToolResponse.ofText("from-tool-module"))
                   .register();
    }
}
