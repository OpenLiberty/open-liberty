/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.spi;

import java.util.Collections;
import java.util.List;

import org.mcpjava.server.Icon.Builder;
import org.mcpjava.server.completion.CompletionResult;
import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.AudioContent;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.content.ImageContent;
import org.mcpjava.server.content.ResourceLink;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.prompts.PromptResponse;
import org.mcpjava.server.resources.BlobResourceContents;
import org.mcpjava.server.resources.ResourceResponse;
import org.mcpjava.server.resources.TextResourceContents;
import org.mcpjava.server.spi.McpServerSPI;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.internal.content.AnnotationsImpl;
import io.openliberty.mcp.internal.content.AudioContentImpl;
import io.openliberty.mcp.internal.content.BlobResourceContentsImpl;
import io.openliberty.mcp.internal.content.EmbeddedResourceImpl;
import io.openliberty.mcp.internal.content.ImageContentImpl;
import io.openliberty.mcp.internal.content.ResourceLinkImpl;
import io.openliberty.mcp.internal.content.TextContentImpl;
import io.openliberty.mcp.internal.content.TextResourceContentsImpl;
import io.openliberty.mcp.internal.requests.IconImpl;
import io.openliberty.mcp.internal.tools.ToolResponseImpl;

/**
 *
 */
public class McpSpiImpl implements McpServerSPI {

    @Override
    public CompletionResult.Builder completeResultBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Builder iconBuilder(String uri) {
        return new IconImpl.Builder(uri);
    }

    @Override
    public TextContent.Builder textContentBuilder(String text) {
        return new TextContentImpl.Builder(text);
    }

    @Override
    public AudioContent.Builder audioContentBuilder(byte[] data, String mimeType) {
        return new AudioContentImpl.Builder(data, mimeType);
    }

    @Override
    public ImageContent.Builder imageContentBuilder(byte[] data, String mimeType) {
        return new ImageContentImpl.Builder(data, mimeType);
    }

    @Override
    public EmbeddedResource.Builder textEmbeddedResourceBuilder(String text, String uri) {
        var contentBuilder = new TextResourceContentsImpl.Builder(uri, text);
        return new EmbeddedResourceImpl.TextResourceBuilder(contentBuilder);
    }

    @Override
    public EmbeddedResource.Builder blobEmbeddedResourceBuilder(byte[] data, String uri) {
        var contentBuilder = new BlobResourceContentsImpl.Builder(uri, data);
        return new EmbeddedResourceImpl.BlobResourceBuilder(contentBuilder);
    }

    @Override
    public ResourceLink.Builder resourceLinkBuilder(String name, String uri) {
        return new ResourceLinkImpl.Builder(name, uri);
    }

    @Override
    public Annotations.Builder annotationsBuilder() {
        return new AnnotationsImpl.Builder();
    }

    @Override
    public PromptResponse.Builder promptResponseBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceResponse.Builder resourceResponseBuilder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public TextResourceContents.Builder textResourceContentsBuilder(String uri, String text) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlobResourceContents.Builder blobResourceContentsBuilder(String uri, byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ToolResponse.Builder toolResponseBuilder() {
        return new ToolResponseImpl.Builder();
    }

    @Override
    public ToolResponse newErrorToolResponse(String error) {
        return new ToolResponseImpl(List.of(newTextContent(error)), true, null, Collections.emptyMap());
    }

    @Override
    public ToolResponse newStructuredToolResponse(Object structuredContent) {
        return new ToolResponseImpl(Collections.emptyList(), false, structuredContent, Collections.emptyMap());
    }

    @Override
    public ToolResponse newTextToolResponse(String text) {
        return new ToolResponseImpl(List.of(newTextContent(text)), false, null, Collections.emptyMap());
    }

}
