/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.deploymentErrorApps;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class DuplicateToolErrorTest2 {

    @Tool(name = "echo", title = "Echoes the input", description = "Returns the input unchanged")
    public String echo(@ToolArg(name = "input", description = "input to echo") String input) {
        if (input.equals("throw error")) {
            throw new RuntimeException("Method call caused runtime exception");
        }
        return input;
    }

    ///////////////////
    //// Duplicate echo
    @Tool(name = "echo")
    public String duplicateEcho(@ToolArg(name = "input") String input) {
        return input;
    }

    @Tool(name = "bob2", title = "Echoes the input", description = "Returns the input unchanged")
    public String bob(@ToolArg(name = "input", description = "input to echo") String input) {
        if (input.equals("throw error")) {
            throw new RuntimeException("Method call caused runtime exception");
        }
        return input;
    }

    ///////////////////
    //// Duplicate bob
    @Tool(name = "bob")
    public String duplicateBob(@ToolArg(name = "input") String input) {
        return input;
    }

}
