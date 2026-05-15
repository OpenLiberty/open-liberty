/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mcp.internal.requests;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.Icon;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

/**
 * Record implementation of {@link Icon}
 */
@JsonbTypeSerializer(IconImpl.Serializer.class)
@JsonbTypeDeserializer(IconImpl.Deserializer.class)
public record IconImpl(String src, String mimeTypeValue, List<String> sizes, Theme themeValue) implements Icon {

    public IconImpl {
        Objects.requireNonNull(src, "src must not be null");
        Objects.requireNonNull(sizes, "sizes must not be null");
    }

    @Override
    public Optional<String> mimeType() {
        return Optional.of(mimeTypeValue);
    }

    @Override
    public Optional<Theme> theme() {
        return Optional.of(themeValue);
    }

    /**
     * Builder implementation for {@link Icon}
     */
    public static class Builder implements Icon.Builder {

        private boolean anySize;
        private List<String> sizes = null;
        private String src;
        private Theme theme;
        private String mimeType;

        public Builder(String src) {
            Objects.requireNonNull(src, "src must not be null");
            this.src = src;
        }

        @Override
        public Icon.Builder addSize(int width, int height) {
            if (anySize) {
                throw new IllegalStateException("Cannot add sizes after calling setAnySize");
            }
            if (sizes == null) {
                sizes = new ArrayList<>();
            }
            sizes.add(width + "x" + height);
            return this;
        }

        @Override
        public Icon.Builder setAnySize() {
            if (!sizes.isEmpty()) {
                throw new IllegalStateException("Cannot call setAnySize after calling addSize");
            }
            anySize = true;
            return this;
        }

        @Override
        public Icon.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public Icon.Builder setTheme(Theme theme) {
            this.theme = theme;
            return this;
        }

        @Override
        public Icon build() {
            if (anySize) {
                sizes = List.of("any");
            } else if (sizes == null) {
                sizes = List.of();
            } else {
                sizes = List.copyOf(this.sizes);
            }
            return new IconImpl(src, mimeType, sizes, theme);
        }
    }

    public static class Serializer implements JsonbSerializer<IconImpl> {

        @Override
        public void serialize(IconImpl icon, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("src", icon.src());
            icon.mimeType().ifPresent(m -> json.write("mimeType", m));
            if (!icon.sizes().isEmpty()) {
                json.writeStartArray();
                icon.sizes().forEach(size -> json.write(size));
                json.writeEnd();
            }
            icon.theme().map(t -> json.write("theme", t.name().toLowerCase()));
            json.writeEnd();
        }
    }

    public static class Deserializer implements JsonbDeserializer<IconImpl> {

        @Override
        public IconImpl deserialize(JsonParser parser, DeserializationContext ctx, Type type) {
            String src = null;
            String mimeType = null;
            List<String> sizes = Collections.emptyList();
            Theme theme = null;
            String expectKey = null;

            while (parser.hasNext()) {
                Event event = parser.next();
                if (expectKey == null) {
                    if (event != Event.KEY_NAME) {
                        throw new IllegalStateException();
                    }
                    expectKey = parser.getString();
                } else {
                    switch (expectKey) {
                        case "src" -> src = parser.getString();
                        //case "sizes" -> sizes = ctx.deserialize(type, parser)
                        case "mimeType" -> mimeType = parser.getString();
                        case "theme" -> theme = Theme.valueOf(parser.getString().toUpperCase());
                        default -> { // Do nothing
                        }
                    }
                    expectKey = null;
                }
            }

            return new IconImpl(src, mimeType, sizes, theme);
        }

    }

}
