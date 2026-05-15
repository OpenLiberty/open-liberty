/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.content.ContentBlock;
import org.mcpjava.server.content.TextContent;
import org.mcpjava.server.tools.ToolResponse;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of {@link ToolResponse}
 */
@JsonbTypeSerializer(ToolResponseImpl.Serializer.class)
public record ToolResponseImpl(List<ContentBlock> content, boolean isError, Object strucuredContentValue, Map<String, Object> metadata) implements ToolResponse {

    @Override
    public Optional<Object> structuredContent() {
        return Optional.ofNullable(strucuredContentValue);
    }

    /**
     * Builder implementation for {@link ToolResponse}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements ToolResponse.Builder {

        private final List<ContentBlock> content = new ArrayList<>();
        private boolean isError = false;
        private Object structuredContent;

        /** {@inheritDoc} */
        @Override
        public ToolResponse.Builder addContent(ContentBlock content) {
            Objects.requireNonNull(content, "content must not be null");
            this.content.add(content);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ToolResponse.Builder addTextContent(String textContent) {
            Objects.requireNonNull(textContent, "textContent must not be null");
            content.add(TextContent.of(textContent));
            return this;
        }

        @Override
        public ToolResponse.Builder setStructuredContent(Object structuredContent) {
            this.structuredContent = structuredContent;
            return this;
        }

        @Override
        public ToolResponse.Builder setError(boolean isError) {
            this.isError = isError;
            return this;
        }

        @Override
        public ToolResponse build() {
            return new ToolResponseImpl(List.copyOf(content), isError, structuredContent, Map.copyOf(metadata));
        }
    }

    public static class Serializer implements JsonbSerializer<ToolResponseImpl> {

        @Override
        public void serialize(ToolResponseImpl response, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            if (!response.content().isEmpty()) {
                json.writeStartArray("content");
                for (var content : response.content()) {
                    ctx.serialize(content, json);
                }
                json.writeEnd();
            }
            json.write("isError", response.isError());
            if (response.strucuredContentValue() != null) {
                ctx.serialize("structuredContent", response.strucuredContentValue(), json);
            }
            if (!response.metadata().isEmpty()) {
                ctx.serialize("_meta", response.metadata(), json);
            }
            json.writeEnd();
        }
    }

}
