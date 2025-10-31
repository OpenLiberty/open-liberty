/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.noparamtool;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NoParamTools {

    @Tool(name = "missingToolArgNameTool", title = "missing ToolArgName Tool", description = "ToolArgName is missing so app wont start")
    public String missingToolArgNameTool(@ToolArg(description = "input to echo") String input) {
        return input;
    }
}