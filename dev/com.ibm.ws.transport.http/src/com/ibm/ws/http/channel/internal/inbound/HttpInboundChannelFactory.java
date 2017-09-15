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
package com.ibm.ws.http.channel.internal.inbound;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.ws.http.channel.internal.HttpChannelFactory;
import com.ibm.ws.http.channel.internal.HttpConfigConstants;
import com.ibm.ws.http.channel.internal.outbound.HttpOutboundChannelFactory;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.http.channel.inbound.HttpInboundServiceContext;

/**
 * Factory to generate the HTTP inbound channels.
 */
public class HttpInboundChannelFactory extends HttpChannelFactory {

    /**
     * Constructor for an HTTP inbound channel factory.
     */
    public HttpInboundChannelFactory() {

        super(HttpInboundServiceContext.class);
    }

    /**
     * Create a new inbound HTTP channel.
     * 
     * @param channelData
     * @return Channel (HttpInboundChannel)
     */
    @Override
    public Channel createChannel(ChannelData channelData) {
        return new HttpInboundChannel(channelData, this, getObjectFactory());
    }

    /*
     * @see
     * com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java
     * .util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> props) {
        return new HttpOutboundDefinition(props);
    }

    /**
     * Http channel implementation of an outbound definition.
     */
    private class HttpOutboundDefinition implements OutboundChannelDefinition {
        private static final long serialVersionUID = 8890341630843524730L;
        private Map<Object, Object> config = new HashMap<Object, Object>();

        /**
         * Constructor.
         * 
         * @param props
         */
        protected HttpOutboundDefinition(Map<Object, Object> props) {
            String binary = (String) props.get(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT);
            if (null != binary) {
                this.config.put(HttpConfigConstants.PROPNAME_BINARY_TRANSPORT, binary);
            }
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#
         * getOutboundChannelProperties()
         */
        @Override
        public Map<Object, Object> getOutboundChannelProperties() {
            return this.config;
        }

        /*
         * @see
         * com.ibm.websphere.channelfw.OutboundChannelDefinition#getOutboundFactory
         * ()
         */
        @Override
        public Class<?> getOutboundFactory() {
            return HttpOutboundChannelFactory.class;
        }

        /*
         * @see com.ibm.websphere.channelfw.OutboundChannelDefinition#
         * getOutboundFactoryProperties()
         */
        @Override
        public Map<Object, Object> getOutboundFactoryProperties() {
            // nothing required
            return null;
        }
    }

}
