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

import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.moduleScope.ModuleScoped;
import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Provider methods to make MCP components available as CDI beans
 */
@ApplicationScoped
public class McpCdiProducers {

    @ModuleScoped
    @Produces
    private ToolManager produceToolManager(McpCdiExtension extension) {
        return extension.getCurrentToolRegistry();
    }

    @ModuleScoped
    @Produces
    private EncoderRegistry produceEncoderRegistry(McpCdiExtension extension) {
        return extension.getCurrentEncoderRegistry();
    }

}
