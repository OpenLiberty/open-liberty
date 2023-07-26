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

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.internal.HttpChain;
import com.ibm.ws.http.internal.HttpEndpointImpl;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer;
import com.ibm.ws.http.netty.pipeline.HttpPipelineInitializer.ConfigElement;
import com.ibm.wsspi.channelfw.VirtualConnection;
import com.ibm.wsspi.channelfw.VirtualConnectionFactory;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.openliberty.netty.internal.NettyFramework;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.tcp.TCPUtils;

/**
 *
 */
public class NettyChain extends HttpChain {

    private static final TraceComponent tc = Tr.register(NettyChain.class);

    public enum ChainState {
        UNINITIALIZED(0, "UNINITIALIZED"),
        DESTROYED(1, "DESTROYED"),
        INITIALIZED(2, "INITIALIZED"),
        STOPPED(3, "STOPPED"),
        QUIESCED(4, "QUIESCED"),
        STARTED(5, "STARTED");

        final int val;
        final String name;

        @Trivial
        ChainState(int val, String name) {
            this.val = val;
            this.name = "name";
        }
    }

    private NettyFramework nettyFramework;

    private volatile boolean enabled = false;

    ServerBootstrapExtended bootstrap = new ServerBootstrapExtended();
    private Channel serverChannel;

    private final EventLoopGroup parent = new NioEventLoopGroup();
    private final EventLoopGroup child = new NioEventLoopGroup();

    /**
     * Netty Http Chain constructor
     *
     * @param owner
     * @param isHttps
     */
    public NettyChain(HttpEndpointImpl owner, boolean isHttps) {
        super(owner, isHttps);
        bootstrap.group(parent, child);
        bootstrap.channel(NioServerSocketChannel.class);

    }

    public void initNettyChain(String endpointId, Object componentId, NettyFramework netty) {
        final String root = endpointId + (isHttps ? "-ssl" : "");
        nettyFramework = netty;
        this.endpointMgr = nettyFramework.getEndpointManager();

        endpointName = root;
        tcpName = root;
        sslName = "SSL-" + root;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;

    }

    /**
     * Stop this chain. This chain will have to be recreated when the port is updated. Notifications
     * and follow-on of stop operation is in the chainStopped listener method.
     *
     * @return
     */
    @Override
    public void stop() {
        if (serverChannel == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.entry(tc, "Netty channel not initialized, returning from stop");
            }
            return;
        } else {
            //When channel is stopped, remove the previously registered endpoint
            //created in the update
            this.endpointMgr.removeEndPoint(endpointName);
        }

        //We don't have to check enabled/disabled here: chains are always allowed to stop

//        if (currentConfig = null) {
//            return;
//        }

        try {
            ChannelFuture future = this.nettyFramework.stop(serverChannel);

            if (!future.isSuccess()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed stopping server channel " + serverChannel);
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Successfully stopped server channel " + serverChannel);
                }
            }
            TCPUtils.logChannelStopped(serverChannel);
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Error stopping chain " + this);
            }
        } finally {
            this.disable();
        }
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

        final ActiveConfiguration newConfig = new ActiveConfiguration(this.isHttps(), tcpOptions, sslOptions, httpOptions, remoteIpOptions, compressionOptions, samesiteOptions, headersOptions, endpointOptions, resolvedHostName);

        if (newConfig.configPort < 0 || !newConfig.complete()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Stopping chain due to configuration " + newConfig);
            }

            // save the new/changed configuration before we start setting up the new chain
            currentConfig = newConfig;

            stop();
        }

        else {
            Map<Object, Object> chanProps;
            try {
                boolean sameConfig = newConfig.unchanged(oldConfig);
                if (validOldConfig) {

                } else {
                    currentConfig = newConfig;
                }
            } catch (Exception e) {

            }
        }

