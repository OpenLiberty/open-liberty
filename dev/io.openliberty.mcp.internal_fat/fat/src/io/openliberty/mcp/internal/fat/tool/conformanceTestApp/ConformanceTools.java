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

import java.util.Base64;
import java.util.List;

import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.Tool;
import org.mcpjava.server.tools.ToolResponse;

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
        byte[] image = Base64.getDecoder().decode(base64Image);

        return ImageContent.of(image, "image/png");
    }

    // 3. Audio content tool (tools-call-audio)
    @Tool(name = "test_audio_content", description = "Tool returns audio content")
    public AudioContent testAudioContent() {
        // Create a minimal WAV file header (base64 encoded)
        String base64Audio = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAAB9AAACABAAZGF0YQAAAAA=";
        byte[] audio = Base64.getDecoder().decode(base64Audio);

        return AudioContent.of(audio, "audio/wav");
    }

    // 4. Tool that throws error (tools-call-error)
    @Tool(name = "test_error_handling", description = "Tool returns error correctly")
    public ToolResponse test_error_handling() {
        return ToolResponse.ofError("This tool intentionally returns an error for testing");
    }

    // 5. Tool that returns embedded resource
    @Tool(name = "test_embedded_resource", description = "returns an embedded resource")
    public EmbeddedResource test_embedded_resource() {
        return EmbeddedResource.builder("This is an embedded resource content.", "test://embedded-resource")
                               .setMimeType("text/plain")
                               .build();
    }

    // 6. Tool that returns multiple contents
    @Tool(name = "test_multiple_content_types", description = "returns multiple content types")
    public List<ContentBlock> test_multiple_content_types() {
        TextContent textContent = TextContent.of("Multiple content types test:");

        String base64Image = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8DwHwAFBQIAX8jx0gAAAABJRU5ErkJggg==";
        byte[] image = Base64.getDecoder().decode(base64Image);
        ImageContent imageContent = ImageContent.of(image, "image/png");

        String resourceUri = "test://mixed-content-resource";
        String resourceText = "\"{\"test\":\"data\",\"value\":123}\"";
        EmbeddedResource resourceContent = EmbeddedResource.builder(resourceText, resourceUri)
                                                           .setMimeType("application/json")
                                                           .build();

        return List.of(textContent, imageContent, resourceContent);
    }
}
