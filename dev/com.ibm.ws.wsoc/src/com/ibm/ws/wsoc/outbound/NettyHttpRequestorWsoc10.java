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
package com.ibm.ws.wsoc.outbound;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLEngine;
import javax.websocket.ClientEndpointConfig;
import javax.websocket.Extension;
import javax.websocket.Extension.Parameter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsoc.Constants;
import com.ibm.ws.wsoc.HandshakeProcessor;
import com.ibm.ws.wsoc.ParametersOfInterest;
import com.ibm.ws.wsoc.WebSocketContainerManager;
import com.ibm.ws.wsoc.external.HandshakeResponseExt;
import com.ibm.ws.wsoc.util.Utils;
import com.ibm.wsspi.bytebuffer.WsByteBuffer;
import com.ibm.wsspi.channelfw.ConnectionReadyCallback;
import com.ibm.wsspi.channelfw.OutboundVirtualConnection;
import com.ibm.wsspi.genericbnf.HeaderField;
import com.ibm.wsspi.genericbnf.exception.MessageSentException;
import com.ibm.wsspi.http.channel.HttpRequestMessage;
import com.ibm.wsspi.http.channel.HttpResponseMessage;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;
import com.ibm.wsspi.http.channel.values.HttpHeaderKeys;
import com.ibm.wsspi.http.channel.values.MethodValues;
import com.ibm.wsspi.http.channel.values.StatusCodes;
import com.ibm.wsspi.http.channel.values.VersionValues;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.ssl.SslContext;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tls.NettyTlsProvider;

/**
 *
 */
public class NettyHttpRequestorWsoc10 implements HttpRequestor {

    private static final TraceComponent tc = Tr.register(NettyHttpRequestorWsoc10.class);

    protected ClientTransportAccess access = null;

    protected WsocAddress endpointAddress = null;

    protected OutboundVirtualConnection vc = null;

    private HttpOutboundServiceContext httpOutboundSC = null;

    private String websocketKey = "";

