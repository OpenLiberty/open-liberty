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
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

@ApplicationScoped
public class ProducerInitializedBean {

    @Produces
    @Dependent
    @Named("producerInitializedValue")
    public String produceValue(ToolManager toolManager) {
        toolManager.newTool("producerInitializedTool")
                   .setDescription("producerInitializedTool description")
                   .setHandler(a -> ToolResponse.ofText("producerInitializedTool response"))
                   .register();
        return "produced";
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object event, @Named("producerInitializedValue") String value) {
        // Trigger producer
    }
}
