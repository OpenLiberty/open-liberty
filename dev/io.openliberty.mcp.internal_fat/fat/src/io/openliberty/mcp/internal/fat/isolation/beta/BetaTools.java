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

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.mcpjava.server.ContentEncoder;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;

import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolArg;
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

    @ApplicationScoped
    public static class BetaContentEncoder implements ContentEncoder<LocalDateTime> {

        @Override
        public Class<LocalDateTime> getType() {
            return LocalDateTime.class;
        }

        @Override
        public ContentBlock encode(LocalDateTime value) {
            return TextContent.of("encoded by BetaContentEncoder: " + value);
        }
    }

    @Tool(name = "betaEncodedTool", title = "Beta Encoded Tool",
          description = "Tool result should be encoded by the fallback encoder because there is no LocalDate encoder available in the Beta app")
    public LocalDate betaEncodedTool() {
        return LocalDate.of(2026, 03, 03);
    }

    @Tool(name = "betaContentEncodedTool", title = "Beta Content Encoded Tool",
          description = "Tool resulting LocalDateTime should be encoded the BetaContentEncoder in the Beta app")
    public LocalDateTime betaContentEncodedTool() {
        return LocalDateTime.of(2026, 03, 03, 03, 03);
    }
}
