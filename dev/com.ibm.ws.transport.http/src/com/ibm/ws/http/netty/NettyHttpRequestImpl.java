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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.channel.HttpRequestImpl;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;
import com.ibm.wsspi.http.ee8.Http2Request;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.openliberty.http.ext.HttpRequestExt;

/**
 * Implementation of an HTTP request message provided by the HTTP dispatcher to
 * various containers.
 */
@Trivial
public class NettyHttpRequestImpl extends HttpRequestImpl implements Http2Request, HttpRequestExt {

    private HttpInputStreamImpl body = null;
    private boolean useEE7Streams = false;
    private FullHttpRequest nettyRequest = null;
    private Channel nettyChannel = null;
    private QueryStringDecoder nettyDecoder = null;
    private Set<Cookie> nettyCookies;

    /**
     * Constructor.
     */
    public NettyHttpRequestImpl() {
        // nothing
    }

    /**
     * Constructor.
     */
    public NettyHttpRequestImpl(boolean useEE7Streams) {
        this.useEE7Streams = useEE7Streams;
    }

    /**
     * Initialize with a new connection.
     *
     * @param context
     */
    public void init(FullHttpRequest request, Channel channel, HttpInboundServiceContext context) {
        this.nettyRequest = request;
        this.nettyChannel = channel;
        this.nettyDecoder = new QueryStringDecoder(nettyRequest.uri());

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
        return HttpUtil.getContentLength(nettyRequest);
    }

    private void initNettyCookies() {
        if (nettyCookies == null) {
            String value = nettyRequest.headers().get(HttpHeaderNames.COOKIE);
            if (value != null) {
                nettyCookies = ServerCookieDecoder.STRICT.decode(value);
            } else {
                nettyCookies = Collections.emptySet();

            }
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {
        initNettyCookies();
        for (Cookie nettyCookie : nettyCookies) {
            if (nettyCookie.name().equalsIgnoreCase(name)) {
                return new HttpCookie(nettyCookie.name(), nettyCookie.value());
            }
        }
        return null;

    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getCookies(String name) {
        initNettyCookies();
        if (!nettyCookies.isEmpty()) {
            List<HttpCookie> libertyCookies = new ArrayList<HttpCookie>();
            for (Cookie nettyCookie : nettyCookies) {
                if (nettyCookie.name().equalsIgnoreCase(name)) {
                    HttpCookie libertyCookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());
                    libertyCookies.add(libertyCookie);
                }
            }
            return libertyCookies;
        } else {
            return Collections.emptyList();
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookies()
     */
    @Override
    public List<HttpCookie> getCookies() {
        initNettyCookies();
        if (!nettyCookies.isEmpty()) {
            List<HttpCookie> libertyCookies = new ArrayList<HttpCookie>(nettyCookies.size());
            for (Cookie nettyCookie : nettyCookies) {
                HttpCookie libertyCookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());
                libertyCookies.add(libertyCookie);
            }
            return libertyCookies;

        } else {
            return Collections.emptyList();
        }
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {
        return this.nettyRequest.headers().get(name);
    }

    /*
     * @see com.ibm.websphere.http.HttpRequestExt#getHeader(com.ibm.wsspi.http.channel.values.HttpHeaderKeys)
     */

    @Override
    public String getHeader(HttpHeaderKeys key) {

        return this.nettyRequest.headers().get(key.getName());
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeaders(java.lang.String)
     */
    @Override
    public List<String> getHeaders(String name) {

        return this.nettyRequest.headers().getAll(name);
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeaderNames()
     */
    @Override
    public List<String> getHeaderNames() {
        return new ArrayList<String>(this.nettyRequest.headers().names());
    }

    /*
     * @see com.ibm.websphere.http.HttpRequestExt#getHeaderNamesSet()
     */
    @Override
    public Set<String> getHeaderNamesSet() {
        //TODO: needed?
        return null;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getMethod()
     */
    @Override
    public String getMethod() {
        return nettyRequest.method().name();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getQuery()
     */
    @Override
    @Trivial
    public String getQuery() {
        return this.nettyDecoder.rawQuery();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getScheme()
     */
    @Override
    public String getScheme() {
        //TODO: pull isSecure from pipeline
        return "http";
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURI()
     */
    @Override
    public String getURI() {
        return this.nettyDecoder.path();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURL()
     */
    @Override
    public String getURL() {
        String host = ((InetSocketAddress) this.nettyChannel.remoteAddress()).getHostString();
        String port = Integer.toString(((InetSocketAddress) this.nettyChannel.localAddress()).getPort());

        return getScheme() + "://" + host + ":" + port + "/" + getURI();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVersion()
     */
    @Override
    public String getVersion() {
        return this.nettyRequest.protocolVersion().text();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualHost()
     */
    @Override
    public String getVirtualHost() {
        String host = ((InetSocketAddress) this.nettyChannel.localAddress()).getHostString();
        if (host == null) {
            host = this.nettyRequest.headers().get(HttpHeaderKeys.HDR_HOST.getName());
            if (host != null && host.contains(":")) {
                host = host.substring(0, host.indexOf(':'));
            }
        }
        return host;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualPort()
     */
    @Override
    public int getVirtualPort() {
        String port = null;
        int portNum = ((InetSocketAddress) this.nettyChannel.localAddress()).getPort();
        if (port == null) {
            port = this.nettyRequest.headers().get(HttpHeaderKeys.HDR_HOST.getName());
            if (port != null & port.contains(":")) {
                port = port.substring(port.indexOf(':'));
                try {
                    portNum = Integer.parseInt(port);
                } catch (NumberFormatException exception) {
                    portNum = -1;
                }
            }
        }

        return portNum;

    }

    @Override
    @Trivial
    public String toString() {
        return this.getClass().getSimpleName() + "[message=" + this.nettyRequest + "]";
    }

    /**
     * Initiate a Push request
     *
     * @return
     */
    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {
        //TODO: H2
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#getTrailers()
     */
    @Override
    public List<String> getTrailerNames() {
        HttpHeaders trailers = this.nettyRequest.trailingHeaders();
        if (trailers != null)
            return new ArrayList<String>(trailers.names());
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
        HttpHeaders trailers = this.nettyRequest.trailingHeaders();
        if (trailers != null)
            return trailers.get(name);
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
//        if (!message.isChunkedEncodingSet()
//            || !message.containsHeader(HttpHeaderKeys.HDR_TRAILER)
//            || ((HttpBaseMessageImpl) message).getTrailersImpl() != null
//            || (message.getVersionValue().getMajor() <= 1 && message.getVersionValue().getMinor() < 1))
//            return true;

        if (!this.nettyRequest.headers().contains(HttpHeaderKeys.HDR_TRAILER.getName()) ||
            this.nettyRequest.trailingHeaders() != null ||
            (this.nettyRequest.protocolVersion().majorVersion() < 2 && this.nettyRequest.protocolVersion().minorVersion() < 1)) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#isPushSupported()
     */
    @Override
    public boolean isPushSupported() {
        // TODO is H2 enabled?
        return false;
    }
}