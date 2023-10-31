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
        MSP.log("stop: " + stopCount);
        Thread.currentThread().dumpStack();

        if (chainState.get() == ChainState.STOPPED.val || chainState.get() == ChainState.STOPPING.val) {
            MSP.log("Returning from Netty stop() becuse state is: " + chainState.get());
            return;
        }

        MSP.log("Netty stop() NettyChain");

        if (Objects.isNull(serverChannel)) {
            chainState.set(ChainState.STOPPED.val);
            if (Objects.nonNull(channelFuture)) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "Netty channel not initialized. Cancelling Future...");
                channelFuture.cancel(true);
                return;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(tc, "Netty channel not initialized, returning from stop");
            }

            return;
        } else {

            MSP.log("Removing endpoint, virtual host notify");
            //chainState.set(ChainState.STOPPING.val);

            //When channel is stopped, remove the previously registered endpoint
            //created in the update
            this.endpointMgr.removeEndPoint(endpointName);
            MSP.log("stop() -> endpoint removed");
            MSP.log("owner:" + owner.toString());
            MSP.log("host: " + currentConfig.getResolvedHost());
            MSP.log("port: " + currentConfig.getConfigPort());
            MSP.log("isHttps:" + isHttps);
            VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
            MSP.log("stop()-> VHOST notified");

//        try {
            // ChannelFuture future =
            MSP.log("chainState during stop -> " + chainState.get());
            if (chainState.get() != ChainState.RESTARTING.val) {
                this.nettyFramework.stop(serverChannel, nettyFramework.getDefaultChainQuiesceTimeout());
                chainState.set(ChainState.STOPPED.val);
            }

//            if (!future.isSuccess()) {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "Failed stopping server channel " + serverChannel);
//                }
//            } else {
//                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                    Tr.debug(tc, "Successfully stopped server channel " + serverChannel);
//
//                }
//            }
//            TCPUtils.logChannelStopped(serverChannel);
//        } catch (Exception e) {
//            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                Tr.debug(tc, "Error stopping chain " + this);
//            }
//        } finally {
            MSP.log("Stop() finally block");
            //this.disable();
            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
            postEvent(topic, currentConfig, null);
            currentConfig.clearActivePort();
        }
        //       }
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
        MSP.log("update count: " + updateCount);

        System.out.println("MSP: updating chain: " + resolvedHostName);

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

        boolean sameConfig = newConfig.unchanged(oldConfig);

        MSP.log("chainState for stop condition -> " + chainState.get());

        if (!sameConfig && chainState.get() != ChainState.STOPPED.val) {
            MSP.log("Not same config, restart chain");
            chainState.set(ChainState.RESTARTING.val);
            stop();
            MSP.log("wait for stop");
            //super.stopWait.waitForStop(nettyFramework.getDefaultChainQuiesceTimeout(), this);
            MSP.log("done waiting");
        }

//        } else {
//            Map<Object, Object> chanProps;
//
//            try {
//
//                if (validOldConfig) {
//                    if (sameConfig) {
//                        int state = chainState.get();
//                        if (state == ChainState.STARTED.val) {
//                            // If configurations are identical, see if the listening port is also the same
//                            // which would indicate that the chain is running with the unchanged configuration
//                            // toggle start/stop of chain if we are somehow active on a different port..
//                            sameConfig = oldConfig.validateActivePort();
//                            if (sameConfig) {
//                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                    Tr.debug(this, tc, "Configuration is unchanged, and chain is already started: " + oldConfig);
//                                }
//                                // EARLY EXIT: we have nothing else to do here: "new configuration" not saved
//                                return;
//                            } else {
//                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                    Tr.debug(this, tc, "Configuration is unchanged, but chain is running with a mismatched configuration: " + oldConfig);
//                                }
//                            }
//                        } else if (state == ChainState.QUIESCED.val) {
//                            // Chain is in the process of stopping.. we need to wait for it
//                            // to finish stopping before we start it again
//                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                Tr.debug(this, tc, "Configuration is unchanged, chain is quiescing, wait for stop: " + newConfig);
//                            }
//                            stopWait.waitForStop(nettyFramework.getDefaultChainQuiesceTimeout(), this); // BLOCK
//                        } else {
//                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                Tr.debug(this, tc, "Configuration is unchanged, chain must be started: " + newConfig);
//                            }
//                        }
//                    }
//                }
//            }
//        }

        startNettyChannel();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "update chain " + this);
        }
    }

    public void startNettyChannel() {
        startCount = startCount + 1;
        MSP.log("start count: " + startCount);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "starting netty channel ");

        }
        System.out.println("MSP: start netty channel, ChainState -> " + chainState.toString());

        if (!(chainState.get() == ChainState.STOPPED.val || chainState.get() == ChainState.RESTARTING.val)) {
            MSP.log("Chain already started, returning");
            return;
        }

        MSP.log("startNettyChannel state -> " + chainState.get());

        if (chainState.get() == ChainState.RESTARTING.val) {
            //TODO: clean up dynamic update, only update changed configs, missing TCP/SSL
            // Map<String, Object> httpOptions = new HashMap<String, Object>();
            // owner.getHttpOptions().forEach(httpOptions::putIfAbsent);

            // httpPipeline.updateConfig(ConfigElement.COMPRESSION, this.owner.getCompressionConfig());
            // httpPipeline.updateConfig(ConfigElement.HTTP_OPTIONS, owner.getHttpOptions());
            httpPipeline.updateConfig(ConfigElement.HEADERS, this.owner.getHeadersConfig());
            //  httpPipeline.updateConfig(ConfigElement.REMOTE_IP, owner.getRemoteIpConfig());
            httpPipeline.updateConfig(ConfigElement.SAMESITE, this.owner.getSamesiteConfig());

            VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
            postEvent(topic, currentConfig, null);
            chainState.set(ChainState.STARTED.val);
        } else {

            //TODO: clean up less clogged active configuration
            Map<String, Object> httpOptions = new HashMap<String, Object>();
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
                MSP.log("Put " + ConfigConstants.EXTERNAL_NAME + " with value: " + endpointName);

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
                        }

                    } else {

                        parent.chainState.set(ChainState.STARTED.val);

                        parent.serverChannel = f.channel();
                        VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                        String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
                        postEvent(topic, currentConfig, null);
                    }
                });
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "Problem in starting the chain " + e);
                }
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

        System.out.println("Protocol version found to be: " + protocolVersion);
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
