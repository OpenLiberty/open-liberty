/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.netty.internal.impl;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.Callable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.TCPMessageConstants;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;

public class TCPUtils {

	private static final TraceComponent tc = Tr.register(NettyFrameworkImpl.class, TCPMessageConstants.NETTY_TRACE_NAME,
            TCPMessageConstants.TCP_BUNDLE);
    private static final int timeBetweenRetriesMsec = 1000; // make this non-configurable

    /**
     * Create a {@link ServerBootstrapExtended} for inbound TCP channels
     * 
     * @param framework
     * @param tcpOptions
     * @return
     * @throws NettyException
     */
    public static ServerBootstrapExtended createTCPBootstrap(NettyFrameworkImpl framework,
            Map<String, Object> tcpOptions) throws NettyException {
        BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, true);
        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(framework.getParentGroup(), framework.getChildGroup());
        bs.channel(NioServerSocketChannel.class);
        // apply the existing user config to the Netty TCP channel
        bs.applyConfiguration(config);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config);
        bs.setBaseInitializer(tcpInitializer);
        return bs;
    }

    /**
     * Create a {@link BootstrapExtended} for outbound TCP channels
     * 
     * @param framework
     * @param tcpOptions
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createTCPBootstrapOutbound(NettyFrameworkImpl framework,
            Map<String, Object> tcpOptions) throws NettyException {
        BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, false);
        BootstrapExtended bs = new BootstrapExtended();
        bs.group(framework.getParentGroup());
        bs.channel(NioSocketChannel.class);
        // apply the existing user config to the Netty TCP channel
        bs.applyConfiguration(config);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config);
        bs.setBaseInitializer(tcpInitializer);
        return bs;
    }

    private static ChannelFuture open(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
            final TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener,
            final int retryCount) {

        ChannelFuture oFuture = null;
        if (inetHost.equals("*")) {
            inetHost = "0.0.0.0";
        }
        if (config.isInbound()) {
            oFuture = ((ServerBootstrapExtended) bootstrap).bind(inetHost, inetPort);
        } else {
            oFuture = ((BootstrapExtended) bootstrap).connect(inetHost, inetPort);
        }
        final ChannelFuture openFuture = oFuture;
        if (openListener != null) {
            openFuture.addListener(openListener);
        }
        final String newHost = inetHost;

        openFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add the new channel to the set of active channels, and set a close future to
                // remove it
                final Channel channel = openFuture.channel();
                framework.getActiveChannels().add(channel);
                channel.closeFuture().addListener(innerFuture -> framework.stop(channel));

                // set common channel attrs
                channel.attr(ConfigConstants.NameKey).set(config.getExternalName());
                channel.attr(ConfigConstants.HostKey).set(newHost);
                channel.attr(ConfigConstants.PortKey).set(inetPort);
                channel.attr(ConfigConstants.IsInboundKey).set(config.isInbound());

                // set up the a helpful log message
                String hostLogString = newHost == "0.0.0.0" ? "*" : newHost;
                SocketAddress addr = channel.localAddress();
                InetSocketAddress inetAddr = (InetSocketAddress)addr;
                String IPvType = "IPv4";
                if (inetAddr.getAddress() instanceof Inet6Address) {
                    IPvType = "IPv6";
                }
                if (newHost == "0.0.0.0") {
                    hostLogString = "*  (" + IPvType + ")";
                } else {
                    hostLogString = config.getHostname() + "  (" + IPvType + ": "
                               + inetAddr.getAddress().getHostAddress() + ")";
                }

                if (config.isInbound()) {
                    Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
                            new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
                                new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                            "open failed for " + config.getExternalName() + " due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to bind again after a wait of " + timeBetweenRetriesMsec + "ms; "
                                + retryCount + " attempts remaining" + " for " + config.getExternalName());
                    }
                    // recurse until we either complete successfully or run out of retries;
                    try {
                        Thread.sleep(timeBetweenRetriesMsec);
                    } catch (InterruptedException x) {
                        // do nothing but debug
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                        }
                    }
                    open(framework, bootstrap, config, newHost, inetPort, openListener, retryCount - 1);
                } else {
                    if (config.isInbound()) {
                        Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
                                String.valueOf(inetPort), openFuture.cause().getMessage() });
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(),
                                    newHost, String.valueOf(inetPort), openFuture.cause().getMessage() });
                        }
                    }
                }
            }
        });
        return openFuture;
    }

    private static ChannelFuture startHelper(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
            TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener)
            throws NettyException {
        try {
            framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() {
                    return open(framework, bootstrap, config, inetHost, inetPort, openListener,
                            config.getPortOpenRetries());
                }
            });
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "caught exception performing late cycle server startup task: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Start an inbound TCP channel
     * 
     * @param framework
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static ChannelFuture start(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap, String inetHost,
            int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (TCP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
        return startHelper(framework, bootstrap, config, inetHost, inetPort, openListener);
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
    public static ChannelFuture startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
            String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startOutbound (TCP): attempt to connect to host " + inetHost + " port " + inetPort);
        }
        TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
        return startHelper(framework, bootstrap, config, inetHost, inetPort, openListener);
    }

    /**
     * Log a TCP channel stopped message. Inbound channels will log a INFO message,
     * and outbound channels will log DEBUG
     * 
     * @param channel
     */
    public static void logChannelStopped(Channel channel) {
        String channelName = channel.attr(ConfigConstants.NameKey).get();
        String host = channel.attr(ConfigConstants.HostKey).get();
        Integer port = channel.attr(ConfigConstants.PortKey).get();
        if (channel.attr(ConfigConstants.IsInboundKey).get()) {
            Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        }
    }

}
