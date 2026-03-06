/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.nonRequiredArgsApp;

import java.util.Optional;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NonRequiredArgsTools {

    @Tool
    public String toolArgRequiredFalse(@ToolArg(required = false, name = "input") String input) {
        if (input != null) {
            return "Hello from: " + input;
        }
        return "Hello toolArgRequiredFalse";
    }

    @Tool
    public String toolArgDefaultValueSetString(@ToolArg(name = "input", defaultValue = "The Default Value") String input) {
        return "Hello from: " + input;
    }

    @Tool
    public String toolArgWithOptionalValue(@ToolArg(name = "input") Optional<String> input) {
        if (input.isEmpty()) {
            return "Hello World";
        }
        return "Hello from: " + input.get();
    }

}
