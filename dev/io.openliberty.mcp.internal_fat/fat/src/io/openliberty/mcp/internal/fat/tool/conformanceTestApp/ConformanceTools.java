/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.fat.tool.conformanceTestApp;

import io.openliberty.mcp.annotations.Tool;
import io.openliberty.mcp.content.AudioContent;
import io.openliberty.mcp.content.ImageContent;
import io.openliberty.mcp.tools.ToolResponse;
import jakarta.enterprise.context.ApplicationScoped;

/**
 *
 */
@ApplicationScoped
public class ConformanceTools {

    // 1. Simple text tool (tools-call-simple-text)
    @Tool(name = "test_simple_text", description = "Returns simple text for testing")
    public String testSimpleText() {
        return "This is a simple text response for testing.";
    }

    // 2. Image content tool (tools-call-image)
    @Tool(name = "test_image_content", description = "Tool returns image content")
    public ImageContent testImageContent() {
        // Create a minimal 1x1 red pixel PNG (base64 encoded)
        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";

        return new ImageContent(base64Image, "image/png");
    }

    // 3. Audio content tool (tools-call-audio)
    @Tool(name = "test_audio_content", description = "Tool returns audio content")
    public AudioContent testAudioContent() {
        // Create a minimal WAV file header (base64 encoded)
        String base64Audio = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQAAAAA=";

        return new AudioContent(base64Audio, "audio/wav");
    }

    // 4. Tool that throws error (tools-call-error)
    @Tool(name = "test_error_handling", description = "Tool returns error correctly")
    public ToolResponse test_error_handling() {
        return ToolResponse.error("This tool intentionally returns an error for testing");
    }
}
