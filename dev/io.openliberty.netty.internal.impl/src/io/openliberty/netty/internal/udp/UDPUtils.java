/*******************************************************************************
 * Copyright (c) 2021, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.udp;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;

public class UDPUtils {

    private static final TraceComponent tc = Tr.register(UDPUtils.class, UDPMessageConstants.NETTY_TRACE_NAME,
                                                         UDPMessageConstants.UDP_BUNDLE);

    /**
     * Create a {@link BootstrapExtended} for inbound UDP channels
     *
     * @param framework
     * @param options
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createUDPBootstrapInbound(NettyFrameworkImpl framework, Map<String, Object> options) throws NettyException {
        return createBootstrap(framework, new UDPConfigurationImpl(options, true));
    }

    /**
     * Create a {@link BootstrapExtended} for outbound channels
     *
     * @param framework
     * @param options
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createUDPBootstrapOutbound(NettyFrameworkImpl framework,
                                                               Map<String, Object> options) throws NettyException {
        return createBootstrap(framework, new UDPConfigurationImpl(options, false));
    }

    private static BootstrapExtended createBootstrap(NettyFrameworkImpl framework, UDPConfigurationImpl config) {
        BootstrapExtended bs = new BootstrapExtended();
        bs.applyConfiguration(config);
        bs.group(framework.getChildGroup());
        bs.channel(NioDatagramChannel.class);
        return bs;
    }

    private static ChannelFuture open(NettyFrameworkImpl framework, BootstrapExtended bootstrap, final Channel channel, String inetHost,
                                      int inetPort, final int retryCount, final int retryDelay, ChannelFutureListener openListener) {
        ChannelFuture openFuture = null;

        final UDPConfigurationImpl config = ((UDPConfigurationImpl) bootstrap.getConfiguration());

        if (config.isInboundChannel()) {
            openFuture = channel.bind(new InetSocketAddress(inetHost, inetPort));
        } else {
            openFuture = channel.connect(new InetSocketAddress(inetHost, inetPort));
        }

        if (inetHost.equals("*")) {
            inetHost = NettyConstants.INADDR_ANY;
        }
        final String newHost = inetHost;

        if (openListener != null) {
            openFuture.addListener(openListener);
        }

        final String channelName = ((UDPConfigurationImpl) bootstrap.getConfiguration()).getExternalName();

        openFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add the new channel to the set of active channels, and set a close future to
                // remove it
                // framework.getActiveChannels().add(channel);

                // set common channel attrs
                channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
                channel.attr(ConfigConstants.HOST_KEY).set(newHost);
                channel.attr(ConfigConstants.PORT_KEY).set(inetPort);
                channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInboundChannel());

                framework.getOutboundConnections().add(channel);

                channel.closeFuture().addListener(innerFuture -> logChannelStopped(channel));

                // set up a helpful log message
                String hostLogString = newHost;
                SocketAddress addr = channel.localAddress();
                InetSocketAddress inetAddr = (InetSocketAddress) addr;
                String IPvType = "IPv4";
                if (inetAddr.getAddress() instanceof Inet6Address) {
                    IPvType = "IPv6";
                }
                if (newHost == NettyConstants.INADDR_ANY) {
                    hostLogString = "*  (" + IPvType + ")";
                } else {
                    hostLogString = config.getHostname() + "  (" + IPvType + ": "
                                    + inetAddr.getAddress().getHostAddress() + ")";
                }

                if (config.isInboundChannel()) {
                    // UDP CWWKO0400I listening message
                    Tr.info(tc, UDPMessageConstants.UDP_CHANNEL_STARTED,
                            new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, UDPMessageConstants.UDP_CHANNEL_STARTED,
                                 new Object[] { config.getExternalName(), hostLogString, String.valueOf(inetPort) });
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "open failed due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
                    if (!channel.isOpen()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Channel not open so must have been cancelled. Returning...");
                        }
                        return;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to open again after a wait of " + retryDelay + "ms; " + retryCount
                                     + " attempts remaining");
                    }
                    // recurse until we either complete successfully or run out of retries;
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException x) {
                        // do nothing but debug
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                        }
                    }
                    open(framework, bootstrap, channel, newHost, inetPort, retryCount - 1, retryDelay, openListener);
                } else {
                    if (!channel.isOpen()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "No retries left and channel is not open so not printing any logs. Returning...");
                        }
                        return;
                    }
                    if (config.isInboundChannel()) {
                        Tr.error(tc, UDPMessageConstants.BIND_FAILURE,
                                 new Object[] { channelName, newHost, String.valueOf(inetPort) });
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, UDPMessageConstants.BIND_FAILURE,
                                     new Object[] { channelName, newHost, String.valueOf(inetPort) });
                        }
                    }
                }
            }
        });
        return openFuture;
    }

    /**
     * Start an outbound UDP channel
     *
     * @param nettyFrameworkImpl
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static Channel startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
                                        String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startOutbound (UDP): attempt to connect to host " + inetHost + " port " + inetPort);
        }
        return startHelper(framework, bootstrap, inetHost, inetPort, openListener);
    }

    /**
     * Start an inbound UDP channel
     *
     * @param framework
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static Channel startInbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap, String inetHost,
                                       int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (UDP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        return startHelper(framework, bootstrap, inetHost, inetPort, openListener);
    }

    private static Channel startHelper(NettyFrameworkImpl framework, BootstrapExtended bootstrap, String inetHost,
                                       int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (framework.isStopping()) { // Framework already started and is no longer active
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "server is stopping, channel will not be started");
            }
            return null;
        }
        try {
            Channel channel;
            if (System.getSecurityManager() == null) {
                channel = bootstrap.register().channel();
            } else {
                channel = AccessController.doPrivileged(
                                                        new PrivilegedAction<ChannelFuture>() {
                                                            @Override
                                                            public ChannelFuture run() {
                                                                return bootstrap.register();
                                                            }
                                                        })
                                .channel();
            }
            framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() throws NettyException {
                    UDPConfigurationImpl config = (UDPConfigurationImpl) bootstrap.getConfiguration();
                    int retryCount = config.getRetryCount();
                    int retryInterval = config.getRetryInterval();
                    InetSocketAddress address = null;
                    String newHost = inetHost;
                    if (newHost.equals("*")) {
                        newHost = NettyConstants.INADDR_ANY;
                    }
                    String hostLogString = newHost == NettyConstants.INADDR_ANY ? "*" : newHost;
                    address = new InetSocketAddress(newHost, inetPort);
                    if (address.isUnresolved()) {
                        final String channelName = ((UDPConfigurationImpl) bootstrap.getConfiguration())
                                        .getExternalName();
                        Tr.error(tc, UDPMessageConstants.DNS_LOOKUP_FAILURE,
                                 new Object[] { channelName, hostLogString, String.valueOf(inetPort) });
                        throw new NettyException("local address unresolved for " + channelName + " - " + hostLogString + ":" + inetPort);
                    }

                    return open(framework, bootstrap, channel, newHost, inetPort, retryCount, retryInterval,
                                openListener);
                }
            });
            return channel;
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "NettyFramework signaled- caught exception:: " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Log a UDP channel stopped message. Inbound channels will log a INFO message,
     * and outbound channels will log DEBUG
     *
     * @param channel
     */
    public static void logChannelStopped(Channel channel) {
        String channelName = channel.attr(ConfigConstants.NAME_KEY).get();
        String host = channel.attr(ConfigConstants.HOST_KEY).get();
        Integer port = channel.attr(ConfigConstants.PORT_KEY).get();
        if (channel.attr(ConfigConstants.IS_INBOUND_KEY).get()) {
            Tr.info(tc, UDPMessageConstants.UDP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, UDPMessageConstants.UDP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        }
    }

}
