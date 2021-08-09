/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.http.channel.internal.outbound;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.ws.http.channel.internal.HttpChannelFactory;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.http.channel.outbound.HttpOutboundServiceContext;

/**
 * Factory to create new HTTP outbound channels.
 */
public class HttpOutboundChannelFactory extends HttpChannelFactory {

    /**
     * Constructor for an HTTP outbound channel factory.
     */
    public HttpOutboundChannelFactory() {
        super(HttpOutboundServiceContext.class);
    }

    /**
     * Create a new HTTP outbound channel instance.
     * 
     * @param channelData
     * @return Channel (HttpOutboundChannel)
     */
    @Override
    public Channel createChannel(ChannelData channelData) {
        return new HttpOutboundChannel(channelData, this, getObjectFactory());
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java
     * .util.Map)
     */
    @Override
    @SuppressWarnings("unused")
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return null;
    }

}
