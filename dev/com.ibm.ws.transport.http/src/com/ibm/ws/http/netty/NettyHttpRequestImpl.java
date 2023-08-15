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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.inbound.HttpInputStreamImpl;
import com.ibm.ws.http.dispatcher.internal.channel.HttpRequestImpl;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.ee7.HttpInputStreamEE7;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;
import com.ibm.wsspi.http.ee8.Http2Request;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.VoidChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.openliberty.http.ext.HttpRequestExt;

/**
 * Implementation of an HTTP request message provided by the HTTP dispatcher to
 * various containers.
 */
@Trivial
public class NettyHttpRequestImpl extends HttpRequestImpl implements Http2Request, HttpRequestExt {

    private static final TraceComponent tc = Tr.register(NettyHttpRequestImpl.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private HttpInputStreamImpl body = null;
    private boolean useEE7Streams = false;
    private FullHttpRequest nettyRequest = null;
    private Channel nettyChannel = null;
    private QueryStringDecoder nettyDecoder = null;
    private Set<Cookie> nettyCookies;

    private ChannelHandlerContext nettyContext;

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
    public void init(FullHttpRequest request, ChannelHandlerContext ctx, HttpInboundServiceContext context) {
        this.nettyRequest = request;
        this.nettyContext = ctx;
        this.nettyChannel = ctx.channel();
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
        return this.nettyRequest.headers().names();
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
            if (port != null && port.contains(":")) {
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
        System.out.println("Hit the pushNewRequest!!!");
        // path is equal to uri + queryString
        String pbPath = null;
        if (pushBuilder.getPathQueryString() != null) {
            pbPath = pushBuilder.getURI() + pushBuilder.getPathQueryString();
        } else {
            pbPath = pushBuilder.getURI();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "HTTPRequestMessageImpl pbPath = " + pbPath);
        }
        HttpToHttp2ConnectionHandler handler = this.nettyChannel.pipeline().get(HttpToHttp2ConnectionHandler.class);
        Http2Connection connection = handler.connection();

        int nextPromisedStreamId = connection.local().incrementAndGetNextStreamId();
        int currentStreamId = nettyRequest.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 0);

        Http2Headers headers = new DefaultHttp2Headers().clear();
//        String scheme = new String("https");
//        if (!this.isSecure()) {
//            scheme = new String("http");
//        }
        String scheme = new String("http");
        headers.method(pushBuilder.getMethod()).scheme(scheme).path(pbPath);

        // Encode authority
        // If the :authority header was sent in the request, get the information from there
        // If it was not, use getTargetHost and and getTargetPort to create it
        // If it's still null, we have to bail, since it's a required header in a push_promise frame
        String auth = getTargetHost();
        if (null != auth) {
            if (0 <= getTargetPort()) {
                auth = auth + ":" + Integer.toString(getTargetPort());
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(tc, "HTTPRequestMessageImpl: Cannot find hostname for required :authority pseudo header");
            }
            return;
        }
        headers.authority(auth);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.debug(tc, "handleH2LinkPreload(): Method is GET, authority is " + auth + ", scheme is " + scheme);
        }

        System.out.println("Sending push promise frame for currentStream " + currentStreamId + " on promisedStream " + nextPromisedStreamId + " with headers " + headers);

        ChannelFuture promise = handler.encoder().writePushPromise(nettyContext, currentStreamId, nextPromisedStreamId, headers, 0,
                                                                   new VoidChannelPromise(this.nettyChannel, true));

        promise.addListener(future -> {
            if (future.isSuccess())
                System.out.println("Successful promise write!");
            else {
                System.out.println("No promise write: " + future.cause());
                future.cause().printStackTrace();
            }
        });

        try {
            DefaultFullHttpRequest newRequest = new DefaultFullHttpRequest(nettyRequest.protocolVersion(), HttpMethod.GET, pbPath);
            System.out.println("Sending new request to dispatcher " + newRequest);
            newRequest.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), nextPromisedStreamId);
            HttpUtil.setContentLength(newRequest, 0);
            this.nettyContext.executor().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        nettyContext.pipeline().get(HttpDispatcherHandler.class).channelRead(nettyContext, newRequest);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
                        System.out.println("Error fowarding push request");
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            // TODO Auto-generated catch block
            // Do you need FFDC here? Remember FFDC instrumentation and @FFDCIgnore
            e.printStackTrace();
        }
    }

    /**
     * Find the target host of the request. This checks the VirtualHost data but
     * falls back on the socket layer target if need be.
     *
     * @return String
     */
    private String getTargetHost() {
        String host = getVirtualHost();
        if (null == host) {
            InetSocketAddress local = (InetSocketAddress) this.nettyChannel.localAddress();
            InetSocketAddress remote = (InetSocketAddress) this.nettyChannel.remoteAddress();
            host = (isIncoming()) ? local.getAddress().getCanonicalHostName() : remote.getAddress().getCanonicalHostName();
        }
        return host;
    }

    /**
     * @return
     */
    private boolean isIncoming() {
        // TODO Complete this method properly
        return true;
    }

    /**
     * Find the target port of the request. This checks the VirtualPort data and
     * falls back on the socket port information if need be.
     *
     * @return int
     */
    private int getTargetPort() {
        int port = getVirtualPort();
        if (HeaderStorage.NOTSET == port) {
            InetSocketAddress local = (InetSocketAddress) this.nettyChannel.localAddress();
            InetSocketAddress remote = (InetSocketAddress) this.nettyChannel.remoteAddress();
            port = (isIncoming()) ? local.getPort() : remote.getPort();
        }
        return port;
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
        System.out.println("Checking if push is supported!!!");
        boolean canPush = this.nettyChannel.pipeline().get(HttpToHttp2ConnectionHandler.class).connection().remote().allowPushTo();
        System.out.println("Can I push? " + canPush);
        return canPush;
    }
}