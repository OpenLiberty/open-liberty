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

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.genericbnf.internal.GenericUtils;
import com.ibm.ws.http.channel.internal.HttpChannelConfig;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.channel.internal.HttpServiceContextImpl;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundServiceContextImpl;
import com.ibm.ws.http.netty.MSP;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.inbound.HttpDispatcherHandler;
import com.ibm.wsspi.genericbnf.HeaderStorage;
import com.ibm.wsspi.genericbnf.exception.UnsupportedMethodException;
import com.ibm.wsspi.genericbnf.exception.UnsupportedSchemeException;
import com.ibm.wsspi.http.HttpCookie;
import com.ibm.wsspi.http.channel.HttpConstants;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpTrailers;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.SchemeValues;
import com.ibm.wsspi.http.channel.values.VersionValues;
import com.ibm.wsspi.http.ee8.Http2PushBuilder;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.VoidChannelPromise;
import io.netty.handler.codec.http.Cookie;
import io.netty.handler.codec.http.CookieDecoder;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;

/**
 *
 */
public class NettyRequestMessage extends NettyBaseMessage implements HttpRequestMessage {

    private static final TraceComponent tc = Tr.register(NettyRequestMessage.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private FullHttpRequest request;
    private HttpHeaders headers;
    private HttpInboundServiceContext context;

    private String url;

    private MethodValues method;
    private SchemeValues scheme;

    private QueryStringDecoder query;

    private Map<String, String[]> parameters;

    private ChannelHandlerContext nettyContext;

    /** Default URI is just a slash */
    private static final byte[] SLASH = { '/' };

    /** Request-Resource as a byte[] */
    private final byte[] myURIBytes = SLASH;

    public NettyRequestMessage(FullHttpRequest request, HttpInboundServiceContext isc, ChannelHandlerContext nettyContext) {
        init(request, isc, nettyContext);

    }

    public void init(FullHttpRequest request, HttpInboundServiceContext isc, ChannelHandlerContext nettyContext) {
        MSP.log("NettyRequestMessage request null:" + Objects.isNull(request));
        MSP.log("NettyRequestMessage isc null: " + Objects.isNull(isc));

        Objects.requireNonNull(request);
        Objects.requireNonNull(isc);

        this.context = isc;
        if (isc instanceof HttpInboundServiceContextImpl)
            incoming(((HttpInboundServiceContextImpl) isc).isInboundConnection());

        this.request = request;
        this.headers = request.headers();
        this.nettyContext = nettyContext;

        parameters = new HashMap<String, String[]>();
        processQuery();

        HttpChannelConfig config = isc instanceof HttpInboundServiceContextImpl ? ((HttpInboundServiceContextImpl) isc).getHttpConfig() : null;

        super.init(request, isc, config);
//        verifyRequest();
    }

    /**
     *
     */
    public static void verifyRequest(HttpRequestMessage message) {
        // TODO Add check for verifying request integrity
        // Method check possibly handled by Netty. Need to verify
        // Path check need to add some for ourselves
        if (!message.getMethod().equalsIgnoreCase(HttpMethod.CONNECT.toString()))
            message.setRequestURI(message.getRequestURI());
        // Need to check if Scheme is also verified by Netty coded or add that ourselves
        // Probably need to add check of authority ourselves as well
    }

    @Override
    public void clear() {
        request = null;
        headers = null;
        context = null;

        url = null;

        method = null;
        scheme = null;

        query = null;
        parameters.clear();

        super.clear();

    }

    @Override
    public void destroy() {

        super.destroy();

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
        System.out.println("Returning method: " + method.getName());
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
        MSP.log("getRequestURI: query.path()" + query.path() + "query uri: " + query.uri());
        if (getMethod().equalsIgnoreCase(HttpMethod.CONNECT.toString())) {
            System.out.println("Found connect method, returning slash");
            return GenericUtils.getEnglishString(SLASH);
        }
        return query.path();
    }

    @Override
    public byte[] getRequestURIAsByteArray() {
        // TODO Auto-generated method stub
        return GenericUtils.getBytes(getRequestURI());
    }

    @Override
    public StringBuffer getRequestURL() {

        String host = context.getLocalAddr().getCanonicalHostName();
        int port = context.getLocalPort();

        return new StringBuffer(getScheme() + "://" + host + ":" + port + "/" + getRequestURI());

    }

    @Override
    public String getRequestURLAsString() {
        if (Objects.isNull(url)) {
            url = getRequestURL().toString();
        }
        return url;
    }

    @Override
    public byte[] getRequestURLAsByteArray() {
        // TODO Auto-generated method stub
        return GenericUtils.getBytes(getRequestURLAsString());
    }

    @Override
    public String getQueryString() {

        return Objects.isNull(parameters) || parameters.isEmpty() ? null : query.rawQuery();

    }

    @Override
    public byte[] getQueryStringAsByteArray() {
        // TODO Auto-generated method stub
        return Objects.isNull(parameters) || parameters.isEmpty() ? null : GenericUtils.getBytes(getQueryString());
    }

    @Override
    public String getParameter(String name) {
        MSP.log("getParameter(name): " + name + " -> " + (parameters.containsKey(name) ? parameters.get(name)[0] : null));

        return parameters.containsKey(name) ? parameters.get(name)[0] : null;

    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        MSP.log("getParamValues name:" + name);
        return parameters.containsKey(name) ? parameters.get(name) : null;
    }

    @Override
    public void setRequestURL(String url) {
        //TODO
        setRequestURL(GenericUtils.getEnglishBytes(url));
    }

    @Override
    public void setRequestURL(byte[] url) {
        // TODO Auto-generated method stub
        System.out.println("setRequestURL Netty called but nothing was done");
    }

    @Override
    public void setRequestURI(String uri) {
        // TODO Auto-generated method stub
        setRequestURI(GenericUtils.getEnglishBytes(uri));
    }

    @Override
    public void setRequestURI(byte[] uri) {
        System.out.println("setRequestURI Netty called but limited work done");
        // TODO Auto-generated method stub
        // Just check for validity of URI
        if (null == uri || 0 == uri.length) {
            throw new IllegalArgumentException("setRequestURI: null input");
        }

        if ('*' == uri[0]) {
            // URI of "*" can only be one character long to be valid
            if (1 != uri.length && '?' != uri[1]) {
                String value = GenericUtils.getEnglishString(uri);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "setRequestURI: invalid uri [" + value + "]");
                }
                throw new IllegalArgumentException("Invalid uri: " + value);
            }
        } else if ('/' != uri[0]) {
            String value = GenericUtils.getEnglishString(uri);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "setRequestURI: invalid uri [" + value + "]");
            }
            throw new IllegalArgumentException("Invalid uri: " + value);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setRequestURI: finished parsing " + getRequestURI());
        }
    }

    @Override
    public String getURLHost() {
        return context.getLocalAddr().getHostName();
    }

    @Override
    public int getURLPort() {
        return context.getLocalPort();
    }

    @Override
    public String getVirtualHost() {

        String host = headers.get(HttpHeaderKeys.HDR_HOST.getName());
        if (Objects.nonNull(host) && host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));

        }

        return Objects.isNull(host) ? getURLHost() : host;
    }

    @Override
    public int getVirtualPort() {

        return context.getLocalPort();
    }

    @Override
    public void setQueryString(String query) {
        throw new UnsupportedOperationException("Set query delegated to http codec");

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
        this.scheme = scheme;
        //TODO: first line changed needed?
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setScheme(v): " + scheme.getName());
        }
    }

    @Override
    public void setScheme(String scheme) throws UnsupportedSchemeException {
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
    public HttpTrailers getTrailers() {
//        if (request.trailingHeaders().isEmpty())
//            return null;
        return new NettyTrailers(this.request.trailingHeaders());
    }

    @Override
    public HttpRequestMessage duplicate() {
        throw new UnsupportedOperationException("The duplicate method is not supported.");

    }

    @Override
    public boolean isPushSupported() {
        this.nettyContext.channel().pipeline().names().forEach(handler -> System.out.println(handler));
        HttpToHttp2ConnectionHandler handler = this.nettyContext.channel().pipeline().get(HttpToHttp2ConnectionHandler.class);
        if (Objects.isNull(handler)) {
            System.out.println("Could NOT find handler for push!");
            return false;
        }
        boolean canPush = handler.connection().remote().allowPushTo();
        System.out.println("Can I push remote? " + canPush);
//        canPush = handler.connection().local().allowPushTo();
//        System.out.println("Can I push local? " + canPush);
        return canPush;
    }

    @Override
    public void pushNewRequest(Http2PushBuilder pushBuilder) {

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
        this.nettyContext.channel().pipeline().names().forEach(handler -> System.out.println(handler));
        HttpToHttp2ConnectionHandler handler = this.nettyContext.channel().pipeline().get(HttpToHttp2ConnectionHandler.class);
        Http2Connection connection = handler.connection();

        int nextPromisedStreamId = connection.local().incrementAndGetNextStreamId();
        int currentStreamId = this.request.headers().getInt(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), 0);

        Http2Headers headers = new DefaultHttp2Headers().clear();
        // TODO Implement this
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
                                                                   new VoidChannelPromise(this.nettyContext.channel(), true));

        promise.addListener(future -> {
            if (future.isSuccess())
                System.out.println("Successful promise write!");
            else {
                System.out.println("No promise write: " + future.cause());
                future.cause().printStackTrace();
            }
        });

        try {
            DefaultFullHttpRequest newRequest = new DefaultFullHttpRequest(request.protocolVersion(), HttpMethod.GET, pbPath);
            System.out.println("Sending new request to dispatcher " + newRequest);
            newRequest.headers().set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), nextPromisedStreamId);
            HttpUtil.setContentLength(newRequest, 0);
            this.nettyContext.executor().execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        nettyContext.channel().pipeline().names().forEach(handler -> System.out.println(handler));
                        ((HttpDispatcherHandler) nettyContext.channel().pipeline().get(HttpPipelineInitializer.HTTP_DISPATCHER_HANDLER_NAME)).channelRead(nettyContext,
                                                                                                                                                          newRequest);;
