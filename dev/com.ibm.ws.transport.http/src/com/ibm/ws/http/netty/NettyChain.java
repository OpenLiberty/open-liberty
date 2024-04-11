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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.net.BindException;

import com.ibm.websphere.channelfw.EndPointInfo;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.HttpMessages;
import com.ibm.ws.http.dispatcher.internal.HttpDispatcher;
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
    private ServerBootstrapExtended bootstrap;
    private volatile Channel serverChannel;
    private FutureTask<ChannelFuture> channelFuture;
    private final AtomicReference<ChainState> state = new AtomicReference<>(ChainState.STOPPED);
    private AtomicBoolean cancelToken = new AtomicBoolean(false);

    private int stopCount = 0;
    private int startCount = 0;
    private int updateCount = 0;

    private volatile boolean enabled = false;

    /**
     * Netty Http Chain constructor
     *
     * @param owner
     * @param isHttps
     */
    public NettyChain(HttpEndpointImpl owner, boolean isHttps) {
        super(owner, isHttps);

    }

    public synchronized void initNettyChain(String endpointId,  NettyFramework netty) {

        Objects.requireNonNull(netty, "NettyFramework cannot be null");
        this.nettyFramework = netty;
        endpointMgr = nettyFramework.getEndpointManager();

        final String root = endpointId + (isHttps ? "-ssl" : "");

        endpointName = root;
        tcpName = "TCP-"+root;
        sslName = isHttps ? "SSL-" + root: null;
        httpName = "HTTP-" + root;
        dispatcherName = "HTTPD-" + root;
        chainName = "CHAIN-" + root;


        MSP.log("NettyChain initialized: Endpoint ID = " + endpointId + ", Endpint Name = " + root);

        state.set(ChainState.STOPPED);

        //TODO: Any bootstrap initial settings we want to consider?

    }

    @Override
    public synchronized void stop() {
        stopCount++;
        MSP.log("Attempting to stop NettyChain. Attempt count: " + stopCount + " Current state: "+state.get());
       


        if(state.get() != ChainState.STOPPING) {
            state.set(ChainState.STOPPING);
            cancelToken.set(true);
            
            if(Objects.nonNull(channelFuture)) {
                
                channelFuture.cancel(true);
                channelFuture = null;
            }
                        
            
            try {
                if(Objects.nonNull(serverChannel) && serverChannel.isOpen()) {
                    
                    MSP.log("STOP -> serverChannel is open, attempting to close");
                    
                    nettyFramework.stop(serverChannel, -1);
                    serverChannel = null;
                }

            }finally {
                endpointMgr.removeEndPoint(endpointName);
                VirtualHostMap.notifyStopped(owner, currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                MSP.log("stop()-> VHOST notified");
                currentConfig.clearActivePort();
                String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STOPPED;
                postEvent(topic, currentConfig, null);
                state.set(ChainState.STOPPED);
                notifyAll();
            }
        } else {
            MSP.log("NETTY CHAIN ERROR (STOP) - NettyChain is not in a stoppable state. Current state: " + state.get());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.exit(this, tc, "stop chain " + this);
        }
    }
    
    private void stopAndWait() {
        if(state.get() != ChainState.STOPPED) {
            stop();
            while(state.get() != ChainState.STOPPED) {
                try {
                    wait();
                } catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public synchronized void update(String resolvedHostName) {

        updateCount++;
        
        
        MSP.log("Update count: " + updateCount + "Current state: " + state.get());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.entry(this, tc, "update chain " + this);
        }

        if(!enabled || FrameworkState.isStopping()) {
            MSP.log("Chain is disabled or framework is stopping, skipping update.");
            return;
        }




        if(configurationsDiffer(resolvedHostName)) {
            //Cancel ongoing channelFuture if necessary
//            if(Objects.nonNull(channelFuture)) {
//                cancelToken.set(true);
//                channelFuture.cancel(true);
//
//            }
//
//            //Ensure the channel is fully stopped if its already started or being started
//            if(Objects.nonNull(serverChannel) && serverChannel.isActive()) {
                stopAndWait();
          //  }

            
            startNettyChannel();
            MSP.log("Channel restarted with new configuration.");
        }
    }

    private boolean configurationsDiffer(String resolvedHostName) {
        final ActiveConfiguration oldConfig = currentConfig;
        final ActiveConfiguration newConfig;
        boolean result = false;

        //The old configuration was valid if it existed and was correctly configured
        final boolean validOldConfig = (Objects.isNull(oldConfig)) ? false : oldConfig.validConfiguration;

        newConfig = new ActiveConfiguration(isHttps(), 
                                            getOwner().getTcpOptions(),
                                            isHttps() ? getOwner().getSslOptions(): null,
                                                            getOwner().getHttpOptions(),
                                                            getOwner().getRemoteIpConfig(),
                                                            getOwner().getCompressionConfig(),
                                                            getOwner().getSamesiteConfig(),
                                                            getOwner().getHeadersConfig(),
                                                            getOwner().getEndpointOptions(),
                                                            resolvedHostName);

        if (newConfig.configPort > 0 && newConfig.complete() && !newConfig.unchanged(oldConfig)) {
            MSP.log("This configuration differs and should cause an update");
            result = true;
            currentConfig = newConfig;
        }

        return result;


    }

    public synchronized void startNettyChannel() {

        startCount++;
        MSP.log("Starting NettyChannel. Attempt count: " + startCount);

        if(state.get() != ChainState.STOPPED) {
            MSP.log("NettyChain is not in STOPPED state. Current state: " + state.get());
            
               
                stopAndWait();
                
                
            
        }
        
        if(state.compareAndSet(ChainState.STOPPED, ChainState.STARTING)) {

            try {
                
                MSP.log("State should ALWAYS be STARTING here: " + state.get());
    
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
    
                EndPointInfo info = endpointMgr.getEndPoint(this.endpointName);
                info = endpointMgr.defineEndPoint(this.endpointName, currentConfig.configHost, currentConfig.configPort);
    
                Map<String, Object> tcpOptions = new HashMap<String, Object>();
                MSP.log("Put " + ConfigConstants.EXTERNAL_NAME + " with value: " + endpointName);
    
                this.getOwner().getTcpOptions().forEach(tcpOptions::putIfAbsent);
                tcpOptions.put(ConfigConstants.EXTERNAL_NAME, endpointName);
    
    
                bootstrap = nettyFramework.createTCPBootstrap(tcpOptions);
                HttpPipelineInitializer httpPipeline = new HttpPipelineInitializer.HttpPipelineBuilder(this)
                                .with(ConfigElement.COMPRESSION, owner.getCompressionConfig())
                                .with(ConfigElement.HTTP_OPTIONS, httpOptions)
                                .with(ConfigElement.HEADERS, owner.getHeadersConfig())
                                .with(ConfigElement.REMOTE_IP, owner.getRemoteIpConfig())
                                .with(ConfigElement.SAMESITE, owner.getSamesiteConfig()).build();
    
                bootstrap.childHandler(httpPipeline);
                
                cancelToken.set(false);
    
                channelFuture = nettyFramework.start(bootstrap, info.getHost(), info.getPort(), this::channelFutureHandler, cancelToken);
                
                VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
                String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
                postEvent(topic, currentConfig, null);
    
    
                //channelFuture.get(10, TimeUnit.SECONDS);
    
    
            } catch (Exception e) {
                MSP.log("Failed to start NettyChannel: " +e.getMessage());
                cancelToken.set(true);
                state.set(ChainState.STOPPED);
            } finally {
                notifyAll();
            }
        }

    }

    private void channelFutureHandler(ChannelFuture future) {
        if(future.isSuccess()) {
            serverChannel = future.channel();
            state.set(ChainState.STARTED);

            MSP.log("Channel is now active and listening on port " + getActivePort());
//            VirtualHostMap.notifyStarted(owner, () -> currentConfig.getResolvedHost(), currentConfig.getConfigPort(), isHttps);
//            String topic = owner.getEventTopic() + HttpServiceConstants.ENDPOINT_STARTED;
//            postEvent(topic, currentConfig, null);

//            state.set(ChainState.STARTED);

        } else {
            MSP.log("ChannelFutureHandler -> Failed to bind to port: " + future.cause());
            stopAndWait();
        }
    }
    
    @Override
    public void enable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "enable chain " + this);
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
