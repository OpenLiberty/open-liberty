/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2022 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.resteasy.plugins.providers.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.util.Objects;
import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.spi.EntityOutputStream;
import org.jboss.resteasy.plugins.providers.multipart.i18n.Messages;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.UnmodifiableMultivaluedMap;

/**
 * An implementation of the {@link EntityPart.Builder}.
 * <p>
 * This is not intended for direct usage. Use the {@link EntityPart#withName(String)} instead.
 * </p>
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class ResteasyEntityPartBuilder implements EntityPart.Builder {
    private static final Annotation[] ANNOTATIONS = {};
    private final String name;
    private final MultivaluedMap<String, String> headers;
    private MediaType mediaType;
    private String fileName;
    private Content content;


    /**
     * Creates a new builder with the part name.
     *
     * @param name the part name
     */
    public ResteasyEntityPartBuilder(final String name) {
        this.name = name;
        headers = new MultivaluedHashMap<>();
    }

    @Override
    public EntityPart.Builder mediaType(final MediaType mediaType) throws IllegalArgumentException {
        this.mediaType = mediaType;
        return this;
    }

    @Override
    public EntityPart.Builder mediaType(final String mediaTypeString) throws IllegalArgumentException {
        this.mediaType = mediaTypeString == null ? null : MediaType.valueOf(mediaTypeString);
        return this;
    }

    @Override
    public EntityPart.Builder header(final String headerName, final String... headerValues)
            throws IllegalArgumentException {
        headers.addAll(headerName, headerValues);
        return this;
    }

    @Override
    public EntityPart.Builder headers(final MultivaluedMap<String, String> newHeaders) throws IllegalArgumentException {
        headers.putAll(newHeaders);
        return this;
    }

    @Override
    public EntityPart.Builder fileName(final String fileName) throws IllegalArgumentException {
        this.fileName = fileName;
        return this;
    }

    @Override
    public EntityPart.Builder content(final InputStream content) throws IllegalArgumentException {
        this.content = new Content(GenericType.forInstance(
                Objects.requireNonNull(content, Messages.MESSAGES.nullParameter("content"))), content);
        return this;
    }

    @Override
    public <T> EntityPart.Builder content(final T content, final Class<? extends T> type)
            throws IllegalArgumentException {
        this.content = new Content(new GenericType<>(Objects.requireNonNull(type, Messages.MESSAGES.nullParameter("type"))),
                Objects.requireNonNull(content, Messages.MESSAGES.nullParameter("content")));
        return this;
    }

    @Override
    public EntityPart.Builder content(final Object content) throws IllegalArgumentException {
        return content(Objects.requireNonNull(content, Messages.MESSAGES.nullParameter("content")), content.getClass());
    }

    @Override
    public <T> EntityPart.Builder content(final T content, final GenericType<T> type) throws IllegalArgumentException {
        this.content = new Content(Objects.requireNonNull(type, Messages.MESSAGES.nullParameter("type")),
                Objects.requireNonNull(content, Messages.MESSAGES.nullParameter("content")));
        return this;
    }

    @Override
    public EntityPart build() throws IllegalStateException, IOException, WebApplicationException {
        //Liberty change start
        // Per RFC 7578 ( https://tools.ietf.org/html/rfc7578#section-4.4 ) default to text/plain if not a file
        // or application/octet-stream if it is.
        if (this.mediaType == null) {
            if (this.fileName == null) {
                mediaType = MediaType.TEXT_PLAIN_TYPE;
            } else {
                mediaType = MediaType.APPLICATION_OCTET_STREAM_TYPE;
            }
        }
        //Liberty change end

        return new EntityPartImpl(name, headers, mediaType, fileName, content);
    }

    private static class EntityPartImpl implements EntityPart {
        private final String name;
        private final MultivaluedMap<String, String> headers;
        private final MediaType mediaType;
        private final String fileName;
        private final Content content;

        private EntityPartImpl(final String name, final MultivaluedMap<String, String> headers,
                               final MediaType mediaType, final String fileName, final Content content) {
            this.name = name;
            this.headers = new UnmodifiableMultivaluedMap<>(new MultivaluedHashMap<>(headers));
            this.mediaType = mediaType;
            this.fileName = fileName;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Optional<String> getFileName() {
            return Optional.ofNullable(fileName);
        }

        @Override
        public InputStream getContent() {
            try {
                return content.getInputStream(mediaType, headers);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public <T> T getContent(final Class<T> type)
                throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
            return getContent(new GenericType<>(type));
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getContent(final GenericType<T> type)
                throws IllegalArgumentException, IllegalStateException, IOException, WebApplicationException {
            final Providers providers = ResteasyContext.getRequiredContextData(Providers.class);
            final MessageBodyReader<T> reader = (MessageBodyReader<T>) providers.getMessageBodyReader(type.getRawType(), type.getType(), ANNOTATIONS, mediaType);
            if (reader == null) {
                throw new RuntimeException(Messages.MESSAGES.unableToFindMessageBodyReader(mediaType, type.getRawType()
                        .getName()));
            }
            LogMessages.LOGGER.debugf("MessageBodyReader: %s", reader.getClass().getName());

            return reader.readFrom((Class<T>) type.getRawType(), type.getType(), ANNOTATIONS, mediaType, headers, getContent());
        }

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            return headers;
        }

        @Override
        public MediaType getMediaType() {
            return mediaType;
        }

        @Override
        public String toString() {
            return String.format("EntityPart[name=%s, fileName=%s, mediaType=%s, headers=%s, content=%s]", name, fileName, mediaType, headers, content);
        }
    }

    private static class Content {
        final GenericType<?> genericType;
        final Object content;
        final InputPart inputPart;

        private Content(final GenericType<?> genericType, final Object content) {
            this.genericType = genericType;
            this.content = content;
            this.inputPart = null;
        }

        InputStream getInputStream(final MediaType mediaType,
                                   final MultivaluedMap<String, String> headers) throws IOException {
            if (content instanceof InputStream) {
                return (InputStream) content;
            }
            if (content instanceof EntityPart) {
                return ((EntityPart) content).getContent();
            }
            if (content instanceof InputPart) {
                return ((InputPart) content).getBody();
            }
            try (EntityOutputStream out = new EntityOutputStream()) {
                final Providers providers = ResteasyContext.getRequiredContextData(Providers.class);
                @SuppressWarnings("unchecked")
                final MessageBodyWriter<Object> messageBodyWriter = providers.getMessageBodyWriter((Class<Object>) genericType.getRawType(), genericType.getType(), ANNOTATIONS, mediaType);
                messageBodyWriter.writeTo(content, genericType.getRawType(), genericType.getType(), ANNOTATIONS, mediaType, new MultivaluedHashMap<>(headers), out);
                return out.toInputStream();
            }
        }

        @Override
        public String toString() {
            return String.format("Content[content=%s, genericType=%s]", content, genericType);
        }
    }
}
