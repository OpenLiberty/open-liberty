/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.TreeMap;
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
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.exception.NettyException;

import com.ibm.ws.http.netty.inbound.NettyTCPConnectionContext;
import com.ibm.ws.netty.upgrade.NettyServletUpgradeHandler;


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

    private FullHttpResponse resp;

    private final ParametersOfInterest things;

    private final Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();

    public NettyHttpRequestorWsoc10(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        System.out.println("New netty http requestor!!");
        this.endpointAddress = endpointAddress;
        this.config = config;
        this.things = things;
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
//        access.setVirtualConnection(vc);
//        vc.connect(endpointAddress);
    }

    private void startConnection() throws InterruptedException, ExecutionException, NettyException {
        InetSocketAddress remoteAddress = endpointAddress.getRemoteAddress();
        String host = remoteAddress.getHostString();
        int port = remoteAddress.getPort();
        factory.handler(new WsocClientInitializer(factory.getBaseInitializer(), this));
        connection = WsocOutboundChain.getNettyFramework().startOutbound(factory, host, port, null).get().sync().channel();
        responsePromise = connection.newPromise();
    }

    @Override
    public void sendRequest() throws IOException, MessageSentException {
        System.out.println("Sending request!");
        sendRequest(null);
//        throw new IOException("Working on it!");
    }

    @Override
    public void sendRequest(ParametersOfInterest poi) throws IOException, MessageSentException {
        System.out.println("Sending request with parameters!");
        // TODO Need to set Netty TCP Conn context here
//        access.setTCPConnectionContext(httpOutboundSC.getTSC());
//        access.setDeviceConnLink(httpOutboundSC.getLink());
        access.setTCPConnectionContext(new NettyTCPConnectionContext(connection, null));
        access.setDeviceConnLink(new ConnectionLink() {

            @Override
            public void ready(VirtualConnection vc) {
                // TODO Auto-generated method stub

            }

            @Override
            public void destroy(Exception e) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setDeviceLink(ConnectionLink next) {
                // TODO Auto-generated method stub

            }

            @Override
            public void setApplicationCallback(ConnectionReadyCallback next) {
                // TODO Auto-generated method stub

            }

            @Override
            public VirtualConnection getVirtualConnection() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ConnectionLink getDeviceLink() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public Object getChannelAccessor() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public ConnectionReadyCallback getApplicationCallback() {
                // TODO Auto-generated method stub
                return null;
            }

            @Override
            public void close(VirtualConnection vc, Exception e) {
                System.out.println("Closing connection from websocket!");
                connection.close();
            }
        });
        
        String uriPath = endpointAddress.getURI().getPath();
        String queryString = endpointAddress.getURI().getQuery();
        String finalUri = uriPath + (queryString != null && !queryString.isEmpty() ? "?" + queryString : "");

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, finalUri);
        HttpUtil.setContentLength(request, 0);
        request.headers().set(HttpHeaderKeys.HDR_HOST.getName(), endpointAddress.getURI().getHost() + ":" + endpointAddress.getURI().getPort());

        HttpHeaders nettyRequestHeaders = request.headers();

//        hrm.setRequestURI(endpointAddress.getPath());
//
//        // PH10279
//        hrm.setQueryString(endpointAddress.getURI().getQuery());
//
//        hrm.setVersion(VersionValues.V11);
//        hrm.setMethod(MethodValues.GET);

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

