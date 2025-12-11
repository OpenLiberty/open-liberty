/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2025 Red Hat, Inc., and individual contributors
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
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.jboss.resteasy.resteasy_jaxrs.i18n.LogMessages;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.jboss.resteasy.spi.AsyncMessageBodyWriter;
import org.jboss.resteasy.spi.AsyncOutputStream;
import org.jboss.resteasy.util.DelegatingOutputStream;
import org.jboss.resteasy.util.HttpHeaderNames;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AbstractMultipartWriter {
    protected static final byte[] DOUBLE_DASH_BYTES = "--".getBytes(StandardCharsets.US_ASCII);
    protected static final byte[] LINE_SEPARATOR_BYTES = "\r\n".getBytes(StandardCharsets.US_ASCII);
    protected static final byte[] COLON_SPACE_BYTES = ": ".getBytes(StandardCharsets.US_ASCII);

    @Context
    protected Providers workers;

    /**
     * @deprecated use {@link #write(MultipartOutput, MediaType, MultivaluedMap, OutputStream, Annotation[])}
     */
    @Deprecated
    protected void write(MultipartOutput multipartOutput, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
            throws IOException {
        write(multipartOutput, mediaType, httpHeaders, entityStream, null);
    }

    protected void write(MultipartOutput multipartOutput, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream, Annotation[] annotations)
            throws IOException {
        String boundary = mediaType.getParameters().get("boundary");
        if (boundary == null)
            boundary = multipartOutput.getBoundary();
        httpHeaders.putSingle(HttpHeaderNames.CONTENT_TYPE, mediaType + "; boundary=" + multipartOutput.getBoundary());
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);

        writeParts(multipartOutput, entityStream, boundaryBytes, annotations);
        entityStream.write(boundaryBytes);
        entityStream.write(DOUBLE_DASH_BYTES);
    }

    /**
     * @deprecated use {@link #writeParts(MultipartOutput, OutputStream, byte[], Annotation[])}
     */
    @Deprecated
    protected void writeParts(MultipartOutput multipartOutput, OutputStream entityStream, byte[] boundaryBytes)
            throws IOException {
        writeParts(multipartOutput, entityStream, boundaryBytes, null);
    }

    protected void writeParts(MultipartOutput multipartOutput, OutputStream entityStream, byte[] boundaryBytes,
            Annotation[] annotations)
            throws IOException {
        for (OutputPart part : multipartOutput.getParts()) {
            MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
            writePart(entityStream, boundaryBytes, part, headers, annotations);
        }
    }

    /**
     * @deprecated use {@link #writePart(OutputStream, byte[], OutputPart, MultivaluedMap, Annotation[])}
     */
    @Deprecated
    protected void writePart(OutputStream entityStream, byte[] boundaryBytes, OutputPart part,
            MultivaluedMap<String, Object> headers)
            throws IOException {
        writePart(entityStream, boundaryBytes, part, headers, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void writePart(OutputStream entityStream, byte[] boundaryBytes, OutputPart part,
            MultivaluedMap<String, Object> headers, Annotation[] annotations)
            throws IOException {
        entityStream.write(boundaryBytes);
        entityStream.write(LINE_SEPARATOR_BYTES);
        headers.putAll(part.getHeaders());
        headers.putSingle(HttpHeaderNames.CONTENT_TYPE, part.getMediaType());

        Object entity = part.getEntity();
        Class<?> entityType = part.getType();
        Type entityGenericType = part.getGenericType();
        MessageBodyWriter writer = workers.getMessageBodyWriter(entityType, entityGenericType, annotations,
                part.getMediaType());
        LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());
        OutputStream partStream = new DelegatingOutputStream(entityStream) {
            @Override
            public void close() {
                // no close
                // super.close();
            }
        };
        //Liberty change start:  Can be removed when https://github.com/resteasy/resteasy/pull/4443 is added
        final HeaderFlushedOutputStream headerFlushedOutputStream = new HeaderFlushedOutputStream(headers, partStream);
        writer.writeTo(entity, entityType, entityGenericType, annotations, part.getMediaType(), headers,
                headerFlushedOutputStream);
        // Flush the headers for cases where the entity was empty
        headerFlushedOutputStream.flushHeaders();
        //Liberty change end
        
        entityStream.write(LINE_SEPARATOR_BYTES);
    }

    /**
     * @deprecated use {@link #asyncWrite(MultipartOutput, MediaType, MultivaluedMap, AsyncOutputStream, Annotation[])}
     */
    @Deprecated
    protected CompletionStage<Void> asyncWrite(MultipartOutput multipartOutput, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            AsyncOutputStream entityStream) {
        return asyncWrite(multipartOutput, mediaType, httpHeaders, entityStream, null);
    }

    protected CompletionStage<Void> asyncWrite(MultipartOutput multipartOutput, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, AsyncOutputStream entityStream,
            Annotation[] annotations) {
        String boundary = mediaType.getParameters().get("boundary");
        if (boundary == null)
            boundary = multipartOutput.getBoundary();
        httpHeaders.putSingle(HttpHeaderNames.CONTENT_TYPE, mediaType + "; boundary=" + multipartOutput.getBoundary());
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);

        return asyncWriteParts(multipartOutput, entityStream, boundaryBytes, annotations)
                .thenCompose(v -> entityStream.asyncWrite(boundaryBytes))
                .thenCompose(v -> entityStream.asyncWrite(DOUBLE_DASH_BYTES));
    }

    /**
     * @deprecated use {@link #asyncWriteParts(MultipartOutput, AsyncOutputStream, byte[], Annotation[])}
     */
    @Deprecated
    protected CompletionStage<Void> asyncWriteParts(MultipartOutput multipartOutput, AsyncOutputStream entityStream,
            byte[] boundaryBytes) {
        return asyncWriteParts(multipartOutput, entityStream, boundaryBytes, null);
    }

    protected CompletionStage<Void> asyncWriteParts(MultipartOutput multipartOutput, AsyncOutputStream entityStream,
            byte[] boundaryBytes, Annotation[] annotations) {
        CompletionStage<Void> ret = CompletableFuture.completedFuture(null);
        for (OutputPart part : multipartOutput.getParts()) {
            MultivaluedMap<String, Object> headers = new MultivaluedMapImpl<>();
            ret = ret.thenCompose(v -> asyncWritePart(entityStream, boundaryBytes, part, headers, annotations));
        }
        return ret;
    }

    /**
     * @deprecated use {@link #asyncWritePart(AsyncOutputStream, byte[], OutputPart, MultivaluedMap, Annotation[])}
     */
    @Deprecated
    protected CompletionStage<Void> asyncWritePart(AsyncOutputStream entityStream, byte[] boundaryBytes,
            OutputPart part, MultivaluedMap<String, Object> headers) {
        return asyncWritePart(entityStream, boundaryBytes, part, headers, null);
    }

    @SuppressWarnings(value = "unchecked")
    protected CompletionStage<Void> asyncWritePart(AsyncOutputStream entityStream, byte[] boundaryBytes,
            OutputPart part, MultivaluedMap<String, Object> headers,
            Annotation[] annotations) {
        headers.putAll(part.getHeaders());
        headers.putSingle(HttpHeaderNames.CONTENT_TYPE, part.getMediaType());

        Object entity = part.getEntity();
        Class<?> entityType = part.getType();
        Type entityGenericType = part.getGenericType();
        final MessageBodyWriter<Object> writer = (MessageBodyWriter<Object>) workers.getMessageBodyWriter(entityType,
                entityGenericType, annotations, part.getMediaType());
        LogMessages.LOGGER.debugf("MessageBodyWriter: %s", writer.getClass().getName());
        final Function<Void, CompletionStage<Void>> writeFunction;
        // Check if this is an AsyncMessageBodyWriter, if it is then when can write asynchronously.
        if (writer instanceof AsyncMessageBodyWriter) {
            writeFunction = unused -> ((AsyncMessageBodyWriter<Object>) writer)
                    .asyncWriteTo(entity, entityType, entityGenericType, annotations, part.getMediaType(), headers,
                            new HeaderFlushedAsyncOutputStream(headers, entityStream));
        } else {
            // The part message body writer is not async, so we have to write synchronously.
            LogMessages.LOGGER.debugf("MessageBodyWriter %s is not asynchronous.", writer.getClass().getName());
            writeFunction = unused -> {
                try {
                    writer.writeTo(entity, entityType, entityGenericType, annotations, part.getMediaType(), headers,
                            new HeaderFlushedAsyncOutputStream(headers, entityStream));
                    return CompletableFuture.completedFuture(null);
                } catch (IOException e) {
                    return ProviderHelper.completedException(e);
                }
            };
        }
        return entityStream.asyncWrite(boundaryBytes)
                .thenCompose(v -> entityStream.asyncWrite(LINE_SEPARATOR_BYTES))
                .thenCompose(writeFunction)
                .thenCompose(v -> entityStream.asyncWrite(LINE_SEPARATOR_BYTES));
    }
}
