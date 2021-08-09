/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.channelfw.testsuite.channels.connector;

import java.util.Map;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.channelfw.OutboundChannelDefinition;
import com.ibm.ws.tcpchannel.internal.TCPChannelFactory;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryAlreadyInitializedException;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;

/**
 * 
 */
@SuppressWarnings("unused")
public class MainChannelFactory implements ChannelFactory {

    Map<Object, Object> props = null;

    /** unitialized state */
    public static final int UNINIT = -1;
    /** initialized state */
    public static final int INIT = 0;
    /** destroyed state */
    public static final int DEST = 1;
    /** current state of factory */
    public int state = UNINIT;

    public Channel findOrCreateChannel(ChannelData config) throws ChannelException {
        return new MainChannel(config);
    }

    public void updateProperties(Map<Object, Object> properties) {
        props = properties;
    }

    public void init(ChannelFactoryData data) throws ChannelFactoryAlreadyInitializedException {
        props = data.getProperties();
        state = INIT;
    }

    public void destroy() {
        state = DEST;
    }

    public Class<?> getApplicationInterface() {
        return TCPConnectionContext.class;
    }

    public Class<?>[] getDeviceInterface() {
        return new Class<?>[] { TCPConnectionContext.class };
    }

    public Map<Object, Object> getProperties() {
        return props;
    }

    /*
     * @see com.ibm.wsspi.channelfw.ChannelFactory#getOutboundChannelDefinition(java.util.Map)
     */
    @Override
    public OutboundChannelDefinition getOutboundChannelDefinition(Map<Object, Object> data) {
        return null;
    }

}
