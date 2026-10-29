/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.dynamicToolApp;

import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;

@ApplicationScoped
public class ObservesParamStartupBean {

    public void init(@Observes Startup event, ToolManager toolManager) {
        toolManager.newTool("observesParamStartupTool")
                   .setDescription("observesParamStartupTool description")
                   .setHandler(a -> ToolResponse.ofText("observesParamStartupTool response"))
                   .register();
    }
}
