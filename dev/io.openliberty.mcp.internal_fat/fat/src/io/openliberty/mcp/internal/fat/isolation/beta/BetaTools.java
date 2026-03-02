/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.isolation.beta;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.annotations.ToolArg;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools for Beta App used to tests multi-app isolation
 */
@ApplicationScoped
public class BetaTools {

    @Tool(name = "betaOnlyTool", title = "Beta Only Tool",
          description = "Tool that exists only in the Beta app")
    public String betaOnlyTool(@ToolArg(name = "input", description = "input to echo") String input) {
        return "beta-response: " + input;
    }

    @Tool(name = "sharedToolName", title = "Shared tool name in Beta",
          description = "Tool with the same name in both apps, Aplha and Beta")
    public String sharedToolName() {
        return "from-beta";
    }
}
