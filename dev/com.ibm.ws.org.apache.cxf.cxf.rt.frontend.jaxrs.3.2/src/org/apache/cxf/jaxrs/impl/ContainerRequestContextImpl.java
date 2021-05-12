/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.DelegatingInputStream;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

public class ContainerRequestContextImpl extends AbstractRequestContextImpl
    implements ContainerRequestContext {

    private static final String ENDPOINT_ADDRESS_PROPERTY = "org.apache.cxf.transport.endpoint.address";

    private boolean preMatch;
    public ContainerRequestContextImpl(Message message, boolean preMatch, boolean responseContext) {
        super(message, responseContext);
        this.preMatch = preMatch;
    }

    @Override
    public InputStream getEntityStream() {
        return m.getContent(InputStream.class);
    }


    @Override
    public Request getRequest() {
        return new RequestImpl(m);
    }

    @Override
    public SecurityContext getSecurityContext() {
        SecurityContext sc = m.get(SecurityContext.class);
        return sc == null ? new SecurityContextImpl(m) : sc;
    }

    @Override
    public UriInfo getUriInfo() {
        return new UriInfoImpl(m);
    }

    @Override
    public boolean hasEntity() {
        InputStream is = getEntityStream();
        if (is == null) {
            return false;
        }
        // Is Content-Length is explicitly set to 0 ?
        if (HttpUtils.isPayloadEmpty(getHeaders())) {
            return false;
        }

        if (is instanceof DelegatingInputStream) {
            ((DelegatingInputStream) is).cacheInput();
        }

        try {
            return !IOUtils.isEmpty(is);
        } catch (IOException ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    @Override
    public void setEntityStream(InputStream is) {
        checkContext();
        m.setContent(InputStream.class, is);
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        h = null;
        return HttpUtils.getModifiableStringHeaders(m);
    }


    @Override
    public void setRequestUri(URI requestUri) throws IllegalStateException {
        if (requestUri.isAbsolute()) {
            String baseUriString = new UriInfoImpl(m).getBaseUri().toString();
            String requestUriString = requestUri.toString();
            if (!requestUriString.startsWith(baseUriString)) {
                setRequestUri(requestUri, URI.create("/"));
                return;
            }
            requestUriString = requestUriString.substring(baseUriString.length());
            if (requestUriString.isEmpty()) {
                requestUriString = "/";
            }
            requestUri = URI.create(requestUriString);

        }
        doSetRequestUri(requestUri);
    }

    protected void doSetRequestUri(URI requestUri) throws IllegalStateException {
        checkNotPreMatch();
        //Liberty change - using requestUri.toString() to pass CTS
        // this was changed after 3.1.8 in the community but the CTS
        // requires the full toString().  we may try to change this
        // back in the community too, but for now we are only changing
        // it here.
        HttpUtils.resetRequestURI(m, requestUri.toString());
        String query = requestUri.getRawQuery();
        if (query != null) {
            m.put(Message.QUERY_STRING, query);
        }
    }

    @Override
    public void setRequestUri(URI baseUri, URI requestUri) throws IllegalStateException {
        doSetRequestUri(requestUri);
        Object servletRequest = m.get("HTTP.REQUEST");
        if (servletRequest != null) {
            ((javax.servlet.http.HttpServletRequest)servletRequest)
                .setAttribute(ENDPOINT_ADDRESS_PROPERTY, baseUri.toString());
        }
    }

    @Override
    public void setSecurityContext(SecurityContext sc) {
        checkContext();
        m.put(SecurityContext.class, sc);
        if (sc instanceof org.apache.cxf.security.SecurityContext) {
            m.put(org.apache.cxf.security.SecurityContext.class,
                  (org.apache.cxf.security.SecurityContext)sc);
        }
    }

    private void checkNotPreMatch() {
        if (!preMatch) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void setMethod(String method) throws IllegalStateException {
        checkNotPreMatch();
        super.setMethod(method);
    }
}
