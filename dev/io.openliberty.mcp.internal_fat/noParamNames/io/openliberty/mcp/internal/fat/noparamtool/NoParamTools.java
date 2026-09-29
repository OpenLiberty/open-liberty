/*******************************************************************************
 * Copyright (c) 2025, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.noparamtool;

import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoParamTools {

    @Tool(name = "missingToolArgNameTool", title = "missing ToolArgName Tool", description = "ToolArgName is missing so app wont start")
    public String missingToolArgNameTool(@ToolArg(description = "input to echo") String input) {
        return input;
    }

    @Tool
    public String missingToolArgAnnotation(String input) {
        return input;
    }

    @Tool(name = "multipleUnnamedArgs", title = "Multiple Unnamed Args Tool", description = "Multiple args without names - CWMCM0003E should appear exactly once")
    public String multipleUnnamedArgs(@ToolArg(description = "first arg") String first,
                                      @ToolArg(description = "second arg") String second,
                                      @ToolArg(description = "third arg") String third) {
        return first + second + third;
    }
}