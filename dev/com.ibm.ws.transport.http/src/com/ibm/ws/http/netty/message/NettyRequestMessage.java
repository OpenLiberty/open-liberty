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

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ibm.ejs.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpRequestMessageImpl;
import com.ibm.ws.http.channel.internal.HttpTrailersImpl;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.values.ConnectionValues;
import com.ibm.wsspi.http.channel.values.ContentEncodingValues;
import com.ibm.wsspi.http.channel.values.ExpectValues;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.SchemeValues;
import com.ibm.wsspi.http.channel.values.TransferEncodingValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpUtil;

/**
 *
 */
public class NettyRequestMessage extends HttpRequestMessageImpl {

    private static final TraceComponent tc = Tr.register(NettyRequestMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private final FullHttpRequest request;

    public NettyRequestMessage(FullHttpRequest request) {
        Objects.requireNonNull(request);
        this.request = request;
    }

    @Override
    public boolean isIncoming() {
        return this.getServiceContext().isInboundConnection();
    }

    @Override
    public boolean isCommitted() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCommitted() {
        // TODO Auto-generated method stub

    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub

    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isBodyExpected() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isBodyAllowed() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setContentLength(long length) {
        HttpUtil.setContentLength(request, length);

    }

    @Override
    public long getContentLength() {
        // TODO Auto-generated method stub
        return HttpUtil.getContentLength(request);
    }

    @Override
    public void setConnection(ConnectionValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConnection(ConnectionValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public ConnectionValues[] getConnection() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isKeepAliveSet() {
        return HttpUtil.isKeepAlive(request);
    }

    @Override
    public boolean isConnectionSet() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setContentEncoding(ContentEncodingValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setContentEncoding(ContentEncodingValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public ContentEncodingValues[] getContentEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setTransferEncoding(TransferEncodingValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setTransferEncoding(TransferEncodingValues[] values) {
        // TODO Auto-generated method stub

    }

    @Override
    public TransferEncodingValues[] getTransferEncoding() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isChunkedEncodingSet() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setCurrentDate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setExpect(ExpectValues value) {
        // TODO Auto-generated method stub

    }

    @Override
    public byte[] getExpect() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isExpect100Continue() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getMIMEType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMIMEType(String type) {
        // TODO Auto-generated method stub

    }

    @Override
    public Charset getCharset() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setCharset(Charset set) {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpTrailers getTrailers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public VersionValues getVersionValue() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getVersion() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setVersion(VersionValues version) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVersion(String version) throws UnsupportedProtocolVersionException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setVersion(byte[] version) throws UnsupportedProtocolVersionException {
        // TODO Auto-generated method stub

    }

    @Override
    public HttpTrailersImpl createTrailers() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDebugContext(Object o) {
        // TODO Auto-generated method stub

    }

    @Override
    public HeaderField getHeader(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeaderField getHeader(byte[] name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HeaderField getHeader(HeaderKeys name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(byte[] name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getHeaders(HeaderKeys name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HeaderField> getAllHeaders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllHeaderNames() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<String> getAllHeaderNamesSet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void appendHeader(byte[] header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(byte[] header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(HeaderKeys header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void appendHeader(String header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getNumberOfHeaderInstances(String header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean containsHeader(byte[] header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsHeader(HeaderKeys header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsHeader(String header) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getNumberOfHeaderInstances(byte[] header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int getNumberOfHeaderInstances(HeaderKeys header) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void removeHeader(byte[] header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(byte[] header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(HeaderKeys header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(HeaderKeys header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeHeader(String header, int instance) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeAllHeaders() {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(byte[] header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(byte[] header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(byte[] header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(HeaderKeys header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public HeaderField setHeaderIfAbsent(HeaderKeys header, String value) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setHeader(String header, byte[] value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(String header, byte[] value, int offset, int length) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setHeader(String header, String value) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setLimitOnNumberOfHeaders(int number) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLimitOnNumberOfHeaders() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setLimitOfTokenSize(int size) {
        // TODO Auto-generated method stub

    }

    @Override
    public int getLimitOfTokenSize() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public byte[] getCookieValue(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getAllCookieValues(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public HttpCookie getCookie(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HttpCookie> getAllCookies() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<HttpCookie> getAllCookies(String name) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean setCookie(HttpCookie cookie, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean setCookie(String name, String value, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeCookie(String name, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean containsCookie(String name, HttpHeaderKeys cookieHeader) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getMethod() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMethod(String method) throws UnsupportedMethodException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMethod(byte[] method) throws UnsupportedMethodException {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMethod(MethodValues method) {
        // TODO Auto-generated method stub

    }

    @Override
    public StringBuffer getRequestURL() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getQueryString() {
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
    public void setQueryString(String query) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setQueryString(byte[] query) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setScheme(SchemeValues scheme) {
        this.myScheme = scheme;
        // Tr.debug(tc, "setScheme(v): " + (Objects.nonNull(scheme) ? scheme.getName() : null));

    }

    @Override
    public HttpRequestMessage duplicate() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isPushSupported() {
        //Tr.debug(tc, "Use of HTTP/2 is delegated to Netty Pipeline Handler, returning false.");
        return Boolean.FALSE;
    }

    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {
        throw new UnsupportedOperationException("Use Netty HTTP/2 handler, not legacy Request Message");

    }

}
