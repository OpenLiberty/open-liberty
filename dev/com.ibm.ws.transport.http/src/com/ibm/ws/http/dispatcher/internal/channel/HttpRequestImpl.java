/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.dispatcher.internal.channel;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;
import com.ibm.wsspi.http.ee8.Http2PushException;
import com.ibm.wsspi.http.ee8.Http2Request;

/**
 * Implementation of an HTTP request message provided by the HTTP dispatcher to
 * various containers.
 */
@Trivial
public class HttpRequestImpl implements Http2Request {
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
     * @see com.ibm.websphere.http.HttpRequest#getHeaders(java.lang.String)
     */
    @Override
    public List<String> getHeaders(String name) {
        List<HeaderField> hdrs = this.message.getHeaders(name);
        List<String> values = new ArrayList<String>(hdrs.size());
        for (HeaderField header : hdrs) {
            values.add(header.asString());
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
     * @see com.ibm.websphere.http.HttpRequest#getMethod()
     */
    @Override
    public String getMethod() {
        return this.message.getMethod();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getQuery()
     */
    @Override
    @Trivial
    public String getQuery() {
        return this.message.getQueryString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getScheme()
     */
    @Override
    public String getScheme() {
        return this.message.getScheme();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURI()
     */
    @Override
    public String getURI() {
        return this.message.getRequestURI();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURL()
     */
    @Override
    public String getURL() {
        return this.message.getRequestURL().toString();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVersion()
     */
    @Override
    public String getVersion() {
        return this.message.getVersion();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualHost()
     */
    @Override
    public String getVirtualHost() {
        return this.message.getVirtualHost();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualPort()
     */
    @Override
    public int getVirtualPort() {
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
    public void pushNewRequest(Http2PushBuilder pushBuilder) throws Http2PushException {

    }

}
