/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.message;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedSchemeException;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.SchemeValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;

/**
 *
 */
public class NettyRequestMessage extends NettyBaseMessage implements HttpRequestMessage {

    private static final TraceComponent tc = Tr.register(NettyRequestMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final FullHttpRequest request;
    private final HttpHeaders headers;
    private final HttpInboundServiceContext context;
    private HttpChannelConfig config;

    private boolean isIncoming = Boolean.FALSE;
    private final boolean isCommitted = Boolean.FALSE;

    private MethodValues method;
    private SchemeValues scheme;

    private QueryStringDecoder query;

    public NettyRequestMessage(FullHttpRequest request, HttpInboundServiceContext isc) {

        MSP.log("NettyRequestMessage request null:" + Objects.isNull(request));
        MSP.log("NettyRequestMessage isc null: " + Objects.isNull(isc));

        Objects.requireNonNull(request);
        Objects.requireNonNull(isc);

        this.context = isc;
        this.request = request;
        this.headers = request.headers();

        if (isc instanceof HttpInboundServiceContextImpl) {
            this.config = ((HttpInboundServiceContextImpl) isc).getHttpConfig();
            this.isIncoming = ((HttpInboundServiceContextImpl) isc).isInboundConnection();
        }

        super.init(request, isc, config);

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    /**
     * Query whether a body is expected to be present with this message. Note
     * that this is only an expectation and not a definitive answer. This will
     * check the necessary headers, status codes, etc, to see if any indicate
     * a body should be present. Without actually reading for a body, this
     * cannot be sure however.
     *
     * @return boolean (true -- a body is expected to be present)
     */
    @Override
    public boolean isBodyExpected() {
        boolean result = Boolean.FALSE;

        if (HttpUtil.isTransferEncodingChunked(request)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg says chunked encoding: " + this);
            }

            result = Boolean.TRUE;
        }

        else if (HttpUtil.isContentLengthSet(request)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Msg says content-length: " + getContentLength() + " " + this);
            }
            result = Boolean.TRUE;
        }

        if (result) {

        }

        else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No bodyExpected: " + this);
            }
        } //TODO: finish

        return result;

    }

    @Override
    public VersionValues getVersionValue() {
        return VersionValues.find(request.protocolVersion().text());

    }

    @Override
    public String getMethod() {
        if (Objects.isNull(method)) {
            method = MethodValues.find(request.method().name());

        }

        return method.getName();
    }

    @Override
    public MethodValues getMethodValue() {

        if (Objects.isNull(method)) {
            method = MethodValues.find(request.method().name());
        }

        return method;
    }

    @Override
    public void setMethod(String method) throws UnsupportedMethodException {
        this.method = MethodValues.find(method);
        request.setMethod(HttpMethod.valueOf(method));

    }

    @Override
    public void setMethod(byte[] method) throws UnsupportedMethodException {
        setMethod(new String(method, StandardCharsets.UTF_8));

    }

    @Override
    public void setMethod(MethodValues method) {
        this.method = method;
        request.setMethod(HttpMethod.valueOf(method.getName()));

    }

    @Override
    public String getRequestURI() {
        return request.uri();
    }

    @Override
    public byte[] getRequestURIAsByteArray() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public StringBuffer getRequestURL() {
        if (Objects.isNull(query)) {
            query = new QueryStringDecoder(request.uri());
        }
        return new StringBuffer().append(query.path());
    }

    @Override
    public String getRequestURLAsString() {
        if (Objects.isNull(query)) {
            query = new QueryStringDecoder(request.uri());
        }
        return query.path();
    }

    @Override
    public byte[] getRequestURLAsByteArray() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQueryString() {
        if (Objects.isNull(query)) {
            query = new QueryStringDecoder(request.uri());
        }
        return query.path();
    }

    @Override
    public byte[] getQueryStringAsByteArray() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getParameter(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getParameterValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setRequestURL(String url) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRequestURL(byte[] url) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRequestURI(String uri) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRequestURI(byte[] uri) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getURLHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getURLPort() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getVirtualHost() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getVirtualPort() {
        if (Objects.isNull(query)) {

        }
        return 0;
    }

    @Override
    public void setQueryString(String query) {
        this.query = new QueryStringDecoder(query);

    }

    @Override
    public void setQueryString(byte[] query) {
        setQueryString(GenericUtils.getEnglishString(query));

    }

    @Override
    public SchemeValues getSchemeValue() {
        return this.scheme;
    }

    @Override
    public String getScheme() {

        return Objects.isNull(scheme) ? null : scheme.getName();
    }

    @Override
    public void setScheme(SchemeValues scheme) {
        Objects.requireNonNull(scheme);
        this.scheme = scheme;
        //TODO: first line changed needed?
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setScheme(v): " + scheme.getName());
        }
    }

    @Override
    public void setScheme(String scheme) throws UnsupportedSchemeException {
        Objects.requireNonNull(scheme);
        SchemeValues value = SchemeValues.match(scheme, 0, scheme.length());

        if (Objects.isNull(value)) {
            throw new UnsupportedSchemeException("Illegal scheme " + scheme);
        }
        setScheme(value);

    }

    @Override
    public void setScheme(byte[] scheme) throws UnsupportedSchemeException {
        Objects.requireNonNull(scheme);
        SchemeValues value = SchemeValues.match(scheme, 0, scheme.length);
        if (Objects.isNull(value)) {
            throw new UnsupportedSchemeException("Illegal scheme " + GenericUtils.getEnglishString(scheme));
        }
        setScheme(value);

    }

    @Override
    public HttpRequestMessage duplicate() {
        throw new UnsupportedOperationException("The duplicate method is not supported.");

    }

    @Override
    public boolean isPushSupported() {
        throw new UnsupportedOperationException("HTTP/2 delegated to pipeline, isPushSupported method is not supported.");
    }

    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {
        throw new UnsupportedOperationException("HTTP/2 delegated to pipeline, pushNewRequest method is not supported.");

    }

    /**
     *
     * @return request start time with nanosecond precision (relative to the JVM instance as opposed to the time since epoch)
     */
    @Override
    public long getStartTime() {
        return context.getStartNanoTime();
    }

    /**
     * Queries the Inbound Service Context for the remote user. If not set, an empty string
     * is returned.
     *
     * @return
     */
    @Override
    public String getRemoteUser() {

        String remoteUser = null;

        if (context instanceof HttpInboundServiceContextImpl) {
            remoteUser = ((HttpInboundServiceContextImpl) context).getRemoteUser();
        }
        return Objects.nonNull(remoteUser) ? remoteUser : HttpConstants.EMPTY_STRING;
    }

    /**
     * @return
     */
    @Override
    public HttpServiceContextImpl getServiceContext() {

        return (this.context instanceof HttpServiceContextImpl) ? (HttpServiceContextImpl) context : null;
    }

    @Override
    public long getEndTime() {
        return System.nanoTime();
    }

}
