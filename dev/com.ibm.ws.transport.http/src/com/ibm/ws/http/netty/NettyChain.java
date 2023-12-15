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
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.internal.HttpChain;
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
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;

/**
 *
 */
public class NettyChain extends HttpChain {

    private static final TraceComponent tc = Tr.register(NettyChain.class, HttpMessages.HTTP_TRACE_NAME, HttpMessages.HTTP_BUNDLE);

    private NettyFramework nettyFramework;

    private volatile boolean enabled = false;

    ServerBootstrapExtended bootstrap = new ServerBootstrapExtended();
    private Channel serverChannel;
    private HttpPipelineInitializer httpPipeline;

    private int stopCount = 0;
    private int startCount = 0;
    private int updateCount = 0;

    private FutureTask<ChannelFuture> channelFuture;

    /**
     * Netty Http Chain constructor
     *
     * @param owner
     * @param isHttps
     */
    public NettyChain(HttpEndpointImpl owner, boolean isHttps) {
        super(owner, isHttps);

    }

    public void initNettyChain(String endpointId, Object componentId, NettyFramework netty) {
        final String root = endpointId + (isHttps ? "-ssl" : "");
        nettyFramework = netty;
        this.endpointMgr = nettyFramework.getEndpointManager();

        MSP.log("Recorded endpointID: " + endpointId);
        MSP.log("Recorded endpointName: " + root);

        endpointName = root;
        tcpName = root;
        sslName = "SSL-" + root;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

        chainState.set(ChainState.STOPPED.val);

    }

    /**
     * Stop this chain. This chain will have to be recreated when the port is updated. Notifications
     * and follow-on of stop operation is in the chainStopped listener method.
     *
     * @return
     */
    @Override
    public synchronized void stop() {
        stopCount = stopCount + 1;

        if (chainState.get() == ChainState.STOPPED.val || chainState.get() == ChainState.STOPPING.val) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Returning from Netty stop() because state is: " + chainState.get());
            return;
        }

        if (Objects.isNull(serverChannel)) {
            chainState.set(ChainState.STOPPED.val);
            if (Objects.nonNull(channelFuture)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Netty channel not initialized. Cancelling Future...");
                channelFuture.cancel(true);
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(tc, "Netty channel not initialized, returning from stop");
            }
        } else {

            if (chainState.get() != ChainState.RESTARTING.val) {
                this.nettyFramework.stop(serverChannel, nettyFramework.getDefaultChainQuiesceTimeout());
                chainState.set(ChainState.STOPPED.val);
            }

        }

