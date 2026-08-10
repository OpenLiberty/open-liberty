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
import java.util.OptionalLong;

import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.ResourceLink;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of {@link ResourceLink}
 */
@JsonbTypeSerializer(ResourceLinkImpl.Serializer.class)
public record ResourceLinkImpl(String name,
                               String uri,
                               String titleValue,
                               String descriptionValue,
                               String mimeTypeValue,
                               Annotations annotationsValue,
                               OptionalLong size,
                               Map<String, Object> metadata) implements ResourceLink {

    @Override
    public String title() {
        return titleValue;
    }

    @Override
    public Optional<String> description() {
        return Optional.ofNullable(descriptionValue);
    }

    @Override
    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeTypeValue);
    }

    @Override
    public Optional<Annotations> annotations() {
        return Optional.ofNullable(annotationsValue);
    }

    /**
     * Builder implementation for {@link ResourceLink}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements ResourceLink.Builder {

        private final String name;
        private final String uri;
        private String title;
        private String description;
        private String mimeType;
        private Annotations annotations;
        private OptionalLong size = OptionalLong.empty();

        /**
         * @param name
         * @param uri
         */
        public Builder(String name, String uri) {
            Objects.requireNonNull(name);
            Objects.requireNonNull(uri);
            this.name = name;
            this.uri = uri;
        }

        @Override
        public ResourceLink.Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        @Override
        public ResourceLink.Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        @Override
        public ResourceLink.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public ResourceLink.Builder setAnnotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        @Override
        public ResourceLink.Builder setSize(long size) {
            this.size = OptionalLong.of(size);
            return this;
        }

        @Override
        public ResourceLink build() {
            return new ResourceLinkImpl(name, uri, title, description, mimeType, annotations, size, Map.copyOf(metadata));
        }
    }

    public static class Serializer implements JsonbSerializer<ResourceLinkImpl> {

        @Override
        public void serialize(ResourceLinkImpl obj, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("type", "resource_link");
            json.write("name", obj.name());
            json.write("uri", obj.uri());
            if (obj.title() != null) {
                json.write("title", obj.title());
            }
            obj.description().ifPresent(d -> json.write("description", d));
            obj.mimeType().ifPresent(m -> json.write("mimeType", m));
            if (obj.size().isPresent()) {
                json.write("size", obj.size().getAsLong());
            }
            obj.annotations().ifPresent(a -> ctx.serialize("annotations", a, json));
            if (!obj.metadata().isEmpty()) {
                ctx.serialize("_meta", obj.metadata(), json);
            }
            json.writeEnd();
        }
    }

}
