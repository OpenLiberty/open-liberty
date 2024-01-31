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

import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;

/**
 * Registers channel handlers which implement various TCP configuration options. Handlers are
 * initialized with the current values of the registered TCPConfiguration.
 * 
 * Currently these handlers implement the following tcpOptions: 
 * inactivityTimeout, maxOpenConnections, addressExcludeList, addressIncludeList, hostNameExcludeList, hostNameIncludeList
 */
public class LocalChannelInitializerImpl extends ChannelInitializerWrapper {
	
	 private static final TraceComponent tc = Tr.register(LocalChannelInitializerImpl.class, NettyConstants.NETTY_TRACE_NAME,
	            NettyConstants.BASE_BUNDLE);	

    LocalConfigurationImpl config;

	NettyFrameworkImpl bundle;
    
    
    public LocalChannelInitializerImpl(BootstrapConfiguration config, NettyFrameworkImpl bundle) {
    	this.bundle = bundle;
        this.config = (LocalConfigurationImpl) config;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
    	// TODO Add logging equal to channelfw
    	if(bundle.isStopping()) {
    		if(TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            	Tr.event(tc, "Tried to start channel: " + channel + " while framework was shutting down. " + bundle);
            }
    		channel.close();
    		return;
    	}
        if (TraceComponent.isAnyTracingEnabled()) {
   //     	channel.pipeline().addFirst(NettyConstants.TCP_LOGGING_HANDLER_NAME, new TCPLoggingHandler());
		}
  //      if (config.getInactivityTimeout() > 0) {
  //          channel.pipeline().addLast(NettyConstants.INACTIVITY_TIMEOUT_HANDLER_NAME, new InactivityTimeoutHandler(0, 0, config.getInactivityTimeout(), TimeUnit.MILLISECONDS));
  //      }
        
    
        
  //      if (config.getAccessLists() != null) {
  //          AccessListHandler includeHandler = new AccessListHandler(config.getAccessLists());
  //          channel.pipeline().addLast(NettyConstants.ACCESSLIST_HANDLER_NAME, includeHandler);
  //      }
    
        //      MaxOpenConnectionsHandler maxHandler = new MaxOpenConnectionsHandler(config.getMaxOpenConnections());
//        channel.pipeline().addLast(NettyConstants.MAX_OPEN_CONNECTIONS_HANDLER_NAME, maxHandler);
  
        Channel parent = channel.parent();
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        	Tr.debug(tc, "Initializing channel: " + channel + " found parent to be: " + parent);
        }
        
        
        // Add channel to endpoint ChannelGroup if known
        if(parent != null) {
        	ChannelGroup group = bundle.getActiveChannelsMap().get(parent);
        //	channel.closeFuture().addListener(innerFuture -> TCPUtils.logChannelStopped(channel));
        	if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            	Tr.debug(tc, "Found group to be: " + group + " for parent: " + parent);
            }
  
        	if(group != null) {
        		group.add(channel);
        	}
        }
    }

}