        this.endpointMgr.removeEndPoint(endpointName);
        VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
        currentConfig.clearActivePort();
        String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
        postEvent(topic, currentConfig, null);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "stop chain " + this);
        }
    }

    @Override
    public void enable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "enable chain " + this);
        }
        this.enabled = Boolean.TRUE;
    }

    @Override
    public synchronized void update(String resolvedHostName) {
        updateCount++;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "update chain " + this);
        }

        // Don't update or start the chain if it is disabled or the framework is stopping..
        if (!this.enabled || FrameworkState.isStopping()) {
            return;
        }

        final ActiveConfiguration oldConfig = currentConfig;

        // The old configuration was "valid" if it existed, and if it was correctly configured
        final boolean validOldConfig = oldConfig == null ? false : oldConfig.validConfiguration;

        Map<String, Object> tcpOptions = this.getOwner().getTcpOptions();
        Map<String, Object> sslOptions = (this.isHttps()) ? this.getOwner().getSslOptions() : null;
        Map<String, Object> httpOptions = this.getOwner().getHttpOptions();
        Map<String, Object> endpointOptions = this.getOwner().getEndpointOptions();
        Map<String, Object> remoteIpOptions = this.getOwner().getRemoteIpConfig();
        Map<String, Object> compressionOptions = this.getOwner().getCompressionConfig();
        Map<String, Object> samesiteOptions = this.getOwner().getSamesiteConfig();
        Map<String, Object> headersOptions = this.getOwner().getHeadersConfig();

        // currentConfig = new ActiveConfiguration(this.isHttps(), tcpOptions, sslOptions, httpOptions, remoteIpOptions, compressionOptions, samesiteOptions, headersOptions, endpointOptions, resolvedHostName);

        final ActiveConfiguration newConfig = new ActiveConfiguration(this.isHttps(), tcpOptions, sslOptions, httpOptions, remoteIpOptions, compressionOptions, samesiteOptions, headersOptions, endpointOptions, resolvedHostName);

        if (newConfig.configPort < 0 || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }
        }

        // save the new/changed configuration before we start setting up the new chain
        currentConfig = newConfig;

        if (newConfig.unchanged(oldConfig)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(this, tc, "Found unchanged config! Doing nothing and returning...");
            }
            return;
        }

        stop();

        startNettyChannel();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "update chain " + this);
        }
    }

    public synchronized void startNettyChannel() {
        startCount = startCount + 1;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "starting netty channel with state: " + chainState.get());
        }

        if (!(chainState.get() == ChainState.STOPPED.val)) {// || chainState.get() == ChainState.RESTARTING.val)) {
            MSP.log("Chain already started, returning");
            return;
        }

        //TODO: clean up less clogged active configuration
        httpPipeline = null;
        Map<String, Object> httpOptions = new HashMap<String, Object>();
        boolean restarting = chainState.get() == ChainState.RESTARTING.val;
        owner.getHttpOptions().forEach(httpOptions::putIfAbsent);
        // Put the endpoint id, which allows us to find the registered access log
        // dynamically
        httpOptions.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, owner.getName());
        httpOptions.keySet().forEach(MSP::log);
        // Put the protocol version, which allows the http channel to dynamically
        // know what http version it will use.
        if (owner.getProtocolVersion() != null) {
            httpOptions.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
        }

        EndPointInfo info = this.endpointMgr.getEndPoint(this.endpointName);
        info = this.endpointMgr.defineEndPoint(this.endpointName, currentConfig.configHost, currentConfig.configPort);

        try {
            Map<String, Object> tcpOptions = new HashMap<String, Object>();

            this.getOwner().getTcpOptions().forEach(tcpOptions::putIfAbsent);
            tcpOptions.put(ConfigConstants.EXTERNAL_NAME, endpointName);

            this.bootstrap = nettyFramework.createTCPBootstrap(tcpOptions);

            httpPipeline = new HttpPipelineInitializer.HttpPipelineBuilder(this).with(ConfigElement.COMPRESSION,
                                                                                      this.owner.getCompressionConfig()).with(ConfigElement.HTTP_OPTIONS,
                                                                                                                              httpOptions).with(ConfigElement.HEADERS,
                                                                                                                                                this.owner.getHeadersConfig()).with(ConfigElement.REMOTE_IP,
                                                                                                                                                                                    this.owner.getRemoteIpConfig()).with(ConfigElement.SAMESITE,
                                                                                                                                                                                                                         this.owner.getSamesiteConfig()).build();

            bootstrap.childHandler(httpPipeline);
            NettyChain parent = this;
            chainState.set(ChainState.INITIALIZED.val);
            channelFuture = nettyFramework.start(bootstrap, info.getHost(), info.getPort(), f -> {
                if (f.isCancelled() || !f.isSuccess()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(this, tc, "Problem in future for starting the chain " + f.cause());
                        StringBuilder sb = new StringBuilder();
                        sb.append("Netty stop() NettyChain").append("stop() -> endpoint removed").append("owner:"
                                                                                                         + owner.toString()).append("host: "
                                                                                                                                    + currentConfig.getResolvedHost()).append("port: "
                                                                                                                                                                              + currentConfig.getConfigPort()).append("isHttps:"
                                                                                                                                                                                                                      + isHttps);
                        Tr.debug(this, tc, sb.toString());
                    }
                    this.endpointMgr.removeEndPoint(endpointName);
                    VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                    currentConfig.clearActivePort();
                    String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
                    postEvent(topic, currentConfig, null);
                } else {
                    parent.chainState.set(ChainState.STARTED.val);
                    parent.serverChannel = f.channel();
                    VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                    String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
                    postEvent(topic, currentConfig, null);
                }
            });
            if (restarting)
                channelFuture.get(nettyFramework.getDefaultChainQuiesceTimeout(), TimeUnit.MILLISECONDS).await();
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Problem in starting the chain " + e);
            }
        }
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
        return endpointMgr.getEndPoint(endpointName);
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

        if (defaultSetting == null) // No default configured, only HTTP 1.1 is enabled
            return false;
        else
            return defaultSetting == Boolean.TRUE ? !!!HttpConfigConstants.PROTOCOL_VERSION_11.equalsIgnoreCase(protocolVersion) : HttpConfigConstants.PROTOCOL_VERSION_2.equalsIgnoreCase(protocolVersion);
    }

    public VirtualConnection processNewConnection() {
        VirtualConnectionFactory factory = new NettyVirtualConnectionFactoryImpl();
        VirtualConnection vc;

        try {
            vc = factory.createConnection();
            return vc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
