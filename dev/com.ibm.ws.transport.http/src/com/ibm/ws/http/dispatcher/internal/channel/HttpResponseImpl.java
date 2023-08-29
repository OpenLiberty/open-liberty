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
import java.util.List;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.HttpTrailerGeneratorImpl;
import com.ibm.ws.http.channel.internal.outbound.HttpOutputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
import com.ibm.ws.http.netty.MSP;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.HeaderKeys;
import com.ibm.wsspi.genericbnf.exception.UnsupportedProtocolVersionException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.HttpResponse;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.HttpTrailerGenerator;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.ee7.HttpOutputStreamEE7;

import io.openliberty.http.ext.HttpResponseExt;

/**
 * Implementation of the public HTTP transport response message for the dispatcher
 * and container traffic.
 */
@Trivial
public class HttpResponseImpl implements HttpResponse, HttpResponseExt {
    private HttpInboundServiceContext isc = null;
    private HttpResponseMessage message = null;
    private HttpOutputStreamImpl body = null;
    private HttpDispatcherLink connlink = null;

    /**
     * Constructor.
     *
     * @param link
     */
    public HttpResponseImpl(HttpDispatcherLink link) {
        this.connlink = link;
    }

    /**
     * Initialize with a new wrapped message.
     *
     * @param context
     */
    public void init(HttpInboundServiceContext context) {
        this.isc = context;
        this.message = context.getResponse();
        this.body = null;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#isCommitted()
     */
    @Override
    public boolean isCommitted() {
        return this.message.isCommitted();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#addCookie(com.ibm.websphere.http.Cookie)
     */
    @Override
    public void addCookie(HttpCookie cookie) {
        this.message.setCookie(cookie, HttpHeaderKeys.HDR_SET_COOKIE);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {
        return this.message.getCookie(name);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getCookies(String name) {
        return this.message.getAllCookies(name);
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
        return this.message.getAllCookies();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeCookie(com.ibm.websphere.http.HttpCookie)
     */
    @Override
    public void removeCookie(HttpCookie cookie) {
        this.message.removeCookie(cookie.getName(), HttpHeaderKeys.HDR_SET_COOKIE);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getContentLength()
     */
    @Override
    public long getContentLength() {
        return this.message.getContentLength();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setContentLength(long)
     */
    @Override
    public void setContentLength(long length) {
        MSP.log("Content Length Test - " + length);
        this.message.setContentLength(length);
        if (this.body != null) {
            this.body.setContentLength(length);
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void setHeader(String name, String value) {
        this.message.setHeader(name, value);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponseExt#setHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
     */
    @Override
    public void setHeader(HttpHeaderKeys key, String value) {
        this.message.setHeader(key, value);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponseExt#setHeaderIfAbsent(com.ibm.wsspi.http.channel.values.HttpHeaderKeys, java.lang.String)
     */
    @Override
    public String setHeaderIfAbsent(HttpHeaderKeys key, String value) {
        HeaderField oldValue = this.message.setHeaderIfAbsent(key, value);
        return oldValue == null ? null : oldValue.asString();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeHeader(java.lang.String)
     */
    @Override
    public void removeHeader(String name) {
        this.message.removeHeader(name);
    }

    /*
     * @see io.openliberty.http.ext.HttpHeaderResponseExt#removeHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public void removeHeader(HttpHeaderKeys key) {
        this.message.removeHeader(key);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#removeAllHeaders()
     */
    @Override
    public void removeAllHeaders() {
        this.message.removeAllHeaders();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#addHeader(java.lang.String, java.lang.String)
     */
    @Override
    public void addHeader(String name, String value) {
        this.message.appendHeader(name, value);
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
            this.body.setVirtualConnection(this.connlink.getVirtualConnection());
            if (this.message != null) {
                long messageContentLength = getContentLength();
                if (messageContentLength != -1) { //it's set
                    this.body.setContentLength(getContentLength());
                }
            }
        }
        return this.body;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        return this.message.getHeader(name).asString();
    }

    /*
     * @see io.openliberty.http.ext.HttpHeaderResponseExt#getHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */
    @Override
    public String getHeader(HttpHeaderKeys key) {
        return this.message.getHeader(key).asString();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeaders(java.lang.String)
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
     * @see com.ibm.websphere.http.HttpResponse#getHeaders()
     */
    @Override
    public List<String> getHeaders() {
        List<HeaderField> hdrs = this.message.getAllHeaders();
        List<String> values = new ArrayList<String>(hdrs.size());
        for (HeaderField header : hdrs) {
            values.add(header.getName());
        }
        return values;
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getHeaderNames()
     */
    @Override
    public List<String> getHeaderNames() {
        return this.message.getAllHeaderNames();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getReason()
     */
    @Override
    public String getReason() {
        return this.message.getReasonPhrase();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getStatus()
     */
    @Override
    public int getStatus() {
        return this.message.getStatusCodeAsInt();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#getVersion()
     */
    @Override
    public String getVersion() {
        return this.message.getVersion();
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setReason(java.lang.String)
     */
    @Override
    public void setReason(String phrase) {
        this.message.setReasonPhrase(phrase);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setStatus(int)
     */
    @Override
    public void setStatus(int code) {
        this.message.setStatusCode(code);
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#setVersion(java.lang.String)
     */
    @Override
    public void setVersion(String version) {
        try {
            this.message.setVersion(version);
        } catch (UnsupportedProtocolVersionException e) {
            this.message.setVersion(VersionValues.V11);
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpResponse#reset()
     */
    @Override
    public void reset() {
        this.message.clear();
        this.body.clear();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpResponse#getTrailers()
     */
    @Override
    public void setTrailer(String name, String value) {

        HttpTrailers trailers = message.createTrailers();
        HeaderKeys key = HttpHeaderKeys.find(name, false);

        if (trailers.containsDeferredTrailer(key)) {
            trailers.removeDeferredTrailer(key);
        }

        HttpTrailerGenerator generator = new HttpTrailerGeneratorImpl(key, value);
        trailers.setDeferredTrailer(key, generator);

        return;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpResponse#writeTrailers()
     */
    @Override
    public void writeTrailers() {
        HttpTrailers trailers = message.createTrailers();
        trailers.computeRemainingTrailers();

    }

}
