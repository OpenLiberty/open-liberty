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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.openliberty.netty.internal.BootstrapConfiguration;
import io.openliberty.netty.internal.ChannelInitializerWrapper;
import io.openliberty.netty.internal.impl.NettyConstants;
import io.openliberty.netty.internal.impl.NettyFrameworkImpl;

/**
 * This class is a common superclass for non-{port:host} based Netty channels in Liberty.
 * It adds handlers that are common across different local protocols that are useful
 * to the server management. A particular local protocol is expected to subclass this
 * to add protocol specific handlers and call super.initChannel(Channel) to invoke the
 * initialisation of common handlers here - the call to super.initChannel should be made
 * after protocol specific handlers are added.
 *
 * This enables OpenLiberty Netty code to handle local channels that
 * are not open source.
 */
public abstract class LocalChannelInitializerWrapper extends ChannelInitializerWrapper {
	
	 private static final TraceComponent tc = Tr.register(LocalChannelInitializerWrapper.class, NettyConstants.NETTY_TRACE_NAME,
	            NettyConstants.BASE_BUNDLE);	

    LocalConfigurationImpl config;
	NettyFrameworkImpl bundle;
    
    
    public LocalChannelInitializerWrapper(BootstrapConfiguration config, NettyFrameworkImpl bundle) {
    	this.bundle = bundle;
        this.config = (LocalConfigurationImpl) config;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
    	// TODO Add logging equal to channelfw

    	
    	// DO not start a channel when the server is closing down.
    	if(bundle.isStopping()) {
    		if(TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            	Tr.event(tc, "Tried to start channel: " + channel + " while framework was shutting down. " + bundle);
            }
    		channel.close();
    		return;
    	}

    	//TODO The logging handler appears to do more than logging (search for 'write') - worth investigating
        if (TraceComponent.isAnyTracingEnabled()) {
        	channel.pipeline().addFirst(NettyConstants.LOCAL_LOGGING_HANDLER_NAME, new LocalLoggingHandler());
		}
        
        Channel parent = channel.parent();
        if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
        	Tr.debug(tc, "Initializing channel: " + channel + " found parent to be: " + parent);
        }
        
        
        // Add channel to endpoint ChannelGroup if known
        if(parent != null) {
        	ChannelGroup group = bundle.getActiveChannelsMap().get(parent);

        	channel.closeFuture().addListener(innerFuture -> LocalUtils.logChannelStopped(channel));
        	if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            	Tr.debug(tc, "Found group to be: " + group + " for parent: " + parent);
            }
  
        	if(group != null) {
        		group.add(channel);
        	}
        }
    }

}
