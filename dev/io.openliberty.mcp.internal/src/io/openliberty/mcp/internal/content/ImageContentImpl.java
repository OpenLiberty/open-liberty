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

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.ImageContent;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of {@link ImageContent}
 */
@JsonbTypeSerializer(ImageContentImpl.Serializer.class)
public record ImageContentImpl(Annotations annotationsValue, byte[] data, String mimeType, Map<String, Object> metadata) implements ImageContent {

    @Override
    public Optional<Annotations> annotations() {
        return Optional.ofNullable(annotationsValue);
    }

    /**
     * Builder implementation for {@link ImageContent}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements ImageContent.Builder {

        private Annotations annotations;
        private final byte[] data;
        private final String mimeType;

        public Builder(byte[] data, String mimeType) {
            Objects.requireNonNull(data, "data must not be null");
            Objects.requireNonNull(mimeType, "mimeType must not be null");
            this.data = data;
            this.mimeType = mimeType;
        }

        @Override
        public ImageContent.Builder setAnnotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        @Override
        public ImageContent build() {
            return new ImageContentImpl(annotations, data, mimeType, Map.copyOf(metadata));
        }
    }

    public static class Serializer implements JsonbSerializer<ImageContentImpl> {

        @Override
        public void serialize(ImageContentImpl obj, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("type", "image");
            json.write("mimeType", obj.mimeType());
            json.write("data", Base64.getEncoder().encodeToString(obj.data()));
            obj.annotations().ifPresent(a -> ctx.serialize("annotations", a, json));
            if (!obj.metadata().isEmpty()) {
                ctx.serialize("_meta", obj.metadata(), json);
            }
            json.writeEnd();
        }

    }
}
