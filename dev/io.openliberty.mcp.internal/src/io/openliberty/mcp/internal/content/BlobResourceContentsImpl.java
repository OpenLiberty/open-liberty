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

import org.mcpjava.server.resources.BlobResourceContents;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;

/**
 * Record implementation of BlobResourceContents
 */
@JsonbTypeSerializer(BlobResourceContentsImpl.Serializer.class)
public record BlobResourceContentsImpl(String uri,
                                       byte[] blob,
                                       String mimeTypeValue,
                                       Map<String, Object> metadata) implements BlobResourceContents {

    @Override
    public Optional<String> mimeType() {
        return Optional.ofNullable(mimeTypeValue);
    }

    /**
     * Builder implementation for {@link BlobResourceContents}
     */
    public static class Builder extends MetaCarrierBuilderImpl<Builder> implements BlobResourceContents.Builder {

        private final String uri;
        private final byte[] blob;
        private String mimeType;

        /**
         * @param uri
         * @param blob
         */
        public Builder(String uri, byte[] blob) {
            Objects.requireNonNull(uri, "uri must not be null");
            Objects.requireNonNull(blob, "blob must not be null");
            this.uri = uri;
            this.blob = blob;
        }

        @Override
        public BlobResourceContents.Builder setMimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        @Override
        public BlobResourceContents build() {
            return new BlobResourceContentsImpl(uri, blob, mimeType, Map.copyOf(metadata));
        }
    }

    public static class Serializer implements JsonbSerializer<BlobResourceContentsImpl> {

        @Override
        public void serialize(BlobResourceContentsImpl obj, JsonGenerator json, SerializationContext ctx) {
            json.writeStartObject();
            json.write("uri", obj.uri());
            json.write("blob", Base64.getEncoder().encodeToString(obj.blob()));
            obj.mimeType().ifPresent(m -> json.write("mimeType", m));
            if (!obj.metadata().isEmpty()) {
                ctx.serialize("_meta", obj.metadata(), json);
            }
            json.writeEnd();
        }
    }
}