    private final Map<String, List<String>> requestHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);

    private Map<String, List<String>> responseHeaders = null;

    protected ClientEndpointConfig config;

    private final ParametersOfInterest things;

    // Virtual Connection
    private Channel chan;

    private String chainName;

    private final Map<String, Object> tcpOptions = null;;
    private final Map<String, Object> sslOptions = null;

    private NettyTlsProvider tlsProvider;

    private NettyFramework nettyBundle;

    private BootstrapExtended bootstrap;

    public NettyHttpRequestorWsoc10(WsocAddress endpointAddress, ClientEndpointConfig config, ParametersOfInterest things) {
        Tr.debug(this, tc, "<init>");

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

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(this, tc, "connect");
        }

        final NettyHttpRequestorWsoc10 readyConnection = this;

        access = new ClientTransportAccess();

        try {
            nettyBundle = WsocOutboundChain.getNettyBundle();

            bootstrap = WsocOutboundChain.getOutboundBootstrap();

            bootstrap.handler(new NettyWsocClientInitializer(bootstrap.getBaseInitializer(), endpointAddress, null));

            NettyHttpRequestorWsoc10 parent = this;
            int PORT_BIND_TIMEOUT_MS = 60 * 1000;
            Tr.debug(this, tc, "Netty connecting to " + endpointAddress.getRemoteAddress().getAddress().getHostAddress() + ":" + endpointAddress.getRemoteAddress().getPort());

            FutureTask<ChannelFuture> startFuture = nettyBundle.startOutbound(this.bootstrap,
                                                                              endpointAddress.getRemoteAddress().getAddress().getHostAddress(),
                                                                              endpointAddress.getRemoteAddress().getPort(),
                                                                              callback -> {
                                                                                  if (callback.isSuccess()) {
                                                                                      chan = callback.channel();
                                                                                  } else {

                                                                                      throw new Exception(callback.cause());
                                                                                  }
                                                                              });

            startFuture.get(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS).await(PORT_BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            //nettyBundle.startOutbound(this.bootstrap,
            //                          endpointAddress.getRemoteAddress().getAddress().getHostAddress(),
            //                          endpointAddress.getRemoteAddress().getPort(), f -> {
            //                              if (f.isCancelled() || !f.isSuccess()) {
            //                                  Tr.debug(this, tc, "Channel exception during connect: " + f.cause().getMessage());
            //                                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            //                                      Tr.entry(parent, tc, "destroy", f.cause());
            //                                  //listener.connectRequestFailedNotification((Exception) f.cause());
            //                                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            //                                      Tr.exit(parent, tc, "destroy");
            //                              } else {
            //                                 if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            //                                      Tr.entry(parent, tc, "ready", f);
            //                                  //parent.chan = f.channel();
            //                                  //listener.connectRequestSucceededNotification(readyConnection);
            //                                  if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            //                                      Tr.exit(parent, tc, "ready");
            //                              }
            //
            //                          });

        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(this, tc, "netty connect failed ", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "connect");

    }

    @Override
    public void sendRequest() throws IOException, MessageSentException {
        sendRequest(null);
    }

    /**
     * @return Returns the channel connection.
     */
    Channel getVirtualConnection() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(this, tc, "getVirtualConnection");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(this, tc, "getVirtualConnection", chan);
        return this.chan;
    }

    private final Map<String, List<String>> parameterMap = new HashMap<String, List<String>>();

    @Override
    public void sendRequest(ParametersOfInterest poi) throws IOException, MessageSentException {
        Tr.debug(this, tc, "About to call vc.getChannelAccessor()");
        httpOutboundSC = (HttpOutboundServiceContext) vc.getChannelAccessor();
        Tr.debug(this, tc, "About to set tcpconncontext");
        access.setTCPConnectionContext(httpOutboundSC.getTSC());
        Tr.debug(this, tc, "About to set deviceconnlink");
        access.setDeviceConnLink(httpOutboundSC.getLink());
        Tr.debug(this, tc, "About to get request");

        HttpRequestMessage hrm = httpOutboundSC.getRequest();
        Tr.debug(this, tc, "About to set uri");

        hrm.setRequestURI(endpointAddress.getPath());
        Tr.debug(this, tc, "About to set query string");
        // PH10279
        hrm.setQueryString(endpointAddress.getURI().getQuery());
        Tr.debug(this, tc, "About to set version and method");
        hrm.setVersion(VersionValues.V11);
        hrm.setMethod(MethodValues.GET);
        Tr.debug(this, tc, "About to set requestHeaders");

        //   We put request headers in Map for possible modification by configurator beforeRequest, and also used by Session
        requestHeaders.put(HttpHeaderKeys.HDR_CONNECTION.getName(), Arrays.asList(Constants.HEADER_VALUE_UPGRADE));
        requestHeaders.put(HttpHeaderKeys.HDR_UPGRADE.getName(), Arrays.asList(Constants.HEADER_VALUE_WEBSOCKET));
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_VERSION, Arrays.asList(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION));
        Tr.debug(this, tc, "About to set websocket key");
        websocketKey = WebSocketContainerManager.getRef().generateWebsocketKey();
        requestHeaders.put(Constants.HEADER_NAME_SEC_WEBSOCKET_KEY, Arrays.asList(websocketKey));

        if (config != null) {
            Tr.debug(this, tc, "In if config !=null");
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
                hrm.setHeader(entry.getKey(), "");
                continue;
            }
            if (list.size() == 0) {
                hrm.setHeader(entry.getKey(), "");
            }
            for (int x = 0; x < list.size(); x++) {
                if (x == 0) {
                    hrm.setHeader(entry.getKey(), list.get(x));
                } else {
                    hrm.appendHeader(entry.getKey(), list.get(x));
                }
            }
        }

        httpOutboundSC.enableImmediateResponseRead();
        httpOutboundSC.sendRequestHeaders();

        // PH10279
        // client side needs to store query string and path parameters for later retrieval from the session object
        if (poi != null) {
            Tr.debug(tc, "set query parms to " + endpointAddress.getURI().getQuery());
            poi.setQueryString(endpointAddress.getURI().getQuery());

            if (hrm != null) {
                Map<String, String[]> paramMap = hrm.getParameterMap();
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    String key = entry.getKey();
                    String[] value = entry.getValue();
                    List<String> list = Arrays.asList(value);
                    parameterMap.put(key, list);
                }
                poi.setParameterMap(parameterMap);
                Tr.debug(tc, "set ParameterMap " + parameterMap);

            }
        }
    }

    @Override
    public WsByteBuffer completeResponse() throws IOException {
        HttpResponseMessage hrm = httpOutboundSC.getResponse();

        if (!StatusCodes.SWITCHING_PROTOCOLS.equals(hrm.getStatusCode())) {
            String msg = Tr.formatMessage(tc, "client.invalid.returncode", hrm.getStatusCode(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.returncode", hrm.getStatusCode(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        String acceptKey;
        try {
            acceptKey = Utils.makeAcceptResponseHeaderValue(websocketKey);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        String key = hrm.getHeader(Constants.MC_HEADER_NAME_SEC_WEBSOCKET_ACCEPT).asString();
        if (key != null) {
            if (!key.equals(acceptKey)) {
                String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                                              endpointAddress.getURI().toString());
                Tr.error(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                         endpointAddress.getURI().toString());
                throw new IOException(msg);
            }
        } else {
            String msg = Tr.formatMessage(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                                          endpointAddress.getURI().toString());
            Tr.error(tc, "client.invalid.acceptkey", hrm.getStatusCode(),
                     endpointAddress.getURI().toString());
            throw new IOException(msg);
        }

        if (config != null) {
            Collection<String> names = hrm.getAllHeaderNames();
            responseHeaders = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
            Iterator<String> it = names.iterator();
            while (it.hasNext()) {
                String name = it.next();

                List<HeaderField> hdrs = hrm.getHeaders(name);
                List<String> values = new ArrayList<String>(hdrs.size());
                for (HeaderField header : hdrs) {
                    values.add(header.asString());
                }

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

        WsByteBuffer retBuf = httpOutboundSC.getRemainingData();

        // Finish adding needed info, this will be used during Session calls - IE session.getParameterMap, etc
        things.setURI(endpointAddress.getURI());

        // PH10279 things.setParameterMap(null);

        if (config != null) {
            things.setLocalSubProtocols(config.getPreferredSubprotocols());
        }
        things.setWsocProtocolVersion(Constants.HEADER_VALUE_FOR_SEC_WEBSOCKET_VERSION);
        things.setSecure(endpointAddress.isSecure());

        return retBuf;

    }

    @Override
    public void closeConnection(IOException ioe) {
        if (access != null) {
            if (access.getDeviceConnLink() != null) {
                access.getDeviceConnLink().close(vc, ioe);
            }
        }
    }

    /**
     * ChannelInitializer for Wsoc Client over TCP, and optionally TLS with Netty
     */
    private class NettyWsocClientInitializer extends ChannelInitializerWrapper {
        final ChannelInitializerWrapper parent;
        final WsocAddress target;
        final ConnectionReadyCallback listener;

        public static final String SSL_HANDLER_KEY = "sslHandler";
        public static final String DECODER_HANDLER_KEY = "decoder";
        public static final String ENCODER_HANDLER_KEY = "encoder";
        private static final String WSOC_CLIENT_HANDLER_KEY = "wsocHandler";

        public NettyWsocClientInitializer(ChannelInitializerWrapper parent, WsocAddress target, ConnectionReadyCallback listener) {
            this.parent = parent;
            this.target = target;
            this.listener = listener;
        }

        @Override
        protected void initChannel(Channel ch) throws Exception {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(this, tc, "initChannel", "Constructing pipeline");
            parent.init(ch);
            ChannelPipeline pipeline = ch.pipeline();
            // LLA TODO fix ssloptions
            Map<String, Object> sslOptions = null;
            if (sslOptions != null) {
                if (tc.isDebugEnabled())
                    Tr.debug(ch, tc, "initChannel", "Adding SSL Support");
                String host = target.getRemoteAddress().getHostName();
                String port = Integer.toString(target.getRemoteAddress().getPort());
                //if (tc.isDebugEnabled())
                //    Tr.debug(this, tc, "Create SSL", new Object[] { tlsProvider, host, port, sslOptions });
                //SslContext context = tlsProvider.getOutboundSSLContext(sslOptions, host, port);
                // LLA TODO fix sslcontext
                SslContext context = null;
                if (context == null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.entry(this, tc, "initChannel", "Error adding TLS Support");
                    listener.destroy(new NettyException("Problems creating SSL context"));
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        Tr.exit(this, tc, "initChannel");
                    ch.close();
                    return;
                }
                SSLEngine engine = context.newEngine(ch.alloc());
                // LLA TODO
                //pipeline.addFirst(NettyNetworkConnectionFactory.SSL_HANDLER_KEY, new SslHandler(engine, false));
            }
            pipeline.addLast(DECODER_HANDLER_KEY, new NettyToWsBufferDecoder());
            pipeline.addLast(ENCODER_HANDLER_KEY, new WsBufferToNettyEncoder());
            pipeline.addLast(WSOC_CLIENT_HANDLER_KEY, new NettyWsocClientHandler());
        }
    }

}
