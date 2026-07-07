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
import java.util.Optional;

import org.mcpjava.server.content.Annotations;
import org.mcpjava.server.content.EmbeddedResource;
import org.mcpjava.server.resources.BlobResourceContents;
import org.mcpjava.server.resources.ResourceContents;
import org.mcpjava.server.resources.TextResourceContents;

import io.openliberty.mcp.internal.spi.MetaCarrierBuilderImpl;

/**
 * Record implementation of {@link EmbeddedResource}
 */
public record EmbeddedResourceImpl(ResourceContents resource, Annotations annotationsValue, Map<String, Object> metadata) implements EmbeddedResource {

    @Override
    public Optional<Annotations> annotations() {
        return Optional.ofNullable(annotationsValue);
    }

    /**
     * Builder implementation for an {@link EmbeddedResource} containing a {@link TextResourceContents}
     */
    public static class TextResourceBuilder extends MetaCarrierBuilderImpl<TextResourceBuilder> implements EmbeddedResource.Builder {

        private final TextResourceContentsImpl.Builder contentBuilder;
        private Annotations annotations;

        public TextResourceBuilder(TextResourceContentsImpl.Builder contentBuilder) {
            this.contentBuilder = contentBuilder;
        }

        @Override
        public Builder putResourceMeta(String key, Object value) {
            contentBuilder.putMetadata(key, value);
            return this;
        }

        @Override
        public Builder setAnnotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        @Override
        public Builder setMimeType(String mimeType) {
            contentBuilder.setMimeType(mimeType);
            return this;
        }

        @Override
        public EmbeddedResource build() {
            return new EmbeddedResourceImpl(contentBuilder.build(), annotations, Map.copyOf(metadata));
        }
    }

    /**
     * Builder implementation for an {@link EmbeddedResource} containing a {@link BlobResourceContents}
     */
    public static class BlobResourceBuilder extends MetaCarrierBuilderImpl<TextResourceBuilder> implements EmbeddedResource.Builder {

        private final BlobResourceContentsImpl.Builder contentBuilder;
        private Annotations annotations;

        public BlobResourceBuilder(BlobResourceContentsImpl.Builder contentBuilder) {
            this.contentBuilder = contentBuilder;
        }

        @Override
        public Builder putResourceMeta(String key, Object value) {
            contentBuilder.putMetadata(key, value);
            return this;
        }

        @Override
        public Builder setAnnotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        @Override
        public Builder setMimeType(String mimeType) {
            contentBuilder.setMimeType(mimeType);
            return this;
        }

        @Override
        public EmbeddedResource build() {
            return new EmbeddedResourceImpl(contentBuilder.build(), annotations, Map.copyOf(metadata));
        }
    }

}
