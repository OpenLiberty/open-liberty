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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.HttpBaseMessageImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;
import com.ibm.wsspi.http.ee8.Http2Request;

import io.openliberty.http.ext.HttpRequestExt;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;

/**
 * Implementation of an HTTP request message provided by the HTTP dispatcher to
 * various containers.
 */
@Trivial
public class HttpRequestImpl implements Http2Request, HttpRequestExt {
    private HttpRequestMessage message = null;
    private HttpInputStreamImpl body = null;
    private boolean useEE7Streams = false;

    private FullHttpRequest nettyRequest = null;
    private Channel nettyChannel = null;
    private QueryStringDecoder nettyDecoder = null;
    private Set<io.netty.handler.codec.http.cookie.Cookie> nettyCookies;

    private boolean usingNetty = false;

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
     * Initialize with a new netty based connection
     */
    public void init(FullHttpRequest request, Channel channel, HttpInboundServiceContext context) {
        this.nettyRequest = request;
        this.nettyChannel = channel;
        this.nettyDecoder = new QueryStringDecoder(nettyRequest.uri());
        this.usingNetty = true;
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
        int contentLength = -1;
        if (usingNetty) {
            HttpUtil.getContentLength(nettyRequest);
        } else {
            contentLength = this.message.getContentLength();
        }
        return contentLength;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookie(java.lang.String)
     */
    @Override
    public HttpCookie getCookie(String name) {
        HttpCookie cookie = null;
        if (usingNetty) {
            initNettyCookies();
            for (io.netty.handler.codec.http.cookie.Cookie nettyCookie : nettyCookies) {
                if (nettyCookie.name().equalsIgnoreCase(name)) {
                    cookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());
                    break;
                }
            }
        } else {
            cookie = this.message.getCookie(name);
        }
        return cookie;

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
     * @see com.ibm.websphere.http.HttpRequest#getCookies(java.lang.String)
     */
    @Override
    public List<HttpCookie> getCookies(String name) {
        List<HttpCookie> cookies = Collections.emptyList();
        HttpCookie HttpCookie;

        if(usingNetty) {
            initNettyCookies();
            (!nettyCookies.isEmpty()){
                cookies = new ArrayList<HttpCookie>();
                for(io.netty.handler.codec.http.cookie.Cookie cookie: nettyCookies) {
                    httpCookie = new HttpCookie(cookie.name(), cookie.value());
                    cookies.add(httpCookie);
                }
            }
        }else {
            cookies = this.message.getAllCookies(name);
        }
        return cookies;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getCookies()
     */
    @Override
    public List<HttpCookie> getCookies() {
        List<HttpCookie> cookies = Collections.emptyList();
        HttpCookie httpCookie;

        if (usingNetty) {
            initNettyCookies();
            if (!nettyCookies.isEmpty()) {
                cookies = new ArrayList<HttpCookie>(nettyCookies.size());
                for (io.netty.handler.codec.http.cookie.Cookie cookie : nettyCookies) {
                    httpCookie = new HttpCookie(nettyCookie.name(), nettyCookie.value());
                    cookies.add(cookie);
                }
            }
        } else {
            cookies = this.message.getAllCookies();
        }

        return cookies;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getHeader(java.lang.String)
     */
    @Override
    public String getHeader(String name) {

        return this.usingNetty ? this.nettyRequest.headers().get(name) : this.message.getHeader(name).asString();
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
        return usingNetty ? new ArrayList<String>(this.nettyRequest.headers().names()) : this.message.getAllHeaderNames();
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
        return usingNetty ? nettyRequest.method().name() : this.message.getMethod();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getQuery()
     */
    @Override
    @Trivial
    public String getQuery() {
        return usingNetty ? this.nettyDecoder.rawQuery : this.message.getQueryString();
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
        return usingNetty ? this.nettyDecoder.path() : this.message.getRequestURI();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getURL()
     */
    @Override
    public String getURL() {
        String url;
        if (usingNetty) {
            String host = ((InetSocketAddress) this.nettyChannel.remoteAddress()).getHostString();
            String port = Integer.toString(((InetSocketAddress) this.nettyChannel.localAddress()).getPort());

            url = this.getScheme() + "://" + host + ":" + port + "/" + getURI();
        } else {
            url = this.message.getRequestURL().toString();
        }

        return url;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVersion()
     */
    @Override
    public String getVersion() {
        return usingNetty ? nettyRequest.protocolVersion().text() : this.message.getVersion();
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualHost()
     */
    @Override
    public String getVirtualHost() {
        String host;

        if (usingNetty) {
            host = ((InetSocketAddress) this.nettyChannel.localAddress()).getHostString();
            if (host == null) {
                host = this.nettyRequest.headers().get("host");
                if (host != null && host.contains(":")) {
                    host = host.substring(0, host.indexOf(':'));
                }
            }
        } else {
            host = this.message.getVirtualHost();
        }

        return host;
    }

    /*
     * @see com.ibm.websphere.http.HttpRequest#getVirtualPort()
     */
    @Override
    public int getVirtualPort() {
        int port = -1;

        if (usingNetty) {
            port = ((InetSocketAddress) this.nettyChannel.localAddress()).getPort();
        } else {
            this.message.getVirtualPort();
        }
        return port;
    }

    @Override
    @Trivial
    public String toString() {

        return usingNetty ? this.getClass().getSimpleName() + "[message=" + this.nettyRequest + "]" : this.getClass().getSimpleName() + "[message=" + message + "]";

    }

    /**
     * Initiate a Push request
     *
     * @return
     */
    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {
        if (usingNetty) {
            //TODO: H2 Push
        } else {
            this.message.pushNewRequest(pushBuilder);
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#getTrailers()
     */
    @Override
    public List<String> getTrailerNames() {
        List<String> listOfTrailers;

        if (usingNetty) {
            HttpHeaders trailerHeaders = this.nettyRequest.trailingHeaders();
            if (trailerHeaders != null) {
                listOfTrailers = new ArrayList<String>(trailerHeaders.names());
            }

        } else {
            HttpTrailers trailers = message.getTrailers();
            if (trailers != null)
                trailers.getAllHeaderNames();
        }

        return listOfTrailers;

    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#getTrailer(java.lang.String)
     */
    @Override
    public String getTrailer(String name) {
        String trailer;

        if (usingNetty) {
            HttpHeaders trailers = this.nettyRequest.trailingHeaders();
            trailer = trailers.get(name);

        } else {
            HttpTrailers trailers = message.getTrailers();
            if (trailers != null)
                trailer = trailers.getHeader(name).asString();
        }

        return trailer;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#isTrailersReady()
     */
    @Override
    public boolean isTrailersReady() {
        boolean trailersReady;
        if(usingNetty) {
            if(!this.nettyRequest.headers().contains("Trailer")) || this.nettyRequest.trailingHeaders() != null ||
                            (this.nettyRequest.protocolVersion().majorVersion < 2 && this.nettyRequest.protocolVersion().minorVersion() <1)){
                                trailersReady = true;
                            }
        }else {
        if (!message.isChunkedEncodingSet()
            || !message.containsHeader(HttpHeaderKeys.HDR_TRAILER)
            || ((HttpBaseMessageImpl) message).getTrailersImpl() != null
            || (message.getVersionValue().getMajor() <= 1 && message.getVersionValue().getMinor() < 1))
           trailersReady= true;
        }

        return trailersReady;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.wsspi.http.HttpRequest#isPushSupported()
     */
    @Override
    public boolean isPushSupported() {
        // TODO Add HTTP/2 support for Netty
        return usingNetty ? false : message.isPushSupported();
    }
}
