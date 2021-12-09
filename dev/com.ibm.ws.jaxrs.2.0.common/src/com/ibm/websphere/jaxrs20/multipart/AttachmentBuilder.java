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
 * The {@code AttachmentBuilder} class is used to create instances of {@link IAttachment} for sending
 * multipart/form-data payloads in a client request or server response. Instances of the
 * {@code AttachmentBuilder} are not intended to be re-used. Before calling the {@link #build()} method,
 * you must set the {@code inputStream} property using either the {@link #inputStream(InputStream)} method 
 * or the {@link #inputStream(String, InputStream)} method.
 * <p>
 * When building a {@code IAttachment} instance, the {@code contentType} property is optional. If left unset,
 * the default value will be {@code "text/plain"} unless the {@code fileName} property has been set. If the
 * {@code fileName} property is set, then the default value for {@code contentType} will be 
 * {@code "application/octet-stream"}. This behavior is described in
 * <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>.
 * </p><o>
 * Example usage of sending a multipart request using this API might look something like:
 * </p><pre>
 * List&lt;IAttachment&gt; parts = new ArrayList&lt;&gt;();
 * parts.add(AttachmentBuilder.newBuilder("sinpleString")
 *                            .inputStream(new ByteArrayInputStream("Hello World!".getBytes()))
 *                            .build()); // content type for this part will be "text/plain"
 * parts.add(AttachmentBuilder.newBuilder("txtFileWithHeader")
 *                            .inputStream(new FileInputStream("/path/to/myTextFile.txt")
 *                            .fileName("renamedTextFile.txt")
 *                            .header("X-MyCustomHeader", someHeaderValue)
 *                            .build()); // content type for this part will be "application/octet-stream"
 * parts.add(AttachmentBuilder.newBuilder("xmlFile")
 *                            .inputStream("myXmlFile.xml", new FileInputStream("/path/to/myXmlFile.xml"))
 *                            .contentType(MediaType.APPLICATION_XML)
 *                            .build());
 * Client c = ClientBuilder.newClient();
 * WebTarget target = c.target("http://somehost:9080/data/multipart/list");
 * Response r = target.request()
 *                    .header("Content-Type", "multipart/form-data")
 *                    .post(Entity.entity(attachments, MediaType.MULTIPART_FORM_DATA_TYPE));
 * </pre><p>
 * Note that the {@code InputStreams} passed to the builder will be closed by the JAX-RS runtime. Closing
 * the streams prior to sending may result in unexpected behavior.
 * </p>
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

    /**
     * Create a new {@code AttachmentBuilder} instance for creating a new attachment for
     * a multipart payload. The name will be added as a parameter to the part's
     * {@code Content-Disposition} header.
     * See <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, section 4.2</a>
     * for more details.
     * @param fieldName the name of the attachment part
     * @return this builder
     * @throws IllegalArgumentException if the {@code fieldName} is {@code null}
     */
    public static AttachmentBuilder newBuilder(String fieldName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName must be non-null");
        }
        return new AttachmentBuilder(fieldName);
    }

    private AttachmentBuilder(String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * Sets the {@code Content-ID} header in this attachment part. 
     * @param contentId the ID for this particular attachment
     * @return this builder
     */
    public AttachmentBuilder contentId(String contentId) {
        headers.putSingle(CONTENT_ID_HEADER, contentId);
        return this;
    }

    /**
     * Sets the {@code Content-Type} header for this attachment part.
     * This method is analagous to calling {@code contentType(contentType.toString())}
     * @param contentType the {@code MediaType} for this attachment part.
     * @return this builder
     */
    public AttachmentBuilder contentType(MediaType contentType) {
        return contentType(contentType.toString());
    }

    /**
     * Sets the {@code Content-Type} header for this attachment part.
     * @param contentType the content type string for this attachment part.
     * @return this builder
     */
    public AttachmentBuilder contentType(String contentType) {
        headers.putSingle(CONTENT_TYPE_HEADER, contentType);
        return this;
    }

    /**
     * Sets a header for this attachment part.
     * @param headerName the name of the header
     * @param headerValues the (possibly multi-valued) value of the header
     * @return this builder
     */
    public AttachmentBuilder header(String headerName, String...headerValues) {
        if (headerValues.length < 1) {
            headers.remove(headerName);
        } else {
            headers.put(headerName, Arrays.asList(headerValues));
        }
        return this;
    }

    /**
     * Sets headers for this attachment part.
     * @param newHeaders the map of header values to set for this attachment
     * @return this builder
     */
    public AttachmentBuilder headers(MultivaluedMap<String, String> newHeaders) {
        headers.putAll(newHeaders);
        return this;
    }

    /**
     * Sets the file name of this attachment part. If no {@code contentType} is 
     * specified, the default content type will change to {@code "application/octet-stream"}
     * after calling this method. The {@code fileName} parameter value will be added to
     * the {@code Content-Disposition} header. See
     * <a href="https://tools.ietf.org/html/rfc7578#section-4.2">RFC 7578, section 4.2</a>
     * for more details.
     * @param fileName the file name of this attachment part
     * @return this builder
     * @throws IllegalArgumentException if {@code fileName} is {@code null}
     */
    public AttachmentBuilder fileName(String fileName) {
        checkNull("fileName", fileName);
        this.fileName = fileName;
        return this;
    }

    /**
     * Sets the content of this attachment part as an {@code InputStream}. 
     * @param inputStream content body of this attachment part
     * @return this builder
     * @throws IllegalArgumentException if {@code inputStream} is {@code null}
     */
    public AttachmentBuilder inputStream(InputStream inputStream) {
        checkNull("inputStream", inputStream);
        this.inputStream = inputStream;
        return this;
    }

    /**
     * Sets the content of this attachment part as an {@code InputStream}. 
     * Analogous to calling {@code builder.fileName(fileName).inputStream(inputStream)}
     * @param fileName the file name of this attachment part
     * @param inputStream content body of this attachment part

     * @return this builder
     * @throws IllegalArgumentException if {@code fileName} or {@code inputStream} is {@code null}
     * @see {@link #fileName(String)} and {@link #inputStream(InputStream)}
     */
    public AttachmentBuilder inputStream(String fileName, InputStream inputStream) {
        return fileName(fileName).inputStream(inputStream);
    }

    /**
     * Build an instance of an {@code IAttachment} using the properties specified on this builder.
     * @return an instance of {@link IAttachment}
     * @throws IllegalStateException if the {@code inputStream} property has not been set
     */
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
