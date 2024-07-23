/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import com.ibm.websphere.channelfw.EndPointMgr;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.local.LocalAddress;
import io.openliberty.netty.internal.exception.NettyException;

public interface NettyFramework {

    /**
     * Create a TCP bootstrap: handles registering the correct EventLoopGroups,
     * creating a NioServerSocketChannel, and implementing the props in tcpOptions.
     * 
     * Users should add any child handlers via the returned
     * {@link ServerBootstrapExtended}
     * 
     * @param tcpOptions
     * @return ServerBootstrap
     */
    ServerBootstrapExtended createTCPBootstrap(Map<String, Object> tcpOptions) throws NettyException;

    /**
     * Create a TCP bootstrap for outbound connections: handles registering the
     * correct EventLoopGroups, creating a NioServerSocketChannel, and implementing
     * the props in tcpOptions.
     * 
     * Users should add any child handlers via the returned {@link ServerBootstrap}
     * 
     * @param tcpOptions
     * @return ServerBootstrap
     */
    BootstrapExtended createTCPBootstrapOutbound(Map<String, Object> tcpOptions) throws NettyException;

    /**
     * Create a UDP bootstrap: handles registering the correct EventLoopGroups,
     * creating a NioDataGramChannel, and implementing and configuration props.
     * 
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
    BootstrapExtended createUDPBootstrap(Map<String, Object> options) throws NettyException;

    /**
     * Create a UDP bootstrap for outbound connections: handles registering the
     * correct EventLoopGroups, creating a NioDataGramChannel, and implementing and
     * configuration props.
     * 
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
    BootstrapExtended createUDPBootstrapOutbound(Map<String, Object> options) throws NettyException;

    /**
     * Create a local bootstrap: handles registering the correct EventLoopGroups,
     * creating a LocalChannel, and implementing and configuration properties.
     * This method is used by protocols that are based on local addresses rather
     * than remote host and port. It adds in the common handlers that Liberty
     * expects to add to a pipeline. The initializer will be used to add additional
     * protocol specifix handlers.
     * 
     * This is deprecated, use bootstrap.childHandler to add the protocol specific handler
     * @param initializer - and initializer for a particular protocol channel
     *                      that uses local addresses
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
    @Deprecated
	ServerBootstrapExtended createLocalBootstrap(ChannelInitializerWrapper initializer, Map<String, Object> options)
			throws NettyException;

    /**
     * Create a local bootstrap from Netty outbound: handles registering the
     * correct EventLoopGroups, creating a LocalChannel, and implementing and
     * configuration properties.
     * 
     * This is deprecated, use bootstrap.childHandler to add the protocol specific handler
     * 
     * @param initializer - an initializer for a particular protocol channel
     *                      that uses localAddresses
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
	@Deprecated
	BootstrapExtended createLocalBootstrapOutbound(ChannelInitializerWrapper initializer, Map<String, Object> options)
			throws NettyException;

    /**
     * Create a local bootstrap: handles registering the correct EventLoopGroups,
     * creating a LocalChannel, and implementing and configuration properties.
     * This method is used by protocols that are based on local addresses rather
     * than remote host and port. It adds in the common handlers that Liberty
     * expects to add to a pipeline. The initializer will be used to add additional
     * protocol specifix handlers.
     * 
     * @param initializer - and initializer for a particular protocol channel
     *                      that uses local addresses
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
	ServerBootstrapExtended createLocalBootstrap(Map<String, Object> options)
			throws NettyException;

    /**
     * Create a local bootstrap from Netty outbound: handles registering the
     * correct EventLoopGroups, creating a LocalChannel, and implementing and
     * configuration properties.
     * 
     * @param initializer - an initializer for a particular protocol channel
     *                      that uses localAddresses
     * @param options
     * @return BootstrapExtended
     * @throws NettyException
     */
	BootstrapExtended createLocalBootstrapOutbound(Map<String, Object> options)
			throws NettyException;

	
    /**
     * Binds a ServerBootstrap to the given host and port, and registers the
     * ServerChannel with this framework
     * 
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @return ChannelFuture for the ServerChannel, or null if the server is not yet
     *         started
     */
    FutureTask<ChannelFuture> start(ServerBootstrapExtended bootstrap, String inetHost, int inetPort,
            ChannelFutureListener bindListener) throws NettyException;

