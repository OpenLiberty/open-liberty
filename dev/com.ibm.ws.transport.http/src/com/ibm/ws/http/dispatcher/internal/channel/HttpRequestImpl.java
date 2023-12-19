/*******************************************************************************
 * Copyright (c) 2009, 2023 IBM Corporation and others.
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
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.HttpBaseMessageImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;
import com.ibm.wsspi.http.ee8.Http2Request;

import io.netty.handler.codec.http.FullHttpRequest;
import io.openliberty.http.ext.HttpRequestExt;

/**
 * Implementation of an HTTP request message provided by the HTTP dispatcher to
 * various containers.
 */
@Trivial
public class HttpRequestImpl implements Http2Request, HttpRequestExt {
    private HttpRequestMessage message = null;
    private HttpInputStreamImpl body = null;
    private boolean useEE7Streams = false;

    /**
     * Constructor.
     */
    public HttpRequestImpl() {
        // nothing
    }

    /**
     * Constructor.
     */
    public HttpRequestImpl(boolean useEE7Streams) {
        this.useEE7Streams = useEE7Streams;
    }

    /**
     * Initialize with a new connection.
     *
     * @param context
     */
    public void init(HttpInboundServiceContext context) {
        this.message = context.getRequest();
        if (this.useEE7Streams) {
            this.body = new HttpInputStreamEE7(context);
        } else {
            this.body = new HttpInputStreamImpl(context);
        }
    }

    /**
     * Initialize with a new connection.
     *
     * @param context
     */
    public void init(FullHttpRequest request, HttpInboundServiceContext context) {

        this.message = context.getRequest();

        if (this.useEE7Streams) {
            this.body = new HttpInputStreamEE7(context, request);
        } else {
            this.body = new HttpInputStreamImpl(context, request);
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getBody()
     */
    @Override
    public HttpInputStreamImpl getBody() {
        return this.body;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getContentLength()
     */
    @Override
    public long getContentLength() {
        return this.message.getContentLength();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {
        return this.message.getCookie(name);
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getCookies(String name) {
        return this.message.getAllCookies(name);
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookies()
     */
    @Override
    public List<HttpCookie> getCookies() {
        return this.message.getAllCookies();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        return this.message.getHeader(name).asString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequestExt#getHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */

    @Override
    public String getHeader(HttpHeaderKeys key) {
        return this.message.getHeader(key).asString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeaders(java.lang.String)
     */
    @Override
    public List<String> getHeaders(String name) {
        List<HeaderField> hdrs = this.message.getHeaders(name);
        int size = hdrs.size();
        List<String> values;
        if (size == 0) {
            values = Collections.emptyList();
        } else if (size == 1) {
            values = Collections.singletonList(hdrs.get(0).asString());
        } else {
            values = new ArrayList<String>(size);
            for (HeaderField header : hdrs) {
                values.add(header.asString());
            }
        }
        return values;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeaderNames()
     */
    @Override
    public List<String> getHeaderNames() {
        return this.message.getAllHeaderNames();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequestExt#getHeaderNamesSet()
     */
    @Override
    public Set<String> getHeaderNamesSet() {
        return this.message.getAllHeaderNamesSet();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getMethod()
     */
    @Override
    public String getMethod() {
        MSP.log("method: " + this.message.getMethod());
        return this.message.getMethod();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getQuery()
     */
    @Override
    @Trivial
    public String getQuery() {
        MSP.log("query: " + this.message.getQueryString());
        return this.message.getQueryString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getScheme()
     */
    @Override
    public String getScheme() {
        MSP.log("scheme: " + message.getScheme());
        return this.message.getScheme();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURI()
     */
    @Override
    public String getURI() {
        MSP.log("getURI: " + this.message.getRequestURI());
        return this.message.getRequestURI();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURL()
     */
    @Override
    public String getURL() {
        MSP.log("getURL: " + message.getRequestURL().toString());
        return this.message.getRequestURL().toString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVersion()
     */
    @Override
    public String getVersion() {
        MSP.log("getVersion: " + this.message.getVersion());
        return this.message.getVersion();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualHost()
     */
    @Override
    public String getVirtualHost() {
        MSP.log("getVirtualHost: " + message.getVirtualHost());
        return this.message.getVirtualHost();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualPort()
     */
    @Override
    public int getVirtualPort() {
        MSP.log("getVirtualPort: " + message.getVirtualPort());
        return this.message.getVirtualPort();
    }

    @Override
    @Trivial
    public String toString() {
        return this.getClass().getSimpleName() + "[message=" + message + "]";
    }

    /**
     * Initiate a Push request
     *
     * @return
     */
    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {
        this.message.pushNewRequest(pushBuilder);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#getTrailers()
     */
    @Override
    public List<String> getTrailerNames() {
        HttpTrailers trailers = message.getTrailers();
        if (trailers != null)
            return trailers.getAllHeaderNames();
        else
            return null;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#getTrailer(java.lang.String)
     */
    @Override
    public String getTrailer(String name) {
        HttpTrailers trailers = message.getTrailers();
        if (trailers != null)
            return trailers.getHeader(name).asString();
        else
            return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#isTrailersReady()
     */
    @Override
    public boolean isTrailersReady() {
        boolean trailersNull;
        if (message instanceof HttpBaseMessageImpl) {
            trailersNull = ((HttpBaseMessageImpl) message).getTrailersImpl() != null;
        } else
            trailersNull = message.getTrailers() != null;

        if (!message.isChunkedEncodingSet()
            || !message.containsHeader(HttpHeaderKeys.HDR_TRAILER)
            || trailersNull
            || (message.getVersionValue().getMajor() <= 1 && message.getVersionValue().getMinor() < 1))
            return true;
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#isPushSupported()
     */
    @Override
    public boolean isPushSupported() {
        // TODO Auto-generated method stub
        return message.isPushSupported();
    }
}