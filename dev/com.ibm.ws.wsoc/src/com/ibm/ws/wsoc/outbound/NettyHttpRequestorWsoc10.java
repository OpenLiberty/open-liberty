/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.wsoc.outbound;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLEngine;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.netty.NettyHttpConstants;
import com.ibm.ws.http.netty.inbound.NettyTCPConnectionContext;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.HandshakeProcessor;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.external.HandshakeResponseExt;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionLink;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.StatusCodes;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.openliberty.http.options.HttpOption;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;

/**
 *
 */
public class NettyHttpRequestorWsoc10 implements HttpRequestor {

    private static final TraceComponent tc = Tr.register(NettyHttpRequestorWsoc10.class);

    protected ClientTransportAccess access = null;

    protected WsocAddress endpointAddress = null;

    protected Channel connection;
    protected BootstrapExtended factory;

    private String websocketKey = "";

    private final Map<String, List<String>> requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, List<String>> responseHeaders = null;

    protected final ClientEndpointConfig config;

    private ChannelPromise responsePromise;
    
    private CountDownLatch activeChannelLatch;

    private FullHttpResponse resp;

    private final ParametersOfInterest things;

    private final Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();

    protected Map<String, Object> httpOptions;

    public NettyHttpRequestorWsoc10(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        this.endpointAddress = endpointAddress;
        this.config = config;
        this.things = things;
        httpOptions = WsocOutboundChain.getCurrentHttpOptions();
    }

    @Override
    public ClientTransportAccess getClientTransportAccess() {
        return access;
    }

    @Override
    public void connect() throws Exception {
        // Doesn't actually connect, we just initialize the bootstrap
        access = new ClientTransportAccess();
        factory = WsocOutboundChain.getBootstrap(endpointAddress);
        startConnection();
    }

    private void startConnection() throws InterruptedException, ExecutionException, NettyException, TimeoutException {
        InetSocketAddress remoteAddress = endpointAddress.getRemoteAddress();
        String host = remoteAddress.getHostString();
        int port = remoteAddress.getPort();
        factory.handler(new WsocClientInitializer(factory.getBaseInitializer(), this));
        activeChannelLatch = new CountDownLatch(1);
        final AtomicBoolean connectSucceded = new AtomicBoolean(true);
        connection = WsocOutboundChain.getNettyFramework().startOutbound(factory, host, port, future -> {
            if (!future.isSuccess()) {
                connectSucceded.set(false);
            }
            activeChannelLatch.countDown();
        });
        responsePromise = connection.newPromise();
        activeChannelLatch.await(HttpOption.READ_TIMEOUT.parse(httpOptions), TimeUnit.SECONDS);
        if(!connectSucceded.get()) {
            throw new NettyException("Unable to connect to the specified endpoint!");
        }
    }

    @Override
    public void sendRequest() throws IOException, MessageSentException {
        sendRequest(null);
    }

    @Override
    public void sendRequest(ParametersOfInterest poi) throws IOException, MessageSentException {
        access.setTCPConnectionContext(new NettyTCPConnectionContext(connection, null));
        access.setDeviceConnLink(new NettyOutboundConnectionLink(connection));

        String uriPath = endpointAddress.getURI().getPath();
        String queryString = endpointAddress.getURI().getQuery();
        String finalUri = uriPath + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, finalUri);
        HttpUtil.setContentLength(request, 0);
        request.headers().set(HttpHeaderKeys.HDR_HOST.getName(), endpointAddress.getURI().getHost() + ":" + endpointAddress.getURI().getPort());

        HttpHeaders nettyRequestHeaders = request.headers();

