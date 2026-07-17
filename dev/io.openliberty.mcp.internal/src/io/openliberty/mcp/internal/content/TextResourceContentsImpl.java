/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.content;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.resources.TextResourceContents;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of TextResourceContents
 */
@JsonbTypeSerializer(TextResourceContentsImpl.Serializer.class)
public record TextResourceContentsImpl(String uri,
                                       String text,
                                       String mimeTypeValue,
                                       Map<String, Object> metadata) implements TextResourceContents {

    @Override
    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeTypeValue);
    }

    /**
     * Builder implementation for {@link TextResourceContents}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements TextResourceContents.Builder {

        private final String uri;
        private final String text;
        private String mimeType;

        /**
         * @param uri
         * @param text
         */
        public Builder(String uri, String text) {
            Objects.requireNonNull(uri, "uri must not be null");
            Objects.requireNonNull(text, "text must not be null");
            this.uri = uri;
            this.text = text;
        }

        @Override
        public TextResourceContents.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public TextResourceContents build() {
            return new TextResourceContentsImpl(uri, text, mimeType, Map.copyOf(metadata));
        }
    }

    public static class Serializer implements JsonbSerializer<TextResourceContentsImpl> {

        @Override
        public void serialize(TextResourceContentsImpl obj, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("uri", obj.uri());
            json.write("text", obj.text());
            obj.mimeType().ifPresent(m -> json.write("mimeType", m));
            if (!obj.metadata().isEmpty()) {
                ctx.serialize("_meta", obj.metadata(), json);
            }
            json.writeEnd();
        }
    }
}
