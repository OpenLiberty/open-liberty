/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package com.ibm.ws.http.netty;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.channel.internal.outbound.HttpOutputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherLink;
import com.ibm.ws.http.dispatcher.internal.channel.HttpResponseImpl;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpOutputStreamEE7;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.ServerCookieEncoder;
import io.openliberty.http.ext.HttpResponseExt;

/**
 * Implementation of the public HTTP transport response message for the dispatcher
 * and container traffic.
 */
@Trivial
public class NettyHttpResponseImpl extends HttpResponseImpl implements HttpResponse, HttpResponseExt {
    /**
     * @param link
     */
    public NettyHttpResponseImpl(HttpDispatcherLink link) {
        super(link);

    }

    private HttpInboundServiceContext isc = null;
    private HttpOutputStreamImpl body = null;
    private final HttpDispatcherLink connlink = null;

    private ChannelHandlerContext nettyContext;
    private io.netty.handler.codec.http.HttpResponse nettyResponse;
    private FullHttpRequest nettyRequest;

    private final boolean committed = false;
    private final long contentLength = 0;

    /**
     * Initialize with a new wrapped message.
     *
     * @param context
     */
    @Override
    public void init(HttpInboundServiceContext context) {
        System.out.println("MSP: init  netty response");
        this.isc = context;
        this.body = null;
        this.nettyContext = ((HttpInboundServiceContextImpl) context).getNettyContext();
        this.nettyRequest = ((HttpInboundServiceContextImpl) context).getNettyRequest();
        this.nettyResponse = new DefaultHttpResponse(nettyRequest.getProtocolVersion(), HttpResponseStatus.OK);
        ((HttpInboundServiceContextImpl) isc).setNettyResponse(nettyResponse);
        System.out.println("Init netty response complete");
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#isCommitted()
     */
    @Override
    public boolean isCommitted() {
        return committed;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#addCookie(com.ibm.websphere.http.Cookie)
     */
    @Override
    public void addCookie(HttpCookie cookie) {

        this.nettyResponse.headers().add(HttpHeaderKeys.HDR_SET_COOKIE.getName(), ServerCookieEncoder.encode(cookie.getName(), cookie.getValue()));
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {

        return null;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getCookies(String name) {
        return null;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#isPersistent()
     */
    @Override
    public boolean isPersistent() {
        return isc.isPersistent();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getCookies()
     */
    @Override
    public List<HttpCookie> getCookies() {
        return null;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeCookie(com.ibm.websphere.http.HttpCookie)
     */
    @Override
    public void removeCookie(HttpCookie cookie) {
        //this.message.removeCookie(cookie.getName(), HttpHeaderKeys.HDR_SET_COOKIE);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getContentLength()
     */
    @Override
    public long getContentLength() {
        if (HttpUtil.isContentLengthSet(nettyResponse)) {
            return HttpUtil.getContentLength(nettyResponse);
        } else {
            return -1;
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setContentLength(long)
     */
    @Override
    public void setContentLength(long length) {
        HttpUtil.setContentLength(nettyResponse, length);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(String name, String value) {
        this.nettyResponse.headers().set(name, value);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponseExt#setHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
     */
    @Override
    public void setHeader(HttpHeaderKeys key, String value) {
        this.nettyResponse.headers().set(key.getName(), value);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponseExt#setHeaderIfAbsent(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
     */
    @Override
    public String setHeaderIfAbsent(HttpHeaderKeys key, String value) {
        String header = null;

        if (!this.nettyResponse.headers().contains(key.getName())) {
            nettyResponse.headers().set(key.getName(), value);

        } else {
            header = nettyResponse.headers().getAsString(key.getName());
        }
        return header;

    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeHeader(java.lang.String)
     */
    @Override
    public void removeHeader(String name) {
        this.nettyResponse.headers().remove(name);
    }

    /*
     * @see io.openliberty.http.ext.HttpHeaderResponseExt#removeHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public void removeHeader(HttpHeaderKeys key) {
        this.nettyResponse.headers().remove(key.getName());
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeAllHeaders()
     */
    @Override
    public void removeAllHeaders() {
        this.nettyResponse.headers().clear();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(String name, String value) {
        this.nettyResponse.headers().add(name, value);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getBody()
     */
    @Override
    public HttpOutputStreamImpl getBody() {
        if (null == this.body) {
            if (HttpDispatcher.useEE7Streams()) {
                this.body = new HttpOutputStreamEE7(this.isc);
            } else {
                this.body = new HttpOutputStreamImpl(this.isc);
            }

        }
        return this.body;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        return this.nettyResponse.headers().getAsString(name);
    }

    /*
     * @see io.openliberty.http.ext.HttpHeaderResponseExt#getHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public String getHeader(HttpHeaderKeys key) {
        return this.nettyResponse.headers().getAsString(key.getName());
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeaders(java.lang.String)
     */
    @Override
    public List<String> getHeaders(String name) {
        return this.nettyResponse.headers().getAll(name);

    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeaders()
     */
    @Override
    public List<String> getHeaders() {
        List<String> names = new ArrayList<String>();
        names.addAll(nettyResponse.headers().names());

        return names;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeaderNames()
     */
    @Override
    public List<String> getHeaderNames() {
        List<String> names = new ArrayList<String>();
        names.addAll(nettyResponse.headers().names());

        return names;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getReason()
     */
    @Override
    public String getReason() {
        return nettyResponse.status().reasonPhrase();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getStatus()
     */
    @Override
    public int getStatus() {
        return this.nettyResponse.status().code();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getVersion()
     */
    @Override
    public String getVersion() {
        return this.nettyResponse.protocolVersion().protocolName();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setReason(java.lang.String)
     */
    @Override
    public void setReason(String phrase) {
        HttpResponseStatus status = this.nettyResponse.status();
        this.nettyResponse.setStatus(new HttpResponseStatus(status.code(), phrase));
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setStatus(int)
     */
    @Override
    public void setStatus(int code) {
        HttpResponseStatus status = new HttpResponseStatus(code, "");
        this.nettyResponse.setStatus(status);

    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setVersion(java.lang.String)
     */
    @Override
    public void setVersion(String version) {
        this.nettyResponse.setProtocolVersion(HttpVersion.valueOf(version));
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#reset()
     */
    @Override
    public void reset() {

        this.body.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpResponse#getTrailers()
     */
    @Override
    public void setTrailer(String name, String value) {

//        HttpTrailers trailers = message.createTrailers();
//        HeaderKeys key = HttpHeaderKeys.find(name, false);
//
//        if (trailers.containsDeferredTrailer(key)) {
//            trailers.removeDeferredTrailer(key);
//        }
//
//        HttpTrailerGenerator generator = new HttpTrailerGeneratorImpl(key, value);
//        trailers.setDeferredTrailer(key, generator);
//
//        return;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpResponse#writeTrailers()
     */
    @Override
    public void writeTrailers() {
//        HttpTrailers trailers = message.createTrailers();
//        trailers.computeRemainingTrailers();

    }

}