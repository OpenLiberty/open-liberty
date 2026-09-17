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

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.bootstrap.AbstractBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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
        return (ServerBootstrapExtended)createBootstrap(framework, tcpOptions, true);
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
        return (BootstrapExtended)createBootstrap(framework, tcpOptions, false);
    }

    private static AbstractBootstrap createBootstrap(NettyFrameworkImpl framework, Map<String, Object> tcpOptions, boolean isInbound) throws NettyException {
        TCPConfigurationImpl config = new TCPConfigurationImpl(tcpOptions, isInbound);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config, framework);
        AbstractBootstrap bs;
        EventLoopGroup acceptGroup;
        if (config.getAcceptThread()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "acceptThread: true - using dedicated accept EventLoopGroup for " + config.getExternalName());
            }
            acceptGroup = framework.getDedicatedAcceptGroup();
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "acceptThread: false - using shared accept EventLoopGroup for " + config.getExternalName());
            }
            acceptGroup = framework.getSharedAcceptGroup();
        }
        if (isInbound) {
            bs = new ServerBootstrapExtended()
                .applyConfiguration(config)
                .setBaseInitializer(tcpInitializer)
                .group(acceptGroup, framework.getChildGroup())
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
                                      final int retryCount, final boolean reuseAddrRetry) {
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
            // Bind with SO_REUSEADDR=false first so the OS rejects a conflicting
            // bind (e.g. a specific-host:port when a wildcard:port is already
            // listening).  This mirrors CHFW's TCPPort.finishInitServerSocket()
            // which calls attemptSocketBind(addr, false) and only sets
            // reuseAddress=true after a successful bind.
            // On the TIME_WAIT retry (reuseAddrRetry=true) keep SO_REUSEADDR=true.
            if (!reuseAddrRetry) {
                channel.config().setOption(ChannelOption.SO_REUSEADDR, false);
            }
            oFuture = channel.bind(new InetSocketAddress(inetHost, inetPort));
        } else {
            oFuture = channel.connect(new InetSocketAddress(inetHost, inetPort));
        }
        final ChannelFuture openFuture = oFuture;

        final String newHost = inetHost;

        openFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add new channel to set of active channels, and set a close future to
                // remove it
                // Get parent and increment active connections

                // set common channel attrs
                channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
                channel.attr(ConfigConstants.HOST_KEY).set(newHost);
                channel.attr(ConfigConstants.PORT_KEY).set(inetPort);
                channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInbound());

                // If this channel was assigned a dedicated accept EventLoopGroup, promote it
                // from the pending set to the channel-keyed map now that the channel is known.
                EventLoopGroup acceptGroup = channel.eventLoop().parent();
                if (acceptGroup != null) {
                    framework.registerDedicatedAcceptGroup(channel, acceptGroup);
                }

                // Listener to stop channel on close: log the stop and delegate to
                // framework.stop() so that any dedicated accept EventLoopGroup is
                // also cleaned up via the central stop path.
                channel.closeFuture().addListener(innerFuture -> {
                    logChannelStopped(innerFuture, channel);
                    framework.stop(channel);
                });
                if (config.isInbound()) {
                    // Restore SO_REUSEADDR=true after a successful bind to allow
                    // graceful restart (TIME_WAIT reuse), matching CHFW behavior.
                    channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
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
                    hostLogString = config.getHostname() + "  (" + IPvType + ": "
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
                // Bind succeeded — notify the chain regardless of which retry attempt
                // succeeded.  This is the only place where openListener is invoked for
                // a success outcome; the unconditional attachment outside this callback
                // has been removed to prevent stale intermediate-failure futures from
                // firing the listener prematurely.
                // Use generateOpenListenerWrapper so the callback runs on the Liberty
                // executor thread rather than the Netty I/O thread, matching the
                // original behaviour of the unconditional addListener call below.
                if (openListener != null) {
                    try {
                        generateOpenListenerWrapper(framework, openListener).operationComplete(openFuture);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception dispatching openListener success callback: " + e.getMessage());
                        }
                        openFuture.channel().close();
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc,
                             "open failed for " + config.getExternalName() + " due to: " + future.cause().getMessage());
                }

                // If the framework itself already owns an active channel on this port
                // (e.g. during a config update where the old channel has not yet been
                // released), this is a transient self-conflict.
                // If retries remain, retry silently — the old channel will release shortly.
                // If no retries remain (retryCount==0), this is treated as a permanent
                // conflict (e.g. two endpoints configured for the same port): log the
                // error and notify the chain so startNettyChannel() is unblocked and the
                // state transitions to STOPPED.
                if (config.isInbound() && future.cause() instanceof java.net.BindException
                    && !reuseAddrRetry && frameworkOwnsPort(framework, inetPort)) {

                    Tr.debug(tc, "Bind failed for " + config.getExternalName() + " on port " + inetPort
                                     + " because this framework already owns that port (config update in progress)."
                                     + " retryCount=" + retryCount + ". Retrying silently.");

                    if (retryCount > 0 && channel.isOpen()) {
                        try {
                            Thread.sleep(timeBetweenRetriesMsec);
                        } catch (InterruptedException x) {
                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                        }
                        // Pass the real openListener so the terminal outcome (success or
                        // final failure) notifies NettyChain.channelFutureHandler().
                        // The current failure future cannot invoke it again because we
                        // return immediately after this call.
                        open(framework, channel, config, newHost, inetPort, openListener, retryCount - 1, false);
                        return;
                    }
                    // retryCount == 0: no retries remain and the port is still owned by
                    // this framework.  With no retry loop there is no opportunity to wait
                    // for the old channel to release, so treat this as a terminal failure.
                    // Log the error (matching CHFW's CWWKO0221E behaviour) and invoke the
                    // openListener so channelFutureHandler() runs, transitions state to
                    // STOPPED, and calls notifyAll() to unblock startNettyChannel().
                    Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
                                                                                String.valueOf(inetPort), openFuture.cause().getMessage() });
                    if (openListener != null) {
                        try {
                            generateOpenListenerWrapper(framework, openListener).operationComplete(openFuture);
                        } catch (Exception e) {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                Tr.debug(tc, "Exception dispatching openListener self-conflict terminal callback: " + e.getMessage());
                            }
                        }
                    }
                    return;
                }

                if (retryCount > 0) {
                    if (!channel.isOpen()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Channel not open so it must have been cancelled. Returning...");
                        }
                        return;
                    }
                    // On the very first bind failure, probe the port to distinguish a real
                    // conflict from a TIME_WAIT remnant.  If it is TIME_WAIT only, retry
                    // immediately with SO_REUSEADDR=true rather than burning through all
                    // retries waiting for the OS TIME_WAIT period to expire.
                    if (config.isInbound() && future.cause() instanceof java.net.BindException
                        && !reuseAddrRetry && retryCount == config.getPortOpenRetries()) {
                        String probeHost = newHost.equals(NettyConstants.INADDR_ANY) ? "localhost" : newHost;
                        InetSocketAddress probeAddr = new InetSocketAddress(probeHost, inetPort);
                        if (!probeAddr.isUnresolved()) {
                            try (java.net.Socket probe = new java.net.Socket()) {
                                probe.connect(probeAddr, 1000);
                                // Connection succeeded — someone is actively listening, not TIME_WAIT.
                                // Fall through to the normal retry countdown below.
                                Tr.debug(tc, "Probe connect to " + probeAddr + " succeeded: real port conflict, will retry normally for " + config.getExternalName());

                            } catch (java.io.IOException probeEx) {
                                // Connection refused — TIME_WAIT only, retry immediately with SO_REUSEADDR=true.
                                Tr.debug(tc, "Probe connect to " + probeAddr + " failed (" + probeEx.getMessage()
                                         + "): TIME_WAIT only, retrying bind with SO_REUSEADDR=true for " + config.getExternalName());

                                channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
                                // Pass the real openListener so the terminal outcome
                                // (success or final failure) notifies channelFutureHandler().
                                // The current failure future cannot invoke it again because
                                // we return immediately after this call.
                                open(framework, channel, config, newHost, inetPort, openListener, retryCount - 1, true);
                                return;
                            }
                        }
                    }

                    // config.getPortOpenRetries() + 1 because the initial bind failed, now trying
                    // config.getPortOpenRetries() additional times.
                    Tr.debug(tc, "attempt " + retryCount + " of " + (config.getPortOpenRetries() + 1)
                             + " failed to open the port, will try again after wait interval");

                    // recurse until we either complete successfully or run out of retries;
                    // Pass the real openListener so the terminal outcome (success or final
                    // failure after retries exhausted) notifies channelFutureHandler().
                    // The current failure future cannot invoke it again because we return
                    // immediately after this call.
                    try {
                        Thread.sleep(timeBetweenRetriesMsec);
                    } catch (InterruptedException x) {
                        // do nothing but debug
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "sleep caught InterruptedException.  will proceed.");
                        }
                    }
                    open(framework, channel, config, newHost, inetPort, openListener, retryCount - 1, false);
                    // A retry has been launched — do NOT fall through to the terminal
                    // listener block below.
                    return;
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
                    } else if (config.isInbound() && future.cause() instanceof java.net.BindException
                               && !reuseAddrRetry) {
                        // portOpenRetries == 0: no retries were configured so the first-failure
                        // probe in the retryCount > 0 branch never ran.  Probe now to distinguish
                        // TIME_WAIT from a real conflict before giving up.
                        String probeHost = newHost.equals(NettyConstants.INADDR_ANY) ? "localhost" : newHost;
                        InetSocketAddress probeAddr = new InetSocketAddress(probeHost, inetPort);
                        if (!probeAddr.isUnresolved()) {
                            try (java.net.Socket probe = new java.net.Socket()) {
                                probe.connect(probeAddr, 1000);
                                // Connection succeeded — real conflict, log the error.
                                Tr.debug(tc, "Probe connect to " + probeAddr + " succeeded: real port conflict for " + config.getExternalName());

                                Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
                                                                                            String.valueOf(inetPort), openFuture.cause().getMessage() });
                            } catch (java.io.IOException probeEx) {
                                // Connection refused — TIME_WAIT only, retry with SO_REUSEADDR=true.
                                Tr.debug(tc, "Probe connect to " + probeAddr + " failed (" + probeEx.getMessage()
                                                 + "): TIME_WAIT only, retrying bind with SO_REUSEADDR=true for " + config.getExternalName());

                                channel.config().setOption(ChannelOption.SO_REUSEADDR, true);
                                open(framework, channel, config, newHost, inetPort, openListener, 0, true);
                                return;
                            }
                        } else {
                            // Unresolvable probe address — treat conservatively as a real conflict.
                            Tr.debug(tc, "Probe address " + probeAddr + " is unresolved, treating as real conflict for " + config.getExternalName());

                            Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), newHost,
                                                                                        String.valueOf(inetPort), openFuture.cause().getMessage() });
                        }
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
                // Bind failed and retries are exhausted — notify the chain.
                // For all other failure branches (retryCount > 0 or self-conflict)
                // the listener is intentionally NOT invoked here to avoid firing
                // notifyStopped() on an intermediate failure when a retry is pending.
                // Use generateOpenListenerWrapper so the callback runs on the Liberty
                // executor thread rather than the Netty I/O thread.
                if (openListener != null) {
                    try {
                        generateOpenListenerWrapper(framework, openListener).operationComplete(openFuture);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception dispatching openListener terminal failure callback: " + e.getMessage());
                        }
                    }
                }
            }
        });

        // openListener is now invoked directly inside the future callback at the
        // two terminal points (success and final failure after retries exhausted).
        // It is NOT attached here unconditionally because that would cause
        // intermediate-failure futures to fire notifyStopped() while a retry is
        // still pending — which was the root cause of spurious CWWKT0017I.
        return openFuture;
    }

    private static ChannelFutureListener generateOpenListenerWrapper(NettyFrameworkImpl framework, ChannelFutureListener listener) {
        return new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                framework.getExecutorService().execute(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            listener.operationComplete(future);
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
                if (config.getWaitToAccept()) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Found waitToAccept enabled, channel will be bound even if the server is not completely started.");
                    }
                    open(framework, channel, config, inetHost, inetPort, openListener,
                        config.getPortOpenRetries(), false);
                } else {
                    framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                        @Override
                        public ChannelFuture call() {
                            return open(framework, channel, config, inetHost, inetPort, openListener,
                                        config.getPortOpenRetries(), false);
                        }
                    });
                }
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
     * Returns true if the framework already has an active inbound channel listening
     * on the given port.  When true, a probe connect to localhost would succeed
     * because we ourselves are listening — not an external conflict — so the probe
     * result cannot be used to distinguish TIME_WAIT from a real conflict.
     */
    private static boolean frameworkOwnsPort(NettyFrameworkImpl framework, int port) {
        synchronized (framework.getActiveChannelsMap()) {
            for (Channel activeChannel : framework.getActiveChannelsMap().keySet()) {
                java.net.SocketAddress localAddr = activeChannel.localAddress();
                if (localAddr instanceof java.net.InetSocketAddress) {
                    if (((java.net.InetSocketAddress) localAddr).getPort() == port) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}
