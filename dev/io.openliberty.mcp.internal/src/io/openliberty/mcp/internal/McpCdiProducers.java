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

import io.openliberty.mcp.internal.encoders.EncoderRegistries;
import io.openliberty.mcp.internal.encoders.EncoderRegistry;
import io.openliberty.mcp.internal.moduleScope.ModuleScoped;
import io.openliberty.mcp.internal.sessions.McpSessionStore;
import io.openliberty.mcp.internal.sessions.McpSessionStores;
import io.openliberty.mcp.tools.ToolManager;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Typed;
import jakarta.json.bind.Jsonb;

/**
 * Holds provider methods which produce CDI beans for MCP components.
 *
 * <p>Each {@code @Produces} method in this class adds a CDI bean. When CDI needs to create
 * an instance of one of these beans, it invokes the corresponding method.
 *
 * <p><b>Lifecycle:</b> The scope annotation on the method defines the scope of the created bean.
 * CDI beans are created lazily and CDI will only call the method when it needs to create a bean instance.
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

    @ModuleScoped
    @Produces
    private ConverterRegistry producerConverterRegistry(ConverterRegistries store) {
        return store.getCurrent();
    }

    @ModuleScoped
    @Produces
    private EncoderRegistry produceEncoderRegistry(EncoderRegistries store) {
        return store.getCurrent();
    }

    @ModuleScoped
    @Produces
    private McpRequestTracker produceRequestTracker(McpRequestTrackers store) {
        return store.getCurrent();
    }

    @ModuleScoped
    @Produces
    private McpSessionStore produceSessionStore(McpSessionStores store) {
        return store.getCurrent();
    }

    @ModuleScoped
    @Produces
    @Typed(ToolRegistry.class)
    private ToolRegistry produceToolRegistry(McpCdiExtension extension) {
        return extension.getCurrentToolRegistry();
    }

    @Dependent
    @Produces
    @McpInternal
    private Jsonb produceInternalJsonb(McpCdiExtension extension) {
        return extension.getJsonb();
    }

}
