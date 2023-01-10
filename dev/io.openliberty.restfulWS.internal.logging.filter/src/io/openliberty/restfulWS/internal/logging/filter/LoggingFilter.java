/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.restfulWS.internal.logging.filter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.Map.Entry;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Providers;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Logs HTTP request/response data when enabled. This filter attempts to reconstruct the
 * inbound HTTP request (it does not attempt to preserve header order, relative paths, etc.)
 * and the outbound HTTP response from the server. 
 */
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private final static TraceComponent tc = Tr.register(LoggingFilter.class);
    private final static String LS = System.lineSeparator();
    private final static String REQUEST_TEMPLATE = Tr.formatMessage(tc, "HTTP_REQUEST") + LS
                                                 + "> %s %s" + LS + LS  // HTTP method + path
                                                 + "%s" + LS + LS       // Headers
                                                 + "%s";                // HTTP body

    private final static String RESPONSE_TEMPLATE = Tr.formatMessage(tc, "HTTP_REQUEST") + LS 
                                                  + "< %d %s" + LS + LS // HTTP status code + description
                                                  + "%s" + LS + LS      // Headers
                                                  + "%s";               // HTTP body

    private final static String UNLOGGED = Tr.formatMessage(tc, "UNLOGGED");
    private final static String NO_ENTITY = Tr.formatMessage(tc, "NO_ENTITY");
    private final boolean LOG_ENTITY = AccessController.doPrivileged((PrivilegedAction<Boolean>) () -> 
        Boolean.getBoolean("io.openliberty.restfulWS.logging.filter.logHTTPEntities"));
    

    @Context
    Providers providers;

    @Override
    public void filter(ContainerRequestContext reqCtx) throws IOException {
        final String httpMethod = reqCtx.getMethod();
        StringBuilder sb = new StringBuilder();
        UriInfo uriInfo = reqCtx.getUriInfo();
        sb.append(uriInfo.getBaseUri()).append(" ").append(uriInfo.getPath());
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        if (!queryParams.isEmpty()) {
            sb.append("?");
            for (Entry<String, List<String>> e : queryParams.entrySet()) {
                for (String value : e.getValue()) {
                    sb.append(e.getKey()).append("=").append(value).append("&");
                }
            }
            sb.deleteCharAt(sb.length()-1); //remove last ampersand
        }
        final String path = sb.toString();

        sb = new StringBuilder();
        for (String headerName : reqCtx.getHeaders().keySet()) {
            sb.append(headerName).append(": ").append(reqCtx.getHeaderString(headerName)).append(LS);
        }
        final String headers = sb.toString();

        // it's expensive to read the HTTP entity (requires copying the stream - and the full stream onto the heap)
        // so best to avoid unless user really wants to see it:
        final String entity;
        if (!LOG_ENTITY) {
            entity = UNLOGGED;
        } else if (!reqCtx.hasEntity()) {
            entity = NO_ENTITY;
        } else {
            byte[] entityBytes = toByteArray(reqCtx.getEntityStream());
            reqCtx.setEntityStream(new ByteArrayInputStream(entityBytes));
            entity = new String(entityBytes, charset(reqCtx.getMediaType()));
        }

        Tr.info(tc, String.format(REQUEST_TEMPLATE, httpMethod, path, headers, entity));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void filter(ContainerRequestContext reqCtx, ContainerResponseContext respCtx) throws IOException {
        final int responseCode = respCtx.getStatusInfo().getStatusCode();
        final String responsePhrase = respCtx.getStatusInfo().getReasonPhrase();
        
        StringBuilder sb = new StringBuilder();
        MultivaluedMap<String, Object> httpHeaders = respCtx.getHeaders();
        for (String headerName : httpHeaders.keySet()) {
            sb.append(headerName).append(": ").append(respCtx.getHeaderString(headerName)).append(LS);
        }
        final String headers = sb.toString();
        
        final String entity;
        if (!LOG_ENTITY) {
            entity = UNLOGGED;
        } else if (!respCtx.hasEntity()) {
            entity = NO_ENTITY;
        } else {
            
            Object entityObj = respCtx.getEntity();
            Class<?> entityClass = respCtx.getEntityClass();
            Annotation[] entityAnnotations = respCtx.getEntityAnnotations();
            Type entityType = respCtx.getEntityType();
            MediaType entityMediaType = respCtx.getMediaType();
            MessageBodyWriter<Object> mbw = (MessageBodyWriter<Object>) providers.getMessageBodyWriter(entityClass, entityType, entityAnnotations, entityMediaType);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            mbw.writeTo(entityObj, entityClass, entityType, entityAnnotations, entityMediaType, httpHeaders, baos);
            entity = new String(baos.toByteArray(), charset(entityMediaType));
        }

        Tr.info(tc, String.format(RESPONSE_TEMPLATE, responseCode, responsePhrase, headers, entity));
    }

    private static String charset(MediaType mt) {
        return Optional.ofNullable(mt.getParameters().get(MediaType.CHARSET_PARAMETER)).orElse("UTF-8");
    }

    private static byte[] toByteArray(InputStream original) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[2048];
        int len;
        while ((len = original.read(buf)) > -1 ) {
            baos.write(buf, 0, len);
        }
        baos.flush();
        
        return baos.toByteArray();
    }
}