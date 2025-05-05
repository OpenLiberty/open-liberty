/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.netty.internal.local;

import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCSelfIntrospectable;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.exception.NettyException;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.tcp.TCPChannelInitializerImpl;
import io.openliberty.netty.internal.tcp.ValidateUtils;

@Trivial
public class LocalConfigurationImpl implements BootstrapConfiguration, FFDCSelfIntrospectable {

	private static final TraceComponent tc = Tr.register(LocalConfigurationImpl.class, NettyConstants.NETTY_TRACE_NAME,
			NettyConstants.BASE_BUNDLE);

	private ChannelData channelData = null;
	private Map<String, Object> channelProperties = null;


	private final boolean inbound;

	/**
	 * Constructor.
	 *
	 * @param options
	 * @param inbound
	 * @throws ChannelException
	 */
	public LocalConfigurationImpl(Map<String, Object> options, boolean inbound) throws NettyException {
		this.inbound = inbound;
		this.channelProperties = options;
	}

	/**
	 * Apply this config to a {@link io.netty.bootstrap.ServerBootstrap} 
	 * Note that most props are implemented via handlers, 
	 * see the initialisation wrapper for the protocol
	 * 
	 * @param bootstrap
	 */
	public void applyConfiguration(ServerBootstrap bootstrap) {
		if(!inbound) {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Apply this config to a {@link io.netty.bootstrap.Bootstrap} 
	 * Note that most props are implemented via handlers, 
	 * see the initialisation wrapper for the protocol
	 * @param bootstrap
	 */
	@Override
	public void applyConfiguration(Bootstrap bootstrap) {
		if(inbound) {
			throw new UnsupportedOperationException();
		}
	}

    /**
     * @return boolean
     */
    public boolean isInbound() {
        return this.inbound;
    }
    
    /**
     * A useful function to omit standard OSGi config from the dumps of Channels' config
     * @param key
     * @return
     */
	//@formatter:off
    private boolean skipKey(String key) {
        return key.equals("id") ||
               key.equals("type") ||
               key.startsWith("service.") ||
               key.startsWith("component.") ||
               key.startsWith("config.") ||
               key.startsWith("objectClass") ||
               key.startsWith("osgi.ds.");
    }
    //@formatter:on

    
	protected ChannelData getChannelData() {
		return this.channelData;
	}

	/* This is abstract to ensure that local channels implement introspection */
	@Override
	public String[] introspectSelf() {
		//TODO GDH 
		return new String[] {"There is probably more to be implemented here"};
	}

}
