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
 * Produces module-scoped CDI beans for MCP tool management and content encoding.
 *
 * <p>When application code uses {@code @Inject} to request a {@link ToolManager} or
 * {@link EncoderRegistry}, CDI invokes the corresponding producer method in this class.
 * The produced beans are automatically scoped to the requesting module (WAR/EJB JAR),
 * with one instance created per module and cached for reuse.
 *
 * <p><b>Lifecycle:</b> The {@code @Inject} annotation triggers CDI to call the producer method
 * on first use within each module. The {@code @ModuleScoped} annotation ensures the produced
 * instance is cached and reused for all subsequent injections within the same module.
 */
@ApplicationScoped
public class McpCdiProducers {

    /**
     * Produces the module-scoped {@link ToolManager} for programmatic tool registration.
     * Invoked lazily on first injection within each module.
     *
     * @param extension The CDI extension managing tool registries
     * @return The ToolManager instance for the current module
     */
    @ModuleScoped
    @Produces
    private ToolManager produceToolManager(McpCdiExtension extension) {
        return extension.getCurrentToolRegistry();
    }

    /**
     * Produces the module-scoped {@link EncoderRegistry} for custom content encoder registration.
     * Invoked lazily on first injection within each module.
     *
     * @param extension The CDI extension managing encoder registries
     * @return The EncoderRegistry instance for the current module
     */
    @ModuleScoped
    @Produces
    private EncoderRegistry produceEncoderRegistry(McpCdiExtension extension) {
        return extension.getCurrentEncoderRegistry();
    }

}
