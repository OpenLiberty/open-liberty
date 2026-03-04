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
import io.openliberty.mcp.content.Content;
import io.openliberty.mcp.content.ContentEncoder;
import io.openliberty.mcp.content.TextContent;
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

    public record ToolEncoderResult(boolean success, String message) {}

    public record ContentEncoderResult(String name, int count) {}

    @ApplicationScoped
    public static class BetaContentEncoder implements ContentEncoder<ContentEncoderResult> {

        @Override
        public boolean supports(Class<?> runtimeType) {
            return ContentEncoderResult.class.isAssignableFrom(runtimeType);
        }

        @Override
        public Content encode(ContentEncoderResult value) {
            return new TextContent("encoded by BetaContentEncoder: " + value.name + ", count=" + value.count);
        }
    }

    @Tool(name = "betaEncodedTool", title = "Beta Encoded Tool",
          description = "Tool result should be encoded by the fallback encoder because there is no ToolEncoderResult encoder available in the Beta app")
    public ToolEncoderResult betaEncodedTool() {
        return new ToolEncoderResult(true, "betaEncodedTool");
    }

    @Tool(name = "betaContentEncodedTool", title = "Beta Content Encoded Tool",
          description = "Tool result should be encoded the BetaContentEncoder in the Beta app")
    public ContentEncoderResult betaContentEncodedTool() {
        return new ContentEncoderResult("BetaContentEncodedTool", 10);
    }
}
