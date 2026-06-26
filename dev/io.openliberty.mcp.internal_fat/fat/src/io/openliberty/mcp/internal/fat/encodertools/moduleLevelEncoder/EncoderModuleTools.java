/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.encodertools.moduleLevelEncoder;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.internal.fat.encodertools.sharedEncoders.Person;
import io.openliberty.mcp.tools.ToolManager;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Module providing tools for testing encoder functionality.
 * This module demonstrates:
 * - Module-specific encoders (PersonContentEncoder, CompanyContentEncoder)
 * - Shared encoders from EAR/lib (SharedPersonContentEncoder)
 * - Encoder isolation between modules
 */
@ApplicationScoped
public class EncoderModuleTools {

    @Inject
    private ToolManager toolManager;

    @Tool
    public String methodTool() {
        return "EncoderModule";
    }

    public void init(@Observes @Initialized(ApplicationScoped.class) Object initializationObject) {
        toolManager.newTool("apiTool")
                   .setHandler(a -> ToolResponse.success("EncoderModule"))
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
     * Tool that returns a Company object - should be encoded by CompanyContentEncoder.
     * This encoder is ONLY available in war1WithInitializedEvent.
     */
    @Tool(name = "testWar1SpecificEncoder", description = "Returns a Company object that can only be encoded in War1")
    public Company testWar1SpecificEncoder() {
        return new Company(350000, "Technology", "IBM");
    }

}
