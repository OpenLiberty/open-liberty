/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.progressApp;

import org.mcpjava.server.progress.Progress;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools used by ProgressTest to verify that {@link Progress} can be declared
 * as a parameter of a {@code @Tool} method.
 */
@ApplicationScoped
public class ProgressTools {

    /**
     * A tool that accepts a {@link Progress} special argument.
     * When no progress token is provided by the client, the
     * no-op {@code ProgressImpl} is injected, so {@code progress.token()} returns
     * an empty {@link java.util.Optional}.
     *
     * @param progress injected automatically by the runtime
     * @param input an ordinary string argument echoed back in the response
     * @return a description of the progress-token state plus the echoed input
     */
    @Tool(name = "progressTool",
          title = "Progress Tool",
          description = "Accepts a Progress parameter and reports whether a progress token is present")
    public String progressTool(Progress progress,
                               @ToolArg(name = "input", description = "value to echo back") String input) {
        boolean hasToken = progress.token().isPresent();
        return "progressTokenPresent=" + hasToken + ",input=" + input;
    }
}
