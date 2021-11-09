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
package io.openliberty.netty.internal.udp;

import java.net.InetSocketAddress;
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
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;

public class UDPUtils {

    private static final TraceComponent tc = Tr.register(NettyFrameworkImpl.class, UDPMessageConstants.NETTY_TRACE_NAME,
            UDPMessageConstants.UDP_BUNDLE);

    /**
     * Create a {@link BootstrapExtended} for inbound UDP channels
     * 
     * @param framework
     * @param options
     * @return
     * @throws NettyException
     */
    public static BootstrapExtended createUDPBootstrap(NettyFrameworkImpl framework, Map<String, Object> options)
            throws NettyException {
        return create(framework, new UDPConfigurationImpl(options, true));
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
        return create(framework, new UDPConfigurationImpl(options, false));
    }

    private static BootstrapExtended create(NettyFrameworkImpl framework, UDPConfigurationImpl config) {
        BootstrapExtended bs = new BootstrapExtended();
        bs.applyConfiguration(config);
        bs.group(framework.getParentGroup());
        bs.channel(NioDatagramChannel.class);
        return bs;
    }

    private static ChannelFuture bind(NettyFrameworkImpl framework, BootstrapExtended bootstrap, String inetHost,
            int inetPort, final int retryCount, final int retryDelay, ChannelFutureListener bindListener) {
        ChannelFuture bindFuture = bootstrap.bind(inetHost, inetPort);
        if (bindListener != null) {
            bindFuture.addListener(bindListener);
        }
        final UDPConfigurationImpl config = ((UDPConfigurationImpl) bootstrap.getConfiguration());
        final String channelName = ((UDPConfigurationImpl) bootstrap.getConfiguration()).getExternalName();

        bindFuture.addListener(future -> {
            if (future.isSuccess()) {

                // add the new channel to the set of active channels, and set a close future to
                // remove it
                final Channel channel = bindFuture.channel();
                framework.getActiveChannels().add(bindFuture.channel());
                channel.closeFuture().addListener(innerFuture -> framework.stop(channel));

                // set common channel attrs
                channel.attr(ConfigConstants.NameKey).set(config.getExternalName());
                channel.attr(ConfigConstants.HostKey).set(inetHost);
                channel.attr(ConfigConstants.PortKey).set(inetPort);
                channel.attr(ConfigConstants.IsInboundKey).set(config.isInboundChannel());

                if (config.isInboundChannel()) {
                    // UDP CWWKO0400I listening message
                    Tr.info(tc, UDPMessageConstants.UDP_CHANNEL_STARTED,
                            new Object[] { config.getExternalName(), inetHost, String.valueOf(inetPort) });
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, UDPMessageConstants.UDP_CHANNEL_STARTED,
                                new Object[] { config.getExternalName(), inetHost, String.valueOf(inetPort) });
                    }
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "bindHelper failed due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to bind again after a wait of " + retryDelay + "ms; " + retryCount
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
                    bind(framework, bootstrap, inetHost, inetPort, retryCount - 1, retryDelay, bindListener);
                } else {
                    if (config.isInboundChannel()) {
                        Tr.error(tc, UDPMessageConstants.BIND_FAILURE,
                                new Object[] { channelName, inetHost, String.valueOf(inetPort) });
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, UDPMessageConstants.BIND_FAILURE,
                                    new Object[] { channelName, inetHost, String.valueOf(inetPort) });
                        }
                    }
                }
            }
        });
        return bindFuture;
    }

    /**
     * Start an outbound UDP channel
     * 
     * @param nettyFrameworkImpl
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param bindListener
     * @return
     * @throws NettyException
     */
    public static ChannelFuture startOutbound(NettyFrameworkImpl framework, BootstrapExtended bootstrap,
            String inetHost, int inetPort, ChannelFutureListener bindListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "startOutbound (UDP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        return startHelper(framework, bootstrap, inetHost, inetPort, bindListener);
    }

    /**
     * Start an inbound UDP channel
     * 
     * @param framework
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param bindListener
     * @return
     * @throws NettyException
     */
    public static ChannelFuture start(NettyFrameworkImpl framework, BootstrapExtended bootstrap, String inetHost,
            int inetPort, ChannelFutureListener bindListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (UDP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        return startHelper(framework, bootstrap, inetHost, inetPort, bindListener);
    }

    private static ChannelFuture startHelper(NettyFrameworkImpl framework, BootstrapExtended bootstrap, String inetHost,
            int inetPort, ChannelFutureListener bindListener) throws NettyException {
        try {
            framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() throws NettyException {
                    UDPConfigurationImpl config = (UDPConfigurationImpl) bootstrap.getConfiguration();
                    int bindRetryCount = config.getRetryCount();
                    int bindRetryInterval = config.getRetryInterval();
                    InetSocketAddress address = null;
                    String newHost = inetHost;
                    if (newHost.equals("*")) {
                        newHost = "0.0.0.0";
                    }
                    address = new InetSocketAddress(newHost, inetPort);
                    if (address.isUnresolved()) {
                        final String channelName = ((UDPConfigurationImpl) bootstrap.getConfiguration())
                                .getExternalName();
                        Tr.error(tc, UDPMessageConstants.DNS_LOOKUP_FAILURE,
                                new Object[] { channelName, newHost, String.valueOf(inetPort) });
                        throw new NettyException(
                                "local address unresolved for " + channelName + " - " + newHost + ":" + inetPort);
                    }

                    return bind(framework, bootstrap, newHost, inetPort, bindRetryCount, bindRetryInterval,
                            bindListener);
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
     * Log a UDP channel stopped message. Inbound channels will log a INFO message,
     * and outbound channels will log DEBUG
     * 
     * @param channel
     */
    public static void logChannelStopped(Channel channel) {
        String channelName = channel.attr(ConfigConstants.NameKey).get();
        String host = channel.attr(ConfigConstants.HostKey).get();
        Integer port = channel.attr(ConfigConstants.PortKey).get();
        if (channel.attr(ConfigConstants.IsInboundKey).get()) {
            Tr.info(tc, UDPMessageConstants.UDP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        } else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, UDPMessageConstants.UDP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
        }
    }

}
