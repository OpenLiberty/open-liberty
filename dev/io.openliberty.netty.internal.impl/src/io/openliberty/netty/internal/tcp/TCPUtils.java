/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.tcp;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.DefaultChannelGroup;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.BootstrapExtended;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;

public class TCPUtils {

    private static final TraceComponent tc = Tr.register(TCPUtils.class, new String[] { TCPMessageConstants.TCP_TRACE_NAME, TCPMessageConstants.NETTY_TRACE_NAME },
                                                         TCPMessageConstants.TCP_BUNDLE, TCPUtils.class.getName());
    private static final int timeBetweenRetriesMsec = 1000; // make this non-configurable

    /**
     * Create a {@link ServerBootstrapExtended} for inbound TCP channels
     *
     * @param framework
     * @param tcpOptions
     * @return
     * @throws NettyException
     */
    public static ServerBootstrapExtended createTCPBootstrapInbound(NettyFrameworkImpl framework,
                                                                    Map<String, Object> tcpOptions) throws NettyException {
        return (ServerBootstrapExtended) createBootstrap(framework, tcpOptions, true);
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
        return (BootstrapExtended) createBootstrap(framework, tcpOptions, false);
    }

    private static AbstractBootstrap createBootstrap(NettyFrameworkImpl framework, Map<String, Object> tcpOptions, boolean isInbound) throws NettyException {
        BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, isInbound);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config, framework);
        AbstractBootstrap bs;
        if (isInbound) {
            bs = new ServerBootstrapExtended()
                            .applyConfiguration(config)
                            .setBaseInitializer(tcpInitializer)
                            .group(framework.getParentGroup(), framework.getChildGroup())
                            .channel(framework.getServerSocketChannelClass());
        } else {
            bs = new BootstrapExtended()
                            .applyConfiguration(config)
                            .setBaseInitializer(tcpInitializer)
                            .group(framework.getChildGroup())
                            .channel(framework.getSocketChannelClass());
        }
        return bs;
    }

    private static ChannelFuture open(NettyFrameworkImpl framework, final Channel channel,
                                      final TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener,
                                      final int retryCount) {
        if (!channel.isOpen()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel not started because it was closed: " + channel);
            }
            return null;
        }
        ChannelFuture oFuture = null;
        if (inetHost.equals("*")) {
            inetHost = NettyConstants.INADDR_ANY;
        }
        if (config.isInbound()) {
            oFuture = channel.bind(new InetSocketAddress(inetHost, inetPort));
        } else {
            oFuture = channel.connect(new InetSocketAddress(inetHost, inetPort));
        }
        final ChannelFuture openFuture = oFuture;

        final String newHost = inetHost;
        
        // Tracks whether the wildcard-conflict check vetoed an otherwise-successful bind.
        // Set to true inside the openFuture listener; read by the openListener wrapper so
        // that NettyChain.channelFutureHandler() is driven as a failure rather than a
        // success — this is mirroring the legacy TCPChannel.takeDownChain() behavior.
        final AtomicBoolean wildcardConflict = new AtomicBoolean(false);

        openFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add new channel to set of active channels, and set a close future to
                // remove it
                // Get parent and increment active connections

                if (config.isInbound() && !NettyConstants.INADDR_ANY.equals(newHost)) {
                    InetAddress boundAddr = ((InetSocketAddress) channel.localAddress()).getAddress();
                    if (!boundAddr.isLoopbackAddress() && wildcardListenerDetected(inetPort)) {
                        // A wildcard listener already owns this port. Close our channel and report the error as a bind conflict.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Specific-address bind for " + newHost + ":" + inetPort
                                         + " conflicts with an existing wildcard listener on the same port. Closing channel.");
                        }
                        channel.close();
                        Tr.error(tc, TCPMessageConstants.BIND_ERROR,
                                 new Object[] { config.getExternalName(), newHost,
                                                String.valueOf(inetPort), "Address already in use" });
                        return;
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "No conflicting wildcard listener found for port " + inetPort + "; bind of " + newHost + " is valid.");
                        }
                    }
                }

                // set common channel attrs
                channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
                channel.attr(ConfigConstants.HOST_KEY).set(newHost);
                channel.attr(ConfigConstants.PORT_KEY).set(inetPort);
                channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInbound());

                // Listener to stop channel on close
                // This should just log that the channel stopped
                channel.closeFuture().addListener(innerFuture -> logChannelStopped(innerFuture, channel));

                if (config.isInbound()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Adding new channel group for " + channel);
                    }
                    synchronized (framework.getActiveChannelsMap()) {
                        framework.getActiveChannelsMap().put(channel, new DefaultChannelGroup(framework.getChildGroup().next()));
                    }
                } else {
                    synchronized (framework.getOutboundConnections()) {
                        framework.getOutboundConnections().add(channel);
                    }
                }
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
                    hostLogString = newHost + "  (" + IPvType + ": "
                                    + inetAddr.getAddress().getHostAddress() + ")";
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "serverSocket getInetAddress is: " + inetAddr);
                    Tr.debug(tc, "serverSocket getLocalSocketAddress is: " + channel.localAddress());
                    Tr.debug(tc, "serverSocket getInetAddress hostname is: " + inetAddr.getAddress().getHostName());
                    Tr.debug(tc, "serverSocket getInetAddress address is: " + inetAddr.getAddress().getHostAddress());
                    Tr.debug(tc, "channelConfig.getHostname() is: " + config.getHostname());
                    Tr.debug(tc, "channelConfig.getPort() is: " + config.getPort());
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
                    if (!channel.isOpen()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Channel not open so it must have been cancelled. Returning...");
                        }
                        return;
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        // config.getPortOpenRetries() + 1 because the initial bind failed, now trying
                        // config.getPortOpenRetries() additional times.
                        Tr.debug(tc, "attempt " + retryCount + " of " + (config.getPortOpenRetries() + 1)
                                     + " failed to open the port, will try again after wait interval");
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
                    open(framework, channel, config, newHost, inetPort, openListener, retryCount - 1);
                } else {
                    if (!channel.isOpen()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "No retries left and channel is not open so not printing any logs. Returning...");
                        }
                        return;
                    }

                    // Check if the exception is or was caused by UnresolvedAddressException
                    Throwable cause = future.cause();
                    boolean unresolvedAddress = false;

                    while (cause != null) {
                        if (cause instanceof java.nio.channels.UnresolvedAddressException) {
                            unresolvedAddress = true;
                            break;
                        }
                        cause = cause.getCause();
                    }
                    if (unresolvedAddress) {
                        // Log the specific error message
                        Tr.error(tc, TCPMessageConstants.LOCAL_HOST_UNRESOLVED,
                                 new Object[] { config.getExternalName(), newHost, String.valueOf(inetPort) });
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
            }
        });

        if (openListener != null) {
            openFuture.addListener(generateOpenListenerWrapper(framework, openListener, wildcardConflict));
        }
        return openFuture;
    }

    private static ChannelFutureListener generateOpenListenerWrapper(NettyFrameworkImpl framework, ChannelFutureListener listener, AtomicBoolean wildcardConflict) {
        return new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                // If a wildcard-conflict was detected in the open listener, substitute a
                // failed future so that NettyChain.channelFutureHandler() follows the
                // failure path (handleStartupError + state transition), mirroring the
                // legacy TCPChannel.takeDownChain() behavior.
                final ChannelFuture effectiveFuture = wildcardConflict.get() ? future.channel().newFailedFuture(new IOException("Address already in use")) : future;
                
                framework.getExecutorService().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            listener.operationComplete(effectiveFuture);
                        } catch (Exception e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Exception caught running open listener!! Closing channel just in case");
                            }
                            future.channel().close();
                        }
                    }
                });
            }
        };
    }

    private static Channel startHelper(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
                                       TCPConfigurationImpl config, String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
        if (framework.isStopping()) { // Framework already stopping and is no longer active
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "server is stopping, channel will not be started");
            }
            return null;
        } else {
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
                    public ChannelFuture call() {
                        return open(framework, channel, config, inetHost, inetPort, openListener,
                                    config.getPortOpenRetries());
                    }
                });
                return channel;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "NettyFramework signaled- caught exception: " + e.getMessage());
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
     * @param inetHost
     * @param inetPort
     * @param openListener
     * @return
     * @throws NettyException
     */
    public static Channel startInbound(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap, String inetHost,
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
    public static Channel startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
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
        String channelName = channel.attr(ConfigConstants.NAME_KEY).get();
        String host = channel.attr(ConfigConstants.HOST_KEY).get();
        Integer port = channel.attr(ConfigConstants.PORT_KEY).get();
        Boolean inbound = channel.attr(ConfigConstants.IS_INBOUND_KEY).get();
        if (inbound != null && inbound) {
            Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        } else if (inbound != null && !inbound) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Socket channel closed, local: " + channel.localAddress() + " remote: " + channel.remoteAddress());
        }
    }

    /**
     * Overrides method above to also log the state of the future.
     *
     * @param channel
     */
    public static void logChannelStopped(Future<?> future, Channel channel) {
        logChannelStopped(channel);
        boolean completed = future.isDone() && !future.isCancelled();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Channel stop future: done and not cancelled --> {0} for port {1}",
                     completed, String.valueOf(channel.attr(ConfigConstants.PORT_KEY).get()));
        }
    }

    /**
     * Log a TCP channel started message. Inbound channels will log a INFO message,
     * and outbound channels will log DEBUG
     *
     * @param channel
     */
    public static void logChannelStarted(Channel channel) {
        String channelName = channel.attr(ConfigConstants.NAME_KEY).get();
        String host = channel.attr(ConfigConstants.HOST_KEY).get();
        Integer port = channel.attr(ConfigConstants.PORT_KEY).get();
        if (channel.attr(ConfigConstants.IS_INBOUND_KEY).get()) {
            Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
                    new Object[] { channelName, host, String.valueOf(port) });
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, TCPMessageConstants.TCP_CHANNEL_STARTED,
                     new Object[] { channelName, host, String.valueOf(port) });
        }
    }

    /**
     * Probes whether any wildcard listener (IPv4 or IPv6) is already accepting
     * connections on the given port. Used to detect the case where SO_REUSEADDR=true
     * has allowed a specific-address bind to succeed even though a wildcard socket
     * already owns the port — a condition the legacy TCPPort would have rejected.
     *
     * A successful connection on either indicates a wildcard listener is present.
     *
     * @param inetPort the port to probe
     * @return true if a wildcard listener is detected on that port; false otherwise
     */
    private static boolean wildcardListenerDetected(int inetPort) {
        if (canConnect("127.0.0.1", inetPort)) {
            return true;
        }

        if (canConnect("::1", inetPort)) {
            return true;
        }
        return false;
    }

    /**
     * Attempts a connection to the given host and port.
     *
     * @return true if the connection was accepted; false if refused or the address
     *         is not available on this host
     */
    private static boolean canConnect(String host, int port) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            SocketChannel testChannel = SocketChannel.open(new InetSocketAddress(addr, port));
            testChannel.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