//        httpOutboundSC.enableImmediateResponseRead();
//        httpOutboundSC.sendRequestHeaders();

        // TODO Send HTTP Upgrade request here
        connection.writeAndFlush(request);

        // PH10279
        // client side needs to store query string and path parameters for later retrieval from the session object
        if (poi != null) {
            Tr.debug(tc, "set query parms to " + endpointAddress.getURI().getQuery());
            if(Objects.nonNull(queryString) && !queryString.isEmpty()) {
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
        System.out.println("Completing response!");
        try {
            responsePromise.get(20000, TimeUnit.MILLISECONDS);
        }catch (InterruptedException | ExecutionException | TimeoutException e1) {
            e1.printStackTrace();
        }
        if (resp == null) {
            throw new IOException("Don't have a response yet! Do we need to wait for it?");
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

        // TODO update the pipeline to remove http and have wsoc specific handlers after successful upgrade

        // Assuming no data left here to parse
        return null;

//        throw new IOException("Working on it!");
    }

    private void updatePipelineToWebsocket() {
        // TODO Add wsoc related handlers
        NettyServletUpgradeHandler upgradeHandler = new NettyServletUpgradeHandler(connection);

//        upgradeHandler.setVC(vc); // Don't think this is necessary
        HttpClientCodec httpHandler = connection.pipeline().get(HttpClientCodec.class);
        if (httpHandler == null) {
            System.out.println("Found null handler HTTP!");
            throw new UnsupportedOperationException("Can't deal with this");
        }
        System.out.println("Should remove remove the dispatch handler, only keep reading and writing upgrade handler");

        // nettyContext.channel().pipeline().addBefore(nettyContext.channel().pipeline().context(httpHandler).name(), "ServletUpgradeHandler", upgradeHandler);
        //nettyContext.channel().pipeline().addLast(new WebSocketServerProtocolHandler("/websocket")); // Handles the WebSocket upgrade and control frames
        connection.pipeline().addLast("ServletUpgradeHandler", upgradeHandler);

        // nettyContext.channel().pipeline().remove(LibertyHttpObjectAggregator.class);

        // if(nettyContext.channel().pipeline().get(HttpDispatcherHandler.class)
        // nettyContext.channel().pipeline().remove(HttpDispatcherHandler.class);
        System.out.println(connection.pipeline().names());

        // Remove HTTP Codecs
        connection.pipeline().remove(HttpClientCodec.class);
        connection.pipeline().remove(HttpObjectAggregator.class);
    }

    @Override
    public void closeConnection(IOException ioe) {
       if( Objects.nonNull(connection))
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

         // TODO enable SSL
            if (requestor.endpointAddress.isSecure()) {
                SSLEngine engine = null;
                if(requestor.endpointAddress instanceof Wsoc21Address) {
                    System.out.println("Attempting to pull sslcontext from Wsoc21Address");
                    engine = ((Wsoc21Address) requestor.endpointAddress).getSSLContext().createSSLEngine();
                    engine.setUseClientMode(true);
                    System.out.println("Pulled engine: " + Objects.nonNull(engine));
                }
                
                
                if (Objects.isNull(engine) &&( WsocOutboundChain.currentSSL == null || WsocOutboundChain.getNettyTlsProvider() == null)) { // This shouldn't happen
                    System.out.println("Oh no, secure address requested but no SSL Options found!");
                    throw new IllegalStateException("This ");
                }
                if (tc.isDebugEnabled())
                    Tr.debug(ch, tc, "initChannel", "Adding SSL Support");
                InetSocketAddress remoteAddress = requestor.endpointAddress.getRemoteAddress();
                String host = remoteAddress.getHostString();
                int port = remoteAddress.getPort();
                if (tc.isDebugEnabled())
                    Tr.debug(this, tc, "Create SSL", new Object[] { WsocOutboundChain.getNettyTlsProvider(), host, port, WsocOutboundChain.currentSSL });
                
                if(Objects.isNull(engine)) {
                SslContext context = WsocOutboundChain.getNettyTlsProvider().getOutboundSSLContext(WsocOutboundChain.currentSSL, host, Integer.toString(port));
                
                if (context == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(this, tc, "initChannel");
                    ch.close();
                    return;
                }
                engine = context.newEngine(ch.alloc());
                }
                pipeline.addFirst("SSLHandler", new SslHandler(engine, false));

            }
            ch.attr(NettyHttpConstants.PROTOCOL).set("WebSocket");
            ch.attr(NettyHttpConstants.IS_OUTBOUND_KEY).set(true);
            // ADD HTTP CODEC for first upgrade request
            pipeline.addLast(new HttpClientCodec());
            pipeline.addLast(new HttpObjectAggregator(Integer.MAX_VALUE));
            pipeline.addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {

                @Override
                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse res) throws Exception {
                    System.out.println("Alrighty here we go! " + res);
                    requestor.resp = res;
                    ctx.pipeline().remove(this);
                    requestor.updatePipelineToWebsocket();
                    requestor.responsePromise.setSuccess();
                }
            });
        }
    }

}