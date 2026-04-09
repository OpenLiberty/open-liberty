/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertool.defaultEncoderTest1;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.fat.encodertools.sharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.inject.Inject;

@ApplicationScoped
public class DefaultEncoderModule {

    @Inject
    private ToolManager toolManager;

    @Tool
    public String methodTool() {
        return "DefaultEncoderModule";
    }

    void startup(@Observes Startup startup) {
        toolManager.newTool("apiTool")
                   .setHandler(a -> ToolResponse.success("DefaultEncoderModule"))
                   .register();
    }

    /**
     * Tool to test that shared encoders from EAR/lib are accessible to all modules.
     * Uses Person class which has a corresponding SharedPersonContentEncoder.
     */
    @Tool(name = "testContentEncoderSharing")
    public Person testContentEncoderSharing() {
        return new Person("Jon", "Doe", 32);
    }

    /**
     * Tool that returns a Company object - should use default JSON encoding because
     * CompanyContentEncoder is only available in DefaultEncoderModule.
     * Uses Company class from the same package.
     */
    @Tool(name = "testWar1SpecificEncoderIsolation", description = "Returns Company with default encoding - no custom encoder available")
    public Company testWar1SpecificEncoderIsolation() {
        return new Company(350000, "Technology", "IBM");
    }
}
