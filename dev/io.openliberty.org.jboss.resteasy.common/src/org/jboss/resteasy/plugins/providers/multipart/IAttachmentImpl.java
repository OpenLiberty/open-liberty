/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.jboss.resteasy.plugins.providers.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.plugins.providers.ProviderHelper;
import org.jboss.resteasy.util.NoContent;

import com.ibm.websphere.jaxrs20.multipart.IAttachment;


public class IAttachmentImpl implements IAttachment, InputPart {
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private final MultivaluedMap<String, String> headers;
    private final boolean contentTypeFromMessage;
    private final String fieldName;
    private final Optional<String> fileName;
    private final InputStream inputStream;

    private final DataHandler dataHandler = new DataHandler(new DataSource() {

        @Override
        public InputStream getInputStream() throws IOException {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getContentType() {
            return getMediaType().toString();
        }

        @Override
        public String getName() {
            return fileName.orElse(fieldName);
        }});

    // Generally this ctor is used when creating IAttachment on incoming data
    public IAttachmentImpl(InputPart part) throws IOException {
        this(part.getHeaders(), 
             extractFromHeader(part.getHeaders().getFirst(CONTENT_DISPOSITION_HEADER), "name")
                  .orElseThrow(() -> new IOException("Missing required header, Content-Disposition or required attribute, name.")), 
             extractFromHeader(part.getHeaders().getFirst(CONTENT_DISPOSITION_HEADER), "filename"), 
             part.getBody(InputStream.class, null));
    }

    // Generally used when creating IAttachment for outgoing data
    public IAttachmentImpl(MultivaluedMap<String, String> headers, String fieldName, Optional<String> fileName, InputStream inputStream) {
        this.headers = headers;
        contentTypeFromMessage = headers.containsKey(CONTENT_TYPE_HEADER);
        headers.computeIfAbsent(CONTENT_TYPE_HEADER, this::constructContentTypeHeaderValue);
        this.fieldName = fieldName;
        this.fileName = fileName;
        this.inputStream = inputStream;
    }

    @Override
    public String getContentId() {
        return getHeader("Content-ID");
    }

    @Override
    public MediaType getContentType() {
        return getMediaType();
    }

    @Override
    public DataHandler getDataHandler() {
        return dataHandler;
    }

    @Override
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    // From InputPart
    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBody(GenericType<T> type) throws IOException {
        return getBody((Class<T>) type.getRawType(), type.getType());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getBody(Class<T> clazz, Type type) throws IOException {
        if (InputStream.class.equals(clazz)) {
            return (T) inputStream;
        }
        if (String.class.equals(clazz)) {
            if (NoContent.isContentLengthZero(headers)) {
                return (T) "";
            }
            return (T) ProviderHelper.readString(inputStream, getMediaType());
        }
        if (IAttachment.class.equals(clazz) || InputPart.class.equals(clazz)) {
            return (T) this;
        }
        return null;
    }

    @Override
    public String getBodyAsString() throws IOException {
        return getBody(String.class, null);
    }

    @Override
    public MediaType getMediaType() {
        return MediaType.valueOf(headers.getFirst(CONTENT_TYPE_HEADER));
    }

    @Override
    public boolean isContentTypeFromMessage() {
        return contentTypeFromMessage;
    }

    @Override
    public void setMediaType(MediaType mt) {
        headers.putSingle(CONTENT_TYPE_HEADER, mt.toString());
    }

    public String getFileName() {
        return fileName.orElse(null);
    }

    public String getFieldName() {
        return fieldName;
    }

    private List<String> constructContentTypeHeaderValue(String headerName) {
        // Per RFC 7578 ( https://tools.ietf.org/html/rfc7578#section-4.4 ) default to text/plain if not a file
        // or application/octet-stream if it is.
        String mediaType = fileName == null ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_OCTET_STREAM;
        return Collections.singletonList(mediaType);
    }

    public static String getFieldNameFromHeader(String headerValue) {
        return extractFromHeader(headerValue, "name").orElse(null);
    }

    private static Optional<String> extractFromHeader(String headerValue, String field) {
        if (headerValue == null) {
            return Optional.empty();
        }
        int x = headerValue.indexOf(field + "=\"");
        if (x < 0) {
            return Optional.empty();
        }
        x = x + field.length() + 2;
        int y = headerValue.indexOf("\"", x+1);
        return Optional.of(headerValue.substring(x, y));
    }
}
