/*******************************************************************************
 * Copyright (c) 2021, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.local;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.ChannelFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ServerChannel;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;
import io.openliberty.netty.internal.impl.NettyConstants;

public class LocalUtils {

    private static final TraceComponent tc = Tr.register(LocalUtils.class, NettyConstants.NETTY_TRACE_NAME,
            NettyConstants.BASE_BUNDLE);

    /**
     * Create a {@link ServerBootstrapExtended} for local channels that are not
     * based on host address/port addressing
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return
     * @throws NettyException
     */
    @Deprecated
    public static ServerBootstrapExtended createLocalBootstrap(NettyFrameworkImpl framework,
            ChannelInitializerWrapper protocolInitializer, Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, true);

        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(framework.getParentGroup(), framework.getChildGroup());
        bs.channel(LocalServerChannel.class);
        bs.applyConfiguration(config);
        bs.setBaseInitializer(protocolInitializer);

        return bs;
    }

    /**
     * Create a {@link ServerBootstrapExtended} for local channels that are not
     * based on host address/port addressing but can use {@link LocalAddress}
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return the bootstrap to which a child handler can be added to deal with
     *         protocol specific tasks
     * @throws NettyException
     */
    public static ServerBootstrapExtended createLocalBootstrap(NettyFrameworkImpl framework,
            Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, true);

        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(framework.getParentGroup(), framework.getChildGroup());
        bs.channel(LocalServerChannel.class);
        bs.applyConfiguration(config);
        ChannelInitializerWrapper serverInitializer = new LocalChannelInitializerWrapper(config, framework);
        bs.setBaseInitializer(serverInitializer);

        return bs;
    }

    /**
     * Create a {@link BootstrapExtended} for outbound local channels TODO GDH - not
     * found where we will use this from yet in WOLA.
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return
     * @throws NettyException
     */
    @Deprecated
    public static BootstrapExtended createLocalBootstrapOutbound(NettyFrameworkImpl framework,
            ChannelInitializerWrapper protocolInitializer, Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, false);

        BootstrapExtended bs = new BootstrapExtended();
        bs.group(framework.getChildGroup());
        bs.channel(LocalChannel.class);
        bs.applyConfiguration(config);
        bs.setBaseInitializer(protocolInitializer);

        // TODO server concerns other than protocol needs?
        // ChannelInitializerWrapper serverInitializer = new
        // LocalChannelInitializerImpl(config, framework)
        return bs;
    }

    /**
     * Create a {@link BootstrapExtended} for outbound local channels TODO GDH - not
     * found where we will use this from yet in WOLA.
     * 
     * @param framework
     * @param channel   class - this is the class of the channel that is added
     * @param options
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createLocalBootstrapOutbound(NettyFrameworkImpl framework,
            Map<String, Object> options) throws NettyException {

        LocalConfigurationImpl config = new LocalConfigurationImpl(options, false);

        BootstrapExtended bs = new BootstrapExtended();
        bs.group(framework.getChildGroup());
        bs.channel(LocalChannel.class);
        bs.applyConfiguration(config);
        ChannelInitializerWrapper serverInitializer = new LocalChannelInitializerWrapper(config, framework);
        bs.setBaseInitializer(serverInitializer);
        return bs;
    }

    private static ChannelFuture open(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
            final LocalConfigurationImpl config, LocalAddress localAddr, ChannelFutureListener openListener,
            final int retryCount) {

        ChannelFuture oFuture = null;

        if (config.isInbound()) {
            oFuture = ((ServerBootstrapExtended) bootstrap).bind(localAddr);
        } else {
            oFuture = ((BootstrapExtended) bootstrap).connect(localAddr);
        }
        final ChannelFuture openFuture = oFuture;
        if (openListener != null) {
            openFuture.addListener(openListener);
        }

        openFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add new channel to set of active channels, and set a close future to
                // remove it
                // Get parent and increment active connections
                final Channel channel = openFuture.channel();

                // set common channel attrs
                channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
                // TODOchannel.attr(ConfigConstants.LOCAL_ADDR_KEY).set(localAddr);
                channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInbound());

                // Listener to stop channel on close
                // This should just log that the channel stopped
                channel.closeFuture().addListener(innerFuture -> logChannelStopped(channel));

                if (config.isInbound()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding new channel group for " + channel);
                    }
                    framework.getActiveChannelsMap().put(channel,
                            new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
                } else {
                    framework.getOutboundConnections().add(channel);
                }

                // set up a helpful log message
                String hostLogString = "local";
                SocketAddress addr = channel.localAddress();
//	              
//	                    IPvType = "IPv6";
//	                }
//	                if (newHost == NettyConstants.INADDR_ANY) {
//	                    hostLogString = "*  (" + IPvType + ")";
//	                } else {
//	                    hostLogString = config.getHostname() + "  (" + IPvType + ": "
//	                               + inetAddr.getAddress().getHostAddress() + ")";
//	                }
//
//	                if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//	                	Tr.debug(tc, "serverSocket getInetAddress is: " + inetAddr);
//	                    Tr.debug(tc, "serverSocket getLocalSocketAddress is: " + channel.localAddress());
//	                    Tr.debug(tc, "serverSocket getInetAddress hostname is: " + inetAddr.getAddress().getHostName());
//	                    Tr.debug(tc, "serverSocket getInetAddress address is: " + inetAddr.getAddress().getHostAddress());
//	                    Tr.debug(tc, "channelConfig.getHostname() is: " + config.getHostname());
//	                    Tr.debug(tc, "channelConfig.getPort() is: " + config.getPort());
//	                }

//	                if (config.isInbound()) {
//	                    Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
//	                            new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
//	                } else {
//	                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//	                        Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
//	                                new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
//	                    }
//	                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "open failed for " + config.getExternalName() + " due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
//	                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//	                        Tr.debug(tc, "attempt to bind again after a wait of " + timeBetweenRetriesMsec + "ms; "
//	                                + retryCount + " attempts remaining" + " for " + config.getExternalName());
//	                    }
//	                    // recurse until we either complete successfully or run out of retries;
//	                    try {
//	                        Thread.sleep(timeBetweenRetriesMsec);
//	                    } catch (InterruptedException x) {
//	                        // do nothing but debug
//	                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//	                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
//	                        }
//	                    }
//	                    open(framework, bootstrap, config, newHost, inetPort, openListener, retryCount - 1);
                } else {
//	                    if (config.isInbound()) {
//	                        Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
//	                                String.valueOf(inetPort), openFuture.cause().getMessage() });
//	                    } else {
//	                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
//	                            Tr.debug(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(),
//	                                    newHost, String.valueOf(inetPort), openFuture.cause().getMessage() });
//	                        }
//	                    }
                }
            }
        });
        return openFuture;
    }

    private static FutureTask<ChannelFuture> startHelper(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
            LocalConfigurationImpl config, LocalAddress localAddr, ChannelFutureListener openListener)
            throws NettyException {
        if (framework.isStopping()) { // Framework already started and is no longer active
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "server is stopping, channel will not be started");
            }
            return null;
        } else {
            try {
                return framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                    @Override
                    public ChannelFuture call() {
                        return open(framework, bootstrap, config, localAddr, openListener, config.getOpenRetries());
                    }
                });
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NettyFramework signaled- caught exception:: " + e.getMessage());
                }
            }
        }
        return null;
    }

    /**
     * Start an inbound TCP channel
     * 
     * @param framework
     * @param bootstrap
     * @param localAddr
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static FutureTask<ChannelFuture> start(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap,
            LocalAddress localAddr, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (LocalAddr): attempt to bind a channel at LocalAddr: " + localAddr.toString());
        }
        LocalConfigurationImpl config = (LocalConfigurationImpl) bootstrap.getConfiguration();
        return startHelper(framework, bootstrap, config, localAddr, openListener);
    }

    // Use startOutbound or start(...ServerBootstrapExtended)  TODO - not even for child channel?
    @Deprecated 
    public static FutureTask<ChannelFuture> start(NettyFrameworkImpl nettyFrameworkImpl, BootstrapExtended bootstrap,
            LocalAddress localAddr, ChannelFutureListener bindListener) {
        return null;
    }


    /**
     * Start an outbound TCP channel
     * 
     * @param framework
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static FutureTask<ChannelFuture> startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
            LocalAddress localAddr, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startOutbound : attempt to " + localAddr.toString());
        }
        LocalConfigurationImpl config = (LocalConfigurationImpl) bootstrap.getConfiguration();
        return startHelper(framework, bootstrap, config, localAddr, openListener);
    }
    
    /**
     * 
     * @param channel
     */
    // TODO make more sophisticated cf TCP
    public static void logChannelStopped(Channel channel) {
        Tr.info(tc, "stopped" + channel.toString());
    }

    /**
     * 
     * @param channel
     */
    // TODO make more sophisticated cf TCP
    public static void logChannelStarted(Channel channel) {
        Tr.info(tc, "started" + channel.toString());
    }
}
