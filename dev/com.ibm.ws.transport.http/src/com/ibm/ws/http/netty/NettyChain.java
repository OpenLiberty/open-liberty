/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.internal.HttpChain;
import com.ibm.ws.http.internal.HttpChain.ActiveConfiguration;
import com.ibm.ws.http.internal.HttpChain.ChainState;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.internal.HttpServiceConstants;
import com.ibm.ws.http.internal.VirtualHostMap;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer.ConfigElement;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.openliberty.http.netty.quiesce.QuiesceStrategy;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;

/**
 *
 */
public class NettyChain extends HttpChain {

    private static final TraceComponent tc = Tr.register(NettyChain.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private NettyFramework nettyFramework;
    private ServerBootstrapExtended bootstrap;
    private volatile Channel serverChannel;
    private FutureTask<ChannelFuture> channelFuture;

    private final AtomicReference<ChainState> state = new AtomicReference<>(ChainState.UNINITIALIZED);

    private volatile boolean enabled = false;

    /**
     * Netty Http Chain constructor
     *
     * @param owner
     * @param isHttps
     */
    public NettyChain(HttpEndpointImpl owner, boolean isHttps) {
        super(owner, isHttps);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "NettyChain constructor, state: " + (state != null ? state.get() : "null"));
        }
    }

    public synchronized void initNettyChain(String endpointId, NettyFramework netty) {

        Objects.requireNonNull(netty, "NettyFramework cannot be null");
        this.nettyFramework = netty;
        endpointMgr = nettyFramework.getEndpointManager();

        final String root = endpointId + (isHttps ? "-ssl" : "");

        endpointName = root;
        tcpName = "TCP-" + root;
        sslName = isHttps ? "SSL-" + root : null;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Netty Chain initialized: Endpoint ID = " + endpointId + ", Endpoint Name = " + root);
        }

    }

    @Override
    public synchronized void stop() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Stopping Netty Chain: " + endpointName + ", Current state: " + state.get());
        }

        if (state.get() == ChainState.STARTED || state.get() == ChainState.STARTING) {
            endpointMgr.removeEndPoint(endpointName);
            state.set(ChainState.STOPPING);

            try {
                if (Objects.nonNull(serverChannel) && serverChannel.isOpen()) {

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Server Channel is open, attempting to close");
                    }

                    nettyFramework.stop(serverChannel, -1);
                    serverChannel.closeFuture().syncUninterruptibly();
                    serverChannel = null;
                }

            } finally {

                VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                currentConfig.clearActivePort();
                String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
                postEvent(topic, currentConfig, null);

                state.set(ChainState.STOPPED);
                notifyAll();
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Netty Chain is not in a stoppable state. Current state: " + state.get());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "stop chain " + this);
        }
    }

    private void stopAndWait() {
        if (state.get() != ChainState.STOPPED && state.get() != ChainState.UNINITIALIZED) {
            stop();
        }
    }

    @Override
    public synchronized void update(String resolvedHostName) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Updating Netty Chain  " + endpointName + " Current state: " + state.get());
        }

        if (!enabled || FrameworkState.isStopping()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.exit(this, tc, "Chain is disabled or framework is stopping, skipping update ");
            }
            return;
        }

        final ActiveConfiguration oldConfig = currentConfig;
        final ActiveConfiguration newConfig = new ActiveConfiguration(isHttps(), getOwner().getTcpOptions(), isHttps() ? getOwner().getSslOptions() : null, getOwner().getHttpOptions(), getOwner().getRemoteIpConfig(), getOwner().getCompressionConfig(), getOwner().getSamesiteConfig(), getOwner().getHeadersConfig(), getOwner().getEndpointOptions(), resolvedHostName);

        if (newConfig.configPort < 0 || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }
            // save the new/changed configuration before we start setting up the new chain
            currentConfig = newConfig;
            stopAndWait();
            state.set(ChainState.UNINITIALIZED);
        }

        else {

            if (!newConfig.unchanged(oldConfig)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "This configuration differs and should cause an update ");
                }
                currentConfig = newConfig;
                if (state.get() != ChainState.UNINITIALIZED) {
                    stopAndWait();
                }
            }
            startNettyChannel();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Channel restarted with new configuration");
        }
    }

    public synchronized void startNettyChannel() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "Starting Netty Channel: " + endpointName + ", Current state: " + state.get() + ", Enabled: " + enabled);
        }

        // if (currentConfig == null || !currentConfig.complete() || !enabled || FrameworkState.isStopping()) {
        //     if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        //         Tr.debug(this, tc, "Cannot start channel due to incomplete configuration or disabled state");
        //     }
        //     return;
        // }

        if (state.compareAndSet(ChainState.STOPPED, ChainState.STARTING) || state.compareAndSet(ChainState.UNINITIALIZED, ChainState.STARTING)) {
            try {
                Map<String, Object> httpOptions = new HashMap<>(owner.getHttpOptions());
                httpOptions.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, owner.getName());
                // Put the protocol version, which allows the http channel to dynamically
                // know what http version it will use.
                if (owner.getProtocolVersion() != null) {
                    httpOptions.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
                }

                EndPointInfo info = endpointMgr.defineEndPoint(this.endpointName, currentConfig.configHost, currentConfig.configPort);

                Map<String, Object> tcpOptions = new HashMap<>(this.getOwner().getTcpOptions());
                tcpOptions.put(ConfigConstants.EXTERNAL_NAME, endpointName);

                bootstrap = nettyFramework.createTCPBootstrap(tcpOptions);
                HttpPipelineInitializer.HttpPipelineBuilder pipelineBuilder = new HttpPipelineInitializer.HttpPipelineBuilder(this).with(ConfigElement.COMPRESSION,
                                                                                                                                         owner.getCompressionConfig()).with(ConfigElement.HTTP_OPTIONS,
                                                                                                                                                                            httpOptions).with(ConfigElement.HEADERS,
                                                                                                                                                                                              owner.getHeadersConfig()).with(ConfigElement.REMOTE_IP,
                                                                                                                                                                                                                             owner.getRemoteIpConfig()).with(ConfigElement.SAMESITE,
                                                                                                                                                                                                                                                             owner.getSamesiteConfig());

                // Add SSL options only if the chain is SSL-enabled
                if (this.isHttps()) {
                    Map<String, Object> sslOptions = new HashMap<>(this.getOwner().getSslOptions());
                    pipelineBuilder.with(ConfigElement.SSL_OPTIONS, sslOptions);
                }

                HttpPipelineInitializer httpPipeline = pipelineBuilder.build();

                bootstrap.childOption(ChannelOption.ALLOW_HALF_CLOSURE, true);
                bootstrap.childHandler(httpPipeline);

                serverChannel = nettyFramework.start(bootstrap, info.getHost(), info.getPort(), this::channelFutureHandler);

                VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
                postEvent(topic, currentConfig, null);

            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.exit(this, tc, "Failed to start Netty Channel: " + e.getMessage());
                }
                state.set(ChainState.STOPPED);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "Finished starting Netty Channel: " + endpointName + ", Final state: " + state.get());
        }

    }

    private void channelFutureHandler(ChannelFuture future) {
        if (state.get() == ChainState.STOPPING) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Chain: " + endpointName + ", Current state: " + state.get() + ", is stopping so will not notify any virtual hosts and will just return");
            }
            return;
        }
        synchronized (this) {
            if (future.isSuccess()) {
                state.set(ChainState.STARTED);
                EndPointInfo info = endpointMgr.getEndPoint(this.endpointName);
                info = endpointMgr.defineEndPoint(this.endpointName, currentConfig.configHost, currentConfig.configPort);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Channel is now active and listening on port " + getActivePort());
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Channel failed to bind to port:  " + future.cause());
                }
                handleStartupError(new NettyException(future.cause()), currentConfig);

                if (currentConfig != null) {
                    VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                    currentConfig.clearActivePort();
                }
                state.set(ChainState.STOPPED);
            }
            //Register chain for quiesce, NO_OP is passed as the task as there is no special 
            //quiesce action required at this time
            nettyFramework.registerEndpointQuiesce(future.channel(), QuiesceStrategy.NO_OP.getTask());
            notifyAll();
        }
    }

    @Override
    public void enable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Enabling Netty chain: " + this);
        }
        enabled = true;

    }

    /**
     * Disable this chain. This does not change the chain's state. The caller should
     * make subsequent calls to perform actions on the chain.
     */
    @Override
    public void disable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "disable chain " + this);
        }
        enabled = false;

    }

    @Override
    public int getActivePort() {
        return (currentConfig != null) ? currentConfig.configPort : -1;
    }

    public String getActiveHost() {
        return (currentConfig != null) ? currentConfig.configHost : null;
    }

    /**
     * @return the bootstrap
     */
    public ServerBootstrapExtended getBootstrap() {
        return bootstrap;
    }

    public EndPointInfo getEndpointInfo() {
        EndPointInfo info = endpointMgr.getEndPoint(endpointName);

        if (Objects.isNull(info) && state.get() == ChainState.STARTED) {
            info = endpointMgr.defineEndPoint(this.endpointName, currentConfig.configHost, currentConfig.configPort);
        }

        return info;
        //   return endpointMgr.getEndPoint(endpointName);
    }

    public String getEndpointPID() {
        return (currentConfig != null) ? currentConfig.getEndpointPID() : null;
    }

    /**
     * Helper method to check if the chain is enabled with HTTP/2.0 or only HTTP/1.1. To do this
     * we check the HttpProtocolBehavior reference which is set according to the different servlet
     * version loaded. We then compare with the set protocol version in the HttpEndpoint to decide
     * which protocol the chain is loaded with.
     *
     * @return true if HTTP/2.0 is enabled on the chain. False if HTTP/1.1 is enabled on the chain
     */
    public boolean isHttp2Enabled() {
        String protocolVersion = getOwner().getProtocolVersion();
        Boolean defaultSetting = getOwner().getChfwBundle().getHttp2DefaultSetting();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "Protocol version found to be:  " + protocolVersion);
        }
        if (defaultSetting == null) // No default configured, only HTTP 1.1 is enabled
            return false;
        else
            return defaultSetting == Boolean.TRUE ? !!!HttpConfigConstants.PROTOCOL_VERSION_11.equalsIgnoreCase(protocolVersion) : HttpConfigConstants.PROTOCOL_VERSION_2.equalsIgnoreCase(protocolVersion);
    }

    public VirtualConnection processNewConnection() {
        VirtualConnectionFactory factory = new NettyVirtualConnectionFactoryImpl();
        VirtualConnection vc;

        try {
            return factory.createConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getChainState() {
        return state.get().val;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
               + "[@=" + System.identityHashCode(this)
               + ",enabled=" + enabled
               + ",state=" + (state != null ? state.get() : "null")
               + ",chainName=" + chainName
               + ",config=" + currentConfig + "]";

    }
}