    /**
     * Binds a Bootstrap to the given host and port, and registers the Channel with
     * this framework
     * 
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @return ChannelFuture for the ServerChannel, or null if the server is not yet
     *         started
     */
    FutureTask<ChannelFuture> start(BootstrapExtended bootstrap, String inetHost, int inetPort, ChannelFutureListener bindListener)
            throws NettyException;

    /**
     * Binds (UDP) or connects (TCP) an outbound Bootstrap to the given host and
     * port, and registers the Channel with this framework
     * 
     * @param bootstrap
     * @param inetHost
     * @param inetPort
     * @param bindListener
     * @return ChannelFuture
     * @throws NettyException
     */
    FutureTask<ChannelFuture> startOutbound(BootstrapExtended bootstrap, String inetHost, int inetPort,
            ChannelFutureListener bindListener) throws NettyException;

    /**
     * Binds a ServerBootstrap to the LocalAddress, and registers the
     * ServerChannel with this framework
     * 
     * @param bootstrap
     * @param localAddr - a representation of the local endpoint address
     * @return ChannelFuture for the ServerChannel, or null if the server is not yet
     *         started
     */
    FutureTask<ChannelFuture> start(ServerBootstrapExtended bootstrap, LocalAddress localAddr,
            ChannelFutureListener bindListener) throws NettyException;

    /**
     * Binds a Bootstrap to the given LocalAddress, and registers the Channel with
     * this framework
     * 
     * @param bootstrap
     * @param localAddr - a representation of the local endpoint address
     * @return ChannelFuture for the ServerChannel, or null if the server is not yet
     *         started
     */
    FutureTask<ChannelFuture> start(BootstrapExtended bootstrap, LocalAddress localAddr, ChannelFutureListener bindListener)
            throws NettyException;

    /**
     * Connects an outbound Bootstrap to the given LocalAddress
     * and registers the Channel with this framework
     * 
     * @param bootstrap
     * @param localAddr - a representation of the local endpoint address
     * @param bindListener
     * @return ChannelFuture
     * @throws NettyException
     */
    FutureTask<ChannelFuture> startOutbound(BootstrapExtended bootstrap, LocalAddress localAddr,
            ChannelFutureListener bindListener) throws NettyException;

    /**
     * Removes a Channel from the set of active Channels. If the Channel is not
     * already closed, then close will be invoked and its ChannelFuture will be
     * returned.
     * 
     * @param channel
     * @return ChannelFuture for the Channel close
     */
    ChannelFuture stop(Channel channel);
    
    /**
     * Adds a handler to be notified on server stopping tp notify quiesce. This
     * handler will call the quiesceTask to run when the event is fired. The 
     * channel has to be an endpoint registered and started through the framework
     * otherwise a warning will be logged and ignored.
     * 
     * @param channel
     * @param quiesceTask
     */
    void registerEndpointQuiesce(Channel channel, Callable quiesceTask);

    /**
     * Removes a Channel from the set of active Channels. If the Channel is not
     * already closed, then close will be invoked and we'll block until the close
     * completes or times out.
     * 
     * @param channel
     * @param long    the amount of time in millis to wait for closure; use -1 to
     *                get the default
     */
    void stop(Channel channel, long time);

    /**
     * Query the active - open and connected - channels
     * 
     * @return the Set<Channel> of the active channels
     */
    Set<Channel> getActiveChannels();

    /**
     * Query the default chain quiesce timeout to use.
     * 
     * @return long
     */
    long getDefaultChainQuiesceTimeout();

    /**
     * Destroy the framework and release its resources. This will stop, destroy, and
     * clean up the resources used framework
     */
    void destroy();

    /**
     * helper method for getting access to the EndPointMgr
     *
     * @return EndPointMgr
     */
    public EndPointMgr getEndpointManager();

}
