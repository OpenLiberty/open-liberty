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
	 * based on address/port addressing Interestingly we need ServerChannel, which
	 * is an empty marker interface but LocalChannel is not marked with this so we
	 * expect a class with 'isa' LocalChannel and 'implements' ServerChannel - in
	 * order to use this method.
	 * 
	 * @param framework
	 * @param channel   class - this is the class of the channel that is added
	 * @param options
	 * @return
	 * @throws NettyException
	 */
	public static ServerBootstrapExtended createLocalBootstrap(NettyFrameworkImpl framework,
			ChannelInitializerWrapper protocolInitializer, Map<String, Object> options) throws NettyException {
		LocalConfigurationImpl config = new LocalConfigurationImpl(options, true);

		ServerBootstrapExtended bs = new ServerBootstrapExtended();
		bs.group(framework.getParentGroup(), framework.getChildGroup());
		bs.setBaseInitializer(protocolInitializer);

		bs.applyConfiguration(config);

		//TODO server concerns other than protocol needs?
		ChannelInitializerWrapper serverInitializer = new LocalChannelInitializerImpl(config, framework);

		return bs;
	}

	/**
	 * Create a {@link BootstrapExtended} for outbound local channels
	 * 
	 * @param framework
	 * @param channel   class - this is the class of the channel that is added
	 * @param options
	 * @return
	 * @throws NettyException
	 */
	public static BootstrapExtended createLocalBootstrapOutbound(NettyFrameworkImpl framework,
			ChannelInitializerWrapper protocolInitializer, Map<String, Object> options) throws NettyException {
		LocalConfigurationImpl config = new LocalConfigurationImpl(options, false);
		BootstrapExtended bs = new BootstrapExtended();
		bs.group(framework.getChildGroup());

		bs.setBaseInitializer(protocolInitializer);
		bs.applyConfiguration(config);
		
		//TODO server concerns other than protocol needs
		// ChannelInitializerWrapper serverInitializer = new LocalChannelInitializerImpl(config, framework);
	

		return bs;
	}

	private static ChannelFuture open(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
			final LocalConfigurationImpl config, LocalAddress addr, ChannelFutureListener openListener,
			final int retryCount) {
		return null;
	}

	private static FutureTask<ChannelFuture> startHelper(NettyFrameworkImpl framework, AbstractBootstrap bootstrap,
			LocalConfigurationImpl config, LocalAddress addr, ChannelFutureListener openListener)
			throws NettyException {
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
	public static FutureTask<ChannelFuture> start(NettyFrameworkImpl framework, ServerBootstrapExtended bootstrap,
			String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
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
			String inetHost, int inetPort, ChannelFutureListener openListener) throws NettyException {
		return null;
	}

	/**
	 * TODO check what the CF implementation produced and match
	 * 
	 * @param channel
	 */
	public static void logChannelStopped(Channel channel) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.info(tc, "Channel stopped:" + channel);
		}
	}

	/**
	 * TODO check what the CF implementation produced and match
	 * 
	 * @param channel
	 */
	public static void logChannelStarted(Channel channel) {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
			Tr.info(tc, "Channel started:" + channel);
		}
	}
}
