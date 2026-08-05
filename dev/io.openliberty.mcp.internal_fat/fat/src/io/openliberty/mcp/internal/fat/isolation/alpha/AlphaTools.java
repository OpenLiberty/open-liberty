/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.isolation.alpha;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.tools.ToolResponseEncoder;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Tools for Alpha App used to tests multi-app isolation
 */
@ApplicationScoped
public class AlphaTools {

    @Tool(name = "alphaOnlyTool", title = "Alpha Only Tool",
          description = "Tool that exists only in the Aplha app")
    public String alphaOnlyTool(@ToolArg(name = "input", description = "input to echo") String input) {
        return "alpha-response: " + input;
    }

    @Tool(name = "sharedToolName", title = "Shared tool name in Alpha",
          description = "Tool with the same name in both apps, Aplha and Beta")
    public String sharedToolName() {
        return "from-alpha";
    }

    @ApplicationScoped
    public static class AlphaToolResponseEncoder implements ToolResponseEncoder<LocalDate> {

        @Override
        public Class<LocalDate> getType() {
            return LocalDate.class;
        }

        @Override
        public ToolResponse encode(LocalDate value) {
            return ToolResponse.ofText("encoded by AlphaToolResponseEncoder: " + value);
        }
    }

    @Tool(name = "alphaEncodedTool", title = "Alpha Encoded Tool",
          description = "Tool with result encoded by AlphaToolResponseEncoder")
    public LocalDate alphaEncodedTool() {
        return LocalDate.of(2026, 03, 03);
    }

    @Tool(name = "alphaContentEncodedTool", title = "Aplpha Content Encoded Tool",
          description = "Tool result should be encoded by the default encoder because the BetaContentEncoder is not visible in the Alpha app")
    public LocalDateTime alphaContentEncodedTool() {
        return LocalDateTime.of(2026, 03, 03, 03, 03);
    }
}
