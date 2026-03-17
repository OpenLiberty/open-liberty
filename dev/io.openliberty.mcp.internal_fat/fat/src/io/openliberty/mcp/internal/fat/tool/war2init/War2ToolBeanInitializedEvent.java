/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.war2init;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.fat.tool.sharedEncoders.SharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class War2ToolBeanInitializedEvent {

    @Inject
    private ToolManager toolManager;

    @Tool
    public String methodTool() {
        return "War2ToolBeanInitializedEvent";
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object initializationObject) {
        toolManager.newTool("apiTool")
                   .setHandler(a -> ToolResponse.success("War2ToolBeanInitializedEvent"))
                   .register();
    }

    /**
     * Tool to test that shared encoders from EAR/lib are accessible to all modules.
     * Uses Person class from SharedEncoders which has a corresponding PersonContentEncoder.
     */
    @Tool(name = "testContentEncoderSharing")
    public Person testContentEncoderSharing() {
        return new Person("Jon", "Doe", 32);
    }
}
