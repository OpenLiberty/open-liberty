/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.war1startup;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.fat.tool.sharedEncoders.SharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@ApplicationScoped
public class War1ToolBeanStartupEvent {

    @Inject
    private ToolManager toolManager;

    @Tool
    public String methodTool() {
        return "War1ToolBeanStartupEvent";
    }

    void startup(@Observes Startup startup) {
        toolManager.newTool("apiTool")
                   .setHandler(a -> ToolResponse.success("War1ToolBeanStartupEvent"))
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

    /**
     * Local Company record - same structure as War1SpecificEncoders.Company but different class
     * This will use default JSON encoding since CompanyContentEncoder is not available in this module
     * Fields are in alphabetical order to match JSON-B serialization
     */
    public record Company(int employees, String industry, String name) {}

    /**
     * Tool that returns a Company object - should use default JSON encoding because
     * CompanyContentEncoder is only available in war1WithInitializedEvent
     */
    @Tool(name = "testWar1SpecificEncoderIsolation", description = "Returns Company with default encoding - no custom encoder available")
    public Company testWar1SpecificEncoderIsolation() {
        return new Company(350000, "Technology", "IBM");
    }
}