        //   We put request headers in Map for possible modification by configurator beforeRequest, and also used by Session
        requestHeaders.put(HttpHeaderKeys.HDR_CONNECTION.getName(), Arrays.asList(Constants.HEADER_VALUE_UPGRADE));
        requestHeaders.put(HttpHeaderKeys.HDR_UPGRADE.getName(), Arrays.asList(Constants.HEADER_VALUE_WEBSOCKET));
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, Arrays.asList(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION));

        websocketKey = WebSocketContainerManager.getRef().generateWebsocketKey();
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_KEY, Arrays.asList(websocketKey));

        if (config != null) {
            List<String> subprotocols = config.getPreferredSubprotocols();
            if (subprotocols != null) {
                if (subprotocols.size() > 0) {
                    String subprotocolValue = "";

                    for (int x = 0; x < subprotocols.size(); x++) {
                        if (x == 0) {
                            subprotocolValue = subprotocols.get(0).trim();
                        } else {
                            subprotocolValue = subprotocolValue + "," + subprotocols.get(x).trim();
                        }
                    }
                    requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL, Arrays.asList(subprotocolValue));
                }
            }

            List<Extension> extensions = config.getExtensions();

            if (extensions != null) {
                if (extensions.size() > 0) {
                    StringBuffer buf = new StringBuffer();
                    boolean first = true;
                    for (Extension ext : extensions) {
                        if (first) {
                            first = false;
                        } else {
                            buf.append(", ");
                        }
                        buf.append(ext.getName());
                        List<Parameter> li = ext.getParameters();
                        if (li != null) {
                            if (li.size() > 0) {
                                for (Parameter p : li) {
                                    buf.append("; " + p.getName() + "=" + p.getValue());
                                }
                            }
                        }
                    }
                    requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS, Arrays.asList(buf.toString()));
                }
            }

            if (config.getConfigurator() != null) {
                config.getConfigurator().beforeRequest(requestHeaders);
            }
        }
        Iterator<Entry<String, List<String>>> i = requestHeaders.entrySet().iterator();
        while (i.hasNext()) {
            Entry<String, List<String>> entry = i.next();
            List<String> list = entry.getValue();

            if (list == null) {
                nettyRequestHeaders.set(entry.getKey(), "");
                continue;
            }
            if (list.size() == 0) {
                nettyRequestHeaders.set(entry.getKey(), "");
            }
            nettyRequestHeaders.add(entry.getKey(), list);
        }

        // Send HTTP Upgrade request here
        connection.writeAndFlush(request);

        // PH10279
        // client side needs to store query string and path parameters for later retrieval from the session object
        if (poi != null) {
            Tr.debug(tc, "set query parms to " + endpointAddress.getURI().getQuery());
            if (Objects.nonNull(queryString) && !queryString.isEmpty()) {
                poi.setQueryString(endpointAddress.getURI().getQuery());
            }

            QueryStringDecoder query = new QueryStringDecoder(endpointAddress.getURI());

            for (Map.Entry<String, List<String>> entry : query.parameters().entrySet()) {
                parameterMap.put(entry.getKey(), entry.getValue());
            }
            poi.setParameterMap(parameterMap);
            Tr.debug(tc, "set ParameterMap " + parameterMap);
        }
    }

    @Override
    public WsByteBuffer completeResponse() throws IOException {
        try {
            responsePromise.get(HttpOption.READ_TIMEOUT.parse(httpOptions), TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e1) {
            // This most probably means we timed out waiting for a response and so
            // we throw a socket timeout exception to wrap around the exception reached
            throw new SocketTimeoutException(e1.getMessage());
        }
        if (resp == null) {
            throw new IOException("Don't have a response yet!");
        }
        if (StatusCodes.SWITCHING_PROTOCOLS.getIntCode() != resp.status().code()) {
            String msg = Tr.formatMessage(tc, "client.invalid.returncode", resp.status().code(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.returncode", resp.status().code(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        String acceptKey;
        try {
            acceptKey = Utils.makeAcceptResponseHeaderValue(websocketKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        String key = resp.headers().get(Constants.MC_HEADER_NAME_SEC_WEBSOCKET_ACCEPT);
        if (key != null) {
            if (!key.equals(acceptKey)) {
                String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", resp.status().code(),
                                              endpointAddress.getURI().toString());
                Tr.error(tc, "client.invalid.acceptkey", resp.status().code(),
                         endpointAddress.getURI().toString());
                throw new IOException(msg);
            }
        } else {
            String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", resp.status().code(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.acceptkey", resp.status().code(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        if (config != null) {
            Collection<String> names = resp.headers().names();
            responseHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            Iterator<String> it = names.iterator();
            while (it.hasNext()) {
                String name = it.next();
                List<String> values = resp.headers().getAll(name);
                // Check for sub protocol here so case will match
                if (name.equalsIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_PROTOCOL)) {
                    if (values != null) {
                        if (values.size() >= 1) {
                            things.setAgreedSubProtocol(values.get(0));
                        }
                    }
                }
                // Check for extensions here so case will match
                if (name.equalsIgnoreCase(Constants.HEADER_NAME_SEC_WEBSOCKET_EXTENSIONS)) {
                    if (values != null) {
                        if (values.size() >= 1) {
                            things.setNegotiatedExtensions(HandshakeProcessor.parseClientExtensions(values));
                        }
                    }
                }
                responseHeaders.put(name, values);
            }

            if (config.getConfigurator() != null) {
                HandshakeResponseExt handshakeResponse = new HandshakeResponseExt(responseHeaders);
                config.getConfigurator().afterResponse(handshakeResponse);
            }
        }

        // Finish adding needed info, this will be used during Session calls - IE session.getParameterMap, etc
        things.setURI(endpointAddress.getURI());

        // PH10279 things.setParameterMap(null);

        if (config != null) {
            things.setLocalSubProtocols(config.getPreferredSubprotocols());
        }
        things.setWsocProtocolVersion(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION);
        things.setSecure(endpointAddress.isSecure());

        // Assuming no data left here to parse
        return null;
    }

    private void updatePipelineToWebsocket() {
        // Add wsoc related handlers
        NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(connection);

        HttpClientCodec httpHandler = connection.pipeline().get(HttpClientCodec.class);
        if (Objects.isNull(httpHandler)) { // Should NOT happen
            throw new UnsupportedOperationException("Found Null Http Codec!");
        }

        connection.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);

        // Remove HTTP Codecs
        connection.pipeline().remove(HttpClientCodec.class);
        connection.pipeline().remove(HttpObjectAggregator.class);
    }

    @Override
    public void closeConnection(IOException ioe) {
        if (Objects.nonNull(connection))
            connection.close();
    }

    private class WsocClientInitializer extends ChannelInitializerWrapper {
        final ChannelInitializerWrapper parent;
        final NettyHttpRequestorWsoc10 requestor;

        public WsocClientInitializer(ChannelInitializerWrapper parent, NettyHttpRequestorWsoc10 requestor) {
            this.parent = parent;
            this.requestor = requestor;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "initChannel: Constructing pipeline");
            parent.init(ch);
            ChannelPipeline pipeline = ch.pipeline();

            // Enable SSL
            if (requestor.endpointAddress.isSecure()) {
                SSLEngine engine = null;

                if (Objects.isNull(engine) && (WsocOutboundChain.getCurrentSslOptions() == null || WsocOutboundChain.getNettyTlsProvider() == null)) { // This shouldn't happen
                    throw new IllegalStateException("Secure address requested but no SSL Options configured");
                }
                if (tc.isDebugEnabled())
                    Tr.debug(ch, tc, "initChannel", "Adding SSL Support");
                InetSocketAddress remoteAddress = requestor.endpointAddress.getRemoteAddress();
                String host = remoteAddress.getHostString();
                int port = remoteAddress.getPort();
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc, "Create SSL", new Object[] { WsocOutboundChain.getNettyTlsProvider(), host, port, WsocOutboundChain.getCurrentSslOptions() });
                SslHandler handler = WsocOutboundChain.getNettyTlsProvider().getOutboundSSLContext(WsocOutboundChain.getCurrentSslOptions(), host, Integer.toString(port), ch);
                if (handler == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(this, tc, "initChannel");
                    ch.close();
                    return;
                }
                pipeline.addFirst("SSLHandler", handler);

            }
            ch.attr(NettyHttpConstants.PROTOCOL).set("WebSocket");
            ch.attr(NettyHttpConstants.IS_OUTBOUND_KEY).set(true);
            // ADD HTTP CODEC for first upgrade request
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
            pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse res) throws Exception {
                    requestor.resp = res;
                    ctx.pipeline().remove(this);
                    requestor.updatePipelineToWebsocket();
                    requestor.responsePromise.setSuccess();
                }

                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                    ctx.fireChannelActive();
                }

            });
            pipeline.remove(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME);
        }
    }

}