//            else {
//                Map<Object, Object> chanProps;
//
//                try {
//                    boolean sameConfig = newConfig.unchanged(oldConfig);
//                    if (validOldConfig) {
//                        if (sameConfig) {
//                            int state = chainState.get();
//                            if (state == ChainState.STARTED.val) {
//                                // If configurations are identical, see if the listening port is also the same
//                                // which would indicate that the chain is running with the unchanged configuration
//                                // toggle start/stop of chain if we are somehow active on a different port..
//                                sameConfig = oldConfig.validateActivePort();
//                                if (sameConfig) {
//                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                        Tr.debug(this, tc, "Configuration is unchanged, and chain is already started: " + oldConfig);
//                                    }
//                                    // EARLY EXIT: we have nothing else to do here: "new configuration" not saved
//                                    return;
//                                } else {
//                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                        Tr.debug(this, tc, "Configuration is unchanged, but chain is running with a mismatched configuration: " + oldConfig);
//                                    }
//                                }
//                            } else if (state == ChainState.QUIESCED.val) {
//                                // Chain is in the process of stopping.. we need to wait for it
//                                // to finish stopping before we start it again
//                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                    Tr.debug(this, tc, "Configuration is unchanged, chain is quiescing, wait for stop: " + newConfig);
//                                }
//                                stopWait.waitForStop(nettyFramework.getDefaultChainQuiesceTimeout(), this); // BLOCK
//                            } else {
//                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                                    Tr.debug(this, tc, "Configuration is unchanged, chain must be started: " + newConfig);
//                                }
//                            }
//                        }
//                    }
//
//                    if (!sameConfig) {
//                        // Note that one path in the above block can change the value of sameConfig:
//                        // if the started chain is actually running on a different port than we expect,
//                        // something strange happened, and the whole thing should be stopped and restarted.
//                        // We come through this block for the stop/teardown...
//
//                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//                            Tr.debug(this, tc, "New/changed chain configuration " + newConfig);
//                        }
//
//
//
//                        // We've been through channel configuration before...
//                        // We have to destroy/rebuild the chains because the channels don't
//                        // really support dynamic updates.
//                        ChainData cd = cfw.getChain(chainName);
//                        if (cd != null) {
//                            cfw.stopChain(cd, cfw.getDefaultChainQuiesceTimeout());
//                            stopWait.waitForStop(cfw.getDefaultChainQuiesceTimeout(), this); // BLOCK
//                            cfw.destroyChain(cd);
//                            cfw.removeChain(cd);
//                        }
//                        // Remove any channels that have to be rebuilt..
//                        if (newConfig.tcpChanged(oldConfig))
//                            removeChannel(tcpName);
//
//                        if (newConfig.sslChanged(oldConfig))
//                            removeChannel(sslName);
//
//                        if (newConfig.httpChanged(oldConfig))
//                            removeChannel(httpName);
//
//                        if (newConfig.endpointChanged(oldConfig))
//                            removeChannel(dispatcherName);
//                    }
//
//                    // save the new/changed configuration before we start setting up the new chain
//                    currentConfig = newConfig;
//
//                    // Define and register an EndPoint to represent this chain
//                    EndPointInfo ep = endpointMgr.defineEndPoint(endpointName, newConfig.configHost, newConfig.configPort);
//
//                    // TCP Channel
////                    ChannelData tcpChannel = cfw.getChannel(tcpName);
////                    if (tcpChannel == null) {
////                        String typeName = (String) tcpOptions.get("type");
////                        chanProps = new HashMap<Object, Object>(tcpOptions);
////                        chanProps.put("endPointName", endpointName);
////                        chanProps.put("hostname", ep.getHost());
////                        chanProps.put("port", String.valueOf(ep.getPort()));
//
//                    //    tcpChannel = cfw.addChannel(tcpName, cfw.lookupFactory(typeName), chanProps);
//                    }
//
////                    // SSL Channel
////                    if (isHttps) {
////                        ChannelData sslChannel = cfw.getChannel(sslName);
////                        if (sslChannel == null) {
////                            chanProps = new HashMap<Object, Object>(sslOptions);
////                            // Put the protocol version, which allows the http channel to dynamically
////                            // know what http version it will use.
////                            if (owner.getProtocolVersion() != null) {
////                                chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
////                            }
////                            sslChannel = cfw.addChannel(sslName, cfw.lookupFactory("SSLChannel"), chanProps);
////                        }
////                    }
//
//                    // HTTP Channel
//
//                        chanProps = new HashMap<Object, Object>(httpOptions);
//                        // Put the endpoint id, which allows us to find the registered access log
//                        // dynamically
//                        chanProps.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, owner.getName());
//                        // Put the protocol version, which allows the http channel to dynamically
//                        // know what http version it will use.
//                        if (owner.getProtocolVersion() != null) {
//                            chanProps.put(HttpConfigConstants.PROPNAME_PROTOCOL_VERSION, owner.getProtocolVersion());
//                        }
//                        if (remoteIpOptions.get("id").equals("defaultRemoteIp")) {
//                            //Put the internal remoteIp set to false since the element was not configured to be used
//                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP, "false");
//                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, null);
//                        } else {
//                            chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP, "true");
//                            //Check if the remoteIp is configured to use the remoteIp in the access log or if
//                            //a custom proxy regex was provided
//                            if (remoteIpOptions.containsKey("proxies")) {
//                                chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_PROXIES, remoteIpOptions.get("proxies"));
//                            }
//                            if (remoteIpOptions.containsKey("useRemoteIpInAccessLog")) {
//                                chanProps.put(HttpConfigConstants.PROPNAME_REMOTE_IP_ACCESS_LOG, remoteIpOptions.get("useRemoteIpInAccessLog"));
//                            }
//                        }
//
//                        if (compressionOptions.get("id").equals("defaultCompression")) {
//                            //Put the internal compression set to false since the element was not configured to be used
//                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION, "false");
//                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, null);
//                        }
//
//                        else {
//                            chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION, "true");
//                            //Check if the compression is configured to use content-type filter
//                            if (compressionOptions.containsKey("types")) {
//                                chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_CONTENT_TYPES, compressionOptions.get("types"));
//
//                            }
//                            if (compressionOptions.containsKey("serverPreferredAlgorithm")) {
//                                chanProps.put(HttpConfigConstants.PROPNAME_COMPRESSION_PREFERRED_ALGORITHM, compressionOptions.get("serverPreferredAlgorithm"));
//                            }
//                        }
//
//                        if (samesiteOptions.get("id").equals("defaultSameSite")) {
//                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE, "false");
//                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT, null);
//                        }
//
//                        else {
//
//                            boolean enableSameSite = false;
//                            if (samesiteOptions.containsKey("lax")) {
//                                enableSameSite = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_LAX, samesiteOptions.get("lax"));
//                            }
//                            if (samesiteOptions.containsKey("none")) {
//                                enableSameSite = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_NONE, samesiteOptions.get("none"));
//                            }
//                            if (samesiteOptions.containsKey("strict")) {
//                                enableSameSite = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE_STRICT, samesiteOptions.get("strict"));
//                            }
//                            chanProps.put(HttpConfigConstants.PROPNAME_SAMESITE, enableSameSite);
//                        }
//
//                        if (headersOptions.get("id").equals("defaultHeaders")) {
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, "false");
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, null);
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, null);
//                        }
//
//                        else {
//                            boolean enableHeadersFeature = false;
//                            if (headersOptions.containsKey("add")) {
//                                enableHeadersFeature = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_ADD, headersOptions.get("add"));
//                            }
//                            if (headersOptions.containsKey("set")) {
//                                enableHeadersFeature = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET, headersOptions.get("set"));
//                            }
//                            if (headersOptions.containsKey("setIfMissing")) {
//                                enableHeadersFeature = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_SET_IF_MISSING, headersOptions.get("setIfMissing"));
//                            }
//                            if (headersOptions.containsKey("remove")) {
//                                enableHeadersFeature = true;
//                                chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS_REMOVE, headersOptions.get("remove"));
//                            }
//                            chanProps.put(HttpConfigConstants.PROPNAME_RESPONSE_HEADERS, enableHeadersFeature);
//                        }
//
//
//                    }
//
//                    // HTTPDispatcher Channel
//
//                        chanProps = new HashMap<Object, Object>();
//                        chanProps.put(HttpDispatcherConfig.PROP_ENDPOINT, owner.getPid());
//                    }
//
//                    // Add chain
//                    ChainData cd = cfw.getChain(chainName);
//                    if (null == cd) {
//                        final String[] chanList;
//                        if (isHttps)
//                            chanList = new String[] { tcpName, sslName, httpName, dispatcherName };
//                        else
//                            chanList = new String[] { tcpName, httpName, dispatcherName };
//
//                        cd = cfw.addChain(chainName, FlowType.INBOUND, chanList);
//                        cd.setEnabled(enabled);
//                        cfw.addChainEventListener(this, chainName);
//
//                        // initialize the chain: this will find/create the channels in the chain,
//                        // initialize each channel, and create the chain. If there are issues with any
//                        // channel properties, they will surface here
//                        // THIS INCLUDES ATTEMPTING TO BIND TO THE PORT
//                        cfw.initChain(chainName);
//                    }
//
//                    // We configured the chain successfully
//                    newConfig.validConfiguration = true;
//                } catch (ChannelException e) {
//                    handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
//                } catch (ChainException e) {
//                    handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
//                } catch (Exception e) {
//                    // The exception stack for this is all internals and does not belong in messages.log.
//                    Tr.error(tc, "config.httpChain.error", tcpName, e.toString());
//                    handleStartupError(e, newConfig);
//                }
//
//                if (newConfig.validConfiguration) {
//                    try {
//                        // Start the chain: follow along to chainStarted method (CFW callback)
//                        cfw.startChain(chainName);
//                    } catch (ChannelException e) {
//                        handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
//                    } catch (ChainException e) {
//                        handleStartupError(e, newConfig); // FFDCIgnore: CFW will have logged and FFDCd already
//                    } catch (Exception e) {
//                        // The exception stack for this is all internals and does not belong in messages.log.
//                        Tr.error(tc, "start.httpChain.error", tcpName, e.toString());
//                        handleStartupError(e, newConfig);
//                    }chanProps.put(HttpConfigConstants.PROPNAME_ACCESSLOG_ID, owner.getName());
//                }
        this.startNettyChannel();
    }

    public void startNettyChannel() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "starting netty channel ");

        }
        System.out.println("MSP: start netty channel");

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
            this.bootstrap = nettyFramework.createTCPBootstrap(this.owner.getTcpOptions());

            HttpPipelineInitializer httpPipeline = new HttpPipelineInitializer.HttpPipelineBuilder(this).with(ConfigElement.HTTP_OPTIONS, httpOptions).with(ConfigElement.REMOTE_IP,
                                                                                                                                            this.owner.getRemoteIpConfig()).build();

            if (isHttps) {

            }

            bootstrap.childHandler(httpPipeline);
            NettyChain parent = this;

            nettyFramework.start(bootstrap, info.getHost(), info.getPort(), f -> {
                if (f.isCancelled() || !f.isSuccess()) {

                } else {
                    parent.serverChannel = f.channel();
                }
            });
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
