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

import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.TextContent;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of {@link TextContent}
 */
@JsonbTypeSerializer(TextContentImpl.Serializer.class)
public record TextContentImpl(Annotations annotationsValue, String text, Map<String, Object> metadata) implements TextContent {

    @Override
    public Optional<Annotations> annotations() {
        return Optional.ofNullable(annotationsValue);
    }

    @Override
    public String text() {
        return text;
    }

    /**
     * Builder implementation for {@link TextContent}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements TextContent.Builder {

        private Annotations annotations;
        private final String text;

        public Builder(String text) {
            super();
            Objects.requireNonNull(text, "text must not be null");
            this.text = text;
        }

        @Override
        public TextContent.Builder setAnnotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        @Override
        public TextContent build() {
            return new TextContentImpl(annotations, text, metadata);
        }

    }

    public static class Serializer implements JsonbSerializer<TextContentImpl> {

        @Override
        public void serialize(TextContentImpl object, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("type", "text");
            json.write("text", object.text());
            object.annotations().ifPresent(a -> ctx.serialize("annotations", a, json));
            if (!object.metadata.isEmpty()) {
                ctx.serialize("_meta", object.metadata(), json);
            }
            json.writeEnd();
        }

    }
}
