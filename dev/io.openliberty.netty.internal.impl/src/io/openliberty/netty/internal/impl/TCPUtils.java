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

import java.util.Map;
import java.util.concurrent.Callable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.ConfigConstants;
import io.openliberty.netty.internal.ServerBootstrapExtended;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.TCPMessageConstants;
import io.openliberty.netty.internal.tcp.TCPConfigurationImpl;

public class TCPUtils {

    private static final TraceComponent tc = Tr.register(NettyFrameworkImpl.class, TCPMessageConstants.NETTY_TRACE_NAME, TCPMessageConstants.TCP_BUNDLE);
    private static final int timeBetweenRetriesMsec = 1000; // make this non-configurable

    public static ServerBootstrapExtended createTCPBootstrap(NettyFrameworkImpl framework, Map<String, Object> tcpOptions) throws NettyException {
        BootstrapConfiguration config = new TCPConfigurationImpl(tcpOptions, true);
        ServerBootstrapExtended bs = new ServerBootstrapExtended();
        bs.group(framework.parentGroup, framework.childGroup);
        bs.channel(NioServerSocketChannel.class);
        // apply the existing user config to the Netty TCP channel
        bs.applyConfiguration(config);
        ChannelInitializerWrapper tcpInitializer = new TCPChannelInitializerImpl(config);
        bs.setBaseInitializer(tcpInitializer);
        return bs;
    }

    private static ChannelFuture bind(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener, final int retryCount) {
        ChannelFuture bindFuture = bootstrap.bind(inetHost, inetPort);
        if (bindListener != null) {
            bindFuture.addListener(bindListener);
        }
        final String channelName = ((TCPConfigurationImpl) bootstrap.getConfiguration()).getExternalName();
        
        bindFuture.addListener(future -> {
            if (future.isSuccess()) {
                framework.activeChannels.add(bindFuture.channel());
                
                // set common channel attrs
                bindFuture.channel().attr(ConfigConstants.NameKey).set(channelName);
                bindFuture.channel().attr(ConfigConstants.HostKey).set(inetHost);
                bindFuture.channel().attr(ConfigConstants.PortKey).set(inetPort);

                Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STARTED, new Object[] { channelName, inetHost, String.valueOf(inetPort) });
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "bindHelper failed for " + channelName +" due to: " + future.cause().getMessage());
                }

                if (retryCount > 0) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "attempt to bind again after a wait of " +timeBetweenRetriesMsec +"ms; " + retryCount + " attempts remaining" +" for " + channelName);
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
                    bind(framework, bootstrap, inetHost, inetPort, bindListener, retryCount - 1);
                } else {
                    Tr.error(tc, TCPMessageConstants.BIND_ERROR,
                            new Object[] { channelName, inetHost, String.valueOf(inetPort), 
                                    bindFuture.cause().getMessage() });
                }
            }
        });
        return bindFuture;
    }

    public static ChannelFuture start(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener) throws NettyException {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "start (TCP): attempt to bind a channel at host " + inetHost + " port " + inetPort);
        }
        try {
            framework.runWhenServerStarted(new Callable<ChannelFuture>() {
                @Override
                public ChannelFuture call() {
                    TCPConfigurationImpl config = (TCPConfigurationImpl) bootstrap.getConfiguration();
                    int bindRetryCount = config.getPortOpenRetries();
                    return bind(framework, bootstrap, inetHost, inetPort, bindListener, bindRetryCount);
                }
            });
        } catch (Exception e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "caught exception performing late cycle server startup task: " + e.getMessage());
            }
        }
        return null;
    }

    public static void logChannelStopped(Channel channel) {
        String channelName = channel.attr(ConfigConstants.NameKey).get();
        String host = channel.attr(ConfigConstants.HostKey).get();
        Integer port = channel.attr(ConfigConstants.PortKey).get();
        Tr.info(tc, TCPMessageConstants.TCP_CHANNEL_STOPPED, channelName, host, String.valueOf(port));
    }

}