//                        nettyContext.pipeline().get(HttpDispatcherHandler.class).channelRead(nettyContext, newRequest);
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
            InetSocketAddress local = (InetSocketAddress) this.nettyContext.channel().localAddress();
            InetSocketAddress remote = (InetSocketAddress) this.nettyContext.channel().remoteAddress();
            host = (isIncoming()) ? local.getAddress().getCanonicalHostName() : remote.getAddress().getCanonicalHostName();
        }
        return host;
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
            InetSocketAddress local = (InetSocketAddress) this.nettyContext.channel().localAddress();
            InetSocketAddress remote = (InetSocketAddress) this.nettyContext.channel().remoteAddress();
            port = (isIncoming()) ? local.getPort() : remote.getPort();
        }
        return port;
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

    private void processQuery() {
        if (Objects.isNull(query)) {
            MSP.log("Processing query with URI: " + request.uri());
            query = new QueryStringDecoder(request.uri());

            for (Map.Entry<String, List<String>> entry : query.parameters().entrySet()) {

                List<String> value = entry.getValue();
                this.parameters.put(entry.getKey(), value.toArray(new String[value.size()]));
                MSP.log("Processed parameter: " + entry.getKey() + " Value: " + value.toString());
            }

            MSP.log("Total parameters: " + parameters.size());

        }
    }

    @Override
    public List<HttpCookie> getAllCookies() {
        List<HttpCookie> list = new LinkedList<HttpCookie>();
        String cookieString = headers.get(HttpHeaders.Names.COOKIE);
        if (Objects.nonNull(cookieString)) {
            Set<Cookie> cookies = CookieDecoder.decode(cookieString);
            if (!cookies.isEmpty()) {
                for (Cookie cookie : cookies) {
                    list.add(new HttpCookie(cookie.getName(), cookie.getValue()));
                }

            }
        }

        return list;
    }
    
    @Override
    public HttpCookie getCookie(String name) {
        if (null == name) {
            return null;
        }
        HttpCookie cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE);
        if (null == cookie) {
            cookie = getCookie(name, HttpHeaderKeys.HDR_COOKIE2);
        }
        // Note: return a clone to avoid corruption by the caller
        return (null == cookie) ? null : cookie.clone();
    }

}
