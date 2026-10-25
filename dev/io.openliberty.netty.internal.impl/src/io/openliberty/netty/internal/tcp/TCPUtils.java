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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

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

    /**
     * Perform a single bind (inbound) or connect (outbound) attempt on the given channel,
     * blocking the calling thread until the operation completes.
     *
     * This method MUST be called from a non-Netty-I/O thread (e.g. Liberty executor or
     * SCR thread).  Calling it from a Netty I/O thread would deadlock because
     * ChannelFuture.await() waits for the I/O thread to complete the operation.
     *
     * On success the channel is registered into the framework's active-channels map and
     * the caller-supplied openListener is invoked (via generateOpenListenerWrapper so it
     * runs on a Liberty executor thread, matching the pre-existing contract).
     *
     * On failure the method returns the failed future; the caller is responsible for
     * logging the error and invoking openListener with the terminal failure future.
     *
     * @return the completed ChannelFuture, or null if the channel was already closed
     */
    private static ChannelFuture bindOrConnect(NettyFrameworkImpl framework, final Channel channel,
                                               final TCPConfigurationImpl config, final String inetHost,
                                               final int inetPort, final boolean allowReuseAddr) throws InterruptedException {
        if (!channel.isOpen()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Channel not started because it was closed: " + channel);
            }
            return null;
        }

        ChannelFuture future;
        if (config.isInbound()) {
            // First attempt: bind with SO_REUSEADDR=false so the OS rejects a real conflict
            // (e.g. a specific-host:port when a wildcard:port is already listening).
            // This mirrors CHFW's TCPPort.finishInitServerSocket() which also binds with
            // reuseAddress=false first.
            //
            // Retry attempts (allowReuseAddr=true): bind with SO_REUSEADDR=true so the OS
            // allows binding over a TIME_WAIT socket left from a previous server run.
            // The OS still rejects the bind if something is actively listening, so this
            // correctly distinguishes TIME_WAIT (succeeds) from a real conflict (fails again).
            channel.config().setOption(ChannelOption.SO_REUSEADDR, allowReuseAddr);
            future = channel.bind(new InetSocketAddress(inetHost, inetPort));
        } else {
            future = channel.connect(new InetSocketAddress(inetHost, inetPort));
        }

        // await() blocks this (non-I/O) thread until the I/O thread completes the
        // operation and signals the future.  The I/O thread is never blocked.
        future.await();
        return future;
    }

    /**
     * Register a successfully bound/connected channel into the framework and log the
     * started message.  Called after a successful bindOrConnect().
     */
    private static void registerSuccess(NettyFrameworkImpl framework, final Channel channel,
                                        final TCPConfigurationImpl config, final String inetHost,
                                        final int inetPort) {
        // Restore the configured SO_REUSEADDR value now that the bind succeeded, so
        // that graceful restart (TIME_WAIT reuse) works when the option is enabled.
        if (config.isInbound()) {
            channel.config().setOption(ChannelOption.SO_REUSEADDR, config.getSoReuseAddress());
        }

        channel.attr(ConfigConstants.NAME_KEY).set(config.getExternalName());
        channel.attr(ConfigConstants.HOST_KEY).set(inetHost);
        channel.attr(ConfigConstants.PORT_KEY).set(inetPort);
        channel.attr(ConfigConstants.IS_INBOUND_KEY).set(config.isInbound());

        // If this channel was assigned a dedicated accept EventLoopGroup, promote it
        // from the pending set to the channel-keyed map now that the channel is known.
        EventLoopGroup acceptGroup = channel.eventLoop().parent();
        if (acceptGroup != null) {
            framework.registerDedicatedAcceptGroup(channel, acceptGroup);
        }

        channel.closeFuture().addListener(innerFuture -> {
            logChannelStopped(innerFuture, channel);
            framework.stop(channel);
        });

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

        SocketAddress addr = channel.localAddress();
        InetSocketAddress inetAddr = (InetSocketAddress) addr;
        String IPvType = (inetAddr.getAddress() instanceof Inet6Address) ? "IPv6" : "IPv4";
        String hostLogString;
        if (inetHost == NettyConstants.INADDR_ANY) {
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
    }

    /**
     * Bind/connect with retry loop, blocking the calling thread until the terminal
     * outcome (success or all retries exhausted).
     *
     * Must be called from a non-Netty-I/O thread.  Retries sleep on this thread;
     * the I/O thread is only active during the brief bind/connect operation itself.
     *
     * On success registers the channel and invokes openListener.
     * On terminal failure logs the error and invokes openListener with the failed future.
     *
     * @return the terminal ChannelFuture, or null if the channel was closed before binding
     */
    @FFDCIgnore(IOException.class)
    private static ChannelFuture open(NettyFrameworkImpl framework, final Channel channel,
                                      final TCPConfigurationImpl config, final String inetHost,
                                      final int inetPort, final ChannelFutureListener openListener,
                                      final int retryCount) {
        String resolvedHost = inetHost.equals("*") ? NettyConstants.INADDR_ANY : inetHost;
        int retriesLeft = retryCount;

        while (true) {
            // Always start with SO_REUSEADDR=false so the OS rejects a real conflict
            // (e.g. specific-host:port when a wildcard is already listening).
            ChannelFuture future;
            try {
                future = bindOrConnect(framework, channel, config, resolvedHost, inetPort, false);
            } catch (InterruptedException ie) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "open interrupted for " + config.getExternalName());
                }
                Thread.currentThread().interrupt();
                return null;
            }

            if (future == null) {
                // channel was closed before the bind started
                return null;
            }

            if (future.isSuccess()) {
                registerSuccess(framework, channel, config, resolvedHost, inetPort);
                if (openListener != null) {
                    try {
                        generateOpenListenerWrapper(framework, openListener).operationComplete(future);
                    } catch (Exception e) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exception dispatching openListener success callback: " + e.getMessage());
                        }
                        channel.close();
                    }
                }
                return future;
            }

            // Bind/connect failed
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "open failed for " + config.getExternalName()
                         + " due to: " + future.cause().getMessage());
            }

            // If soReuseAddress is enabled (the default) and this was a BindException,
            // use a probe-socket to distinguish TIME_WAIT from a real active-listener
            // conflict — mirroring CHFW's TCPPort.finishInitServerSocket() pattern:
            //
            //   1. Attempt an outbound connection to the same host:port.
            //      - Succeeds  → someone is actively listening  → real conflict,
            //                    skip SO_REUSEADDR=true, fall through to retry loop.
            //      - Fails (connection refused / IOException) → only TIME_WAIT sockets
            //                    remain → safe to retry with SO_REUSEADDR=true.
            //   2. Retry with SO_REUSEADDR=true only in the TIME_WAIT case.
            if (config.isInbound() && config.getSoReuseAddress()
                && future.cause() instanceof java.net.BindException
                && channel.isOpen()) {

                boolean activeListenerDetected = false;
                String probeHost = config.getHostname();
                if (probeHost == null || probeHost.equals("*")) {
                    probeHost = "localhost";
                }
                InetSocketAddress probeAddr = new InetSocketAddress(probeHost, inetPort);
                if (!probeAddr.isUnresolved()) {
                    try {
                        SocketChannel probe = SocketChannel.open(probeAddr);
                        // Connection succeeded — someone is actively listening.
                        probe.close();
                        activeListenerDetected = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Probe connection to " + probeHost + ":" + inetPort
                                     + " succeeded — real port conflict for " + config.getExternalName());
                        }
                    } catch (IOException probeIoe) {
                        // Connection refused — only TIME_WAIT sockets present, no active listener.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Probe connection refused on " + probeHost + ":" + inetPort
                                     + " — TIME_WAIT only, retrying with SO_REUSEADDR=true for "
                                     + config.getExternalName());
                        }
                    }
                }

                if (!activeListenerDetected) {
                    try {
                        future = bindOrConnect(framework, channel, config, resolvedHost, inetPort, true);
                    } catch (InterruptedException ie) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "open interrupted (reuseAddr retry) for " + config.getExternalName());
                        }
                        Thread.currentThread().interrupt();
                        return null;
                    }
                    if (future == null) {
                        return null;
                    }
                    if (future.isSuccess()) {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Bind succeeded with SO_REUSEADDR=true (TIME_WAIT reuse) for " + config.getExternalName());
                        }
                        registerSuccess(framework, channel, config, resolvedHost, inetPort);
                        if (openListener != null) {
                            try {
                                generateOpenListenerWrapper(framework, openListener).operationComplete(future);
                            } catch (Exception e) {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                                    Tr.debug(tc, "Exception dispatching openListener success callback (reuseAddr): " + e.getMessage());
                                }
                                channel.close();
                            }
                        }
                        return future;
                    }
                    // SO_REUSEADDR=true failed even though probe said no active listener.
                    // Unusual — fall through to the portOpenRetries loop.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Bind failed with SO_REUSEADDR=true after probe for " + config.getExternalName()
                                 + ": " + future.cause().getMessage());
                    }
                }
            }

            if (retriesLeft > 0 && channel.isOpen()) {
                Tr.debug(tc, "attempt " + retriesLeft + " of " + (retryCount + 1)
                         + " failed to open the port, will try again after wait interval for "
                         + config.getExternalName());
                try {
                    Thread.sleep(timeBetweenRetriesMsec);
                } catch (InterruptedException x) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "sleep interrupted for " + config.getExternalName() + ", will proceed");
                    }
                    Thread.currentThread().interrupt();
                }
                retriesLeft--;
                continue;
            }

            // Terminal failure — log and notify
            if (!channel.isOpen()) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "No retries left and channel is not open, returning for " + config.getExternalName());
                }
                return future;
            }

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
                Tr.error(tc, TCPMessageConstants.LOCAL_HOST_UNRESOLVED,
                         new Object[] { config.getExternalName(), resolvedHost, String.valueOf(inetPort) });
            } else {
                if (config.isInbound()) {
                    Tr.error(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(), resolvedHost,
                                                                                String.valueOf(inetPort), future.cause().getMessage() });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, TCPMessageConstants.BIND_ERROR, new Object[] { config.getExternalName(),
                                                                                    resolvedHost, String.valueOf(inetPort), future.cause().getMessage() });
                    }
                }
            }

            if (openListener != null) {
                try {
                    generateOpenListenerWrapper(framework, openListener).operationComplete(future);
                } catch (Exception e) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Exception dispatching openListener failure callback: " + e.getMessage());
                    }
                }
            }
            return future;
        }
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
                    // open() blocks this thread (SCR/caller) with await() — safe because
                    // this is not a Netty I/O thread.
                    open(framework, channel, config, inetHost, inetPort, openListener,
                         config.getPortOpenRetries());
                } else {
                    framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                        @Override
                        public ChannelFuture call() {
                            // Running on the Liberty executor thread — safe to block with await().
                            // open() drives the full retry loop synchronously here, so this
                            // Callable does not return until the terminal bind outcome (success
                            // or all retries exhausted).  StartTaskRunnable.task.get() therefore
                            // unblocks at exactly the right time with no CompletableFuture needed.
                            return open(framework, channel, config, inetHost, inetPort, openListener,
                                        config.getPortOpenRetries());
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

}
