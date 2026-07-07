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

import org.mcpjava.server.resources.BlobResourceContents;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;

/**
 * Record implementation of BlobResourceContents
 */
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
}
