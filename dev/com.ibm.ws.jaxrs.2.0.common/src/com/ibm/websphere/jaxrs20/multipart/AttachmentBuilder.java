/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.jaxrs20.multipart;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;


import com.ibm.ws.jaxrs20.multipart.impl.AttachmentImpl;

/**
 *
 */
public class AttachmentBuilder {
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_ID_HEADER = "Content-ID";
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    private static final List<String> DEFAULT_CONTENT_ID = Collections.singletonList("[root.message@openliberty.io]");
    private InputStream inputStream;
    private String fileName;
    private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
    private final String fieldName;

    public static AttachmentBuilder newBuilder(String fieldName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must be non-null");
        }
        return new AttachmentBuilder(fieldName);
    }

    private AttachmentBuilder(String fieldName) {
        this.fieldName = fieldName;
    }

    public AttachmentBuilder contentId(String contentId) {
        headers.putSingle(CONTENT_ID_HEADER, contentId);
        return this;
    }

    public AttachmentBuilder contentType(MediaType contentType) {
        return contentType(contentType.toString());
    }

    public AttachmentBuilder contentType(String contentType) {
        headers.putSingle(CONTENT_TYPE_HEADER, contentType);
        return this;
    }

    public AttachmentBuilder header(String headerName, String...headerValues) {
        if (headerValues.length < 1) {
            headers.remove(headerName);
        } else {
            headers.put(headerName, Arrays.asList(headerValues));
        }
        return this;
    }

    public AttachmentBuilder headers(MultivaluedMap<String, String> newHeaders) {
        headers.putAll(newHeaders);
        return this;
    }

    public AttachmentBuilder fileName(String fileName) {
        checkNull("fileName", fileName);
        this.fileName = fileName;
        return this;
    }

    public AttachmentBuilder inputStream(InputStream inputStream) {
        checkNull("inputStream", inputStream);
        this.inputStream = inputStream;
        return this;
    }

    public AttachmentBuilder inputStream(String fileName, InputStream inputStream) {
        return fileName(fileName).inputStream(inputStream);
    }

    public IAttachment build() {
        if (inputStream == null) {
            throw new IllegalStateException("inputStream must be set");
        }
        headers.putIfAbsent(CONTENT_ID_HEADER, DEFAULT_CONTENT_ID);
        headers.computeIfAbsent(CONTENT_DISPOSITION_HEADER, this::constructContentDispositionHeaderValue);
        headers.computeIfAbsent(CONTENT_TYPE_HEADER, this::constructContentTypeHeaderValue);
        Attachment att = new Attachment(inputStream, headers);
        AttachmentImpl attImpl = new AttachmentImpl(att);
        return attImpl;
    }

    private List<String> constructContentDispositionHeaderValue(String headerName) {
        StringBuilder sb = new StringBuilder();
        sb.append("form-data; name=\"");
        sb.append(fieldName);
        sb.append("\"");
        if (fileName != null) {
            sb.append("; filename=\"");
            sb.append(fileName);
            sb.append("\"");
        }
        return Collections.singletonList(sb.toString());
    }

    private List<String> constructContentTypeHeaderValue(String headerName) {
        // Per RFC 7578 ( https://tools.ietf.org/html/rfc7578#section-4.4 ) default to text/plain if not a file
        // or application/octet-stream if it is.
        String mediaType = fileName == null ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_OCTET_STREAM;
        return Collections.singletonList(mediaType);
    }

    private static void checkNull(String attr, Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException(attr + " must not be null");
        }
    }
}
