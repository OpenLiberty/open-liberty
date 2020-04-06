/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.sip;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import com.ibm.websphere.channelfw.ChannelData;
import com.ibm.websphere.channelfw.ChannelFactoryData;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.Channel;
import com.ibm.wsspi.channelfw.ChannelFactory;
import com.ibm.wsspi.channelfw.exception.ChannelException;
import com.ibm.wsspi.channelfw.exception.ChannelFactoryException;
import com.ibm.wsspi.tcpchannel.SSLConnectionContext;
import com.ibm.wsspi.tcpchannel.TCPConnectionContext;
import com.ibm.wsspi.udpchannel.UDPContext;

/**
 * base class for factories that create sip application channels
 * 
 * @author ran
 */
public abstract class SipChannelFactory implements ChannelFactory
{
	/** class logger */
	private static final TraceComponent tc = Tr.register(SipChannelFactory.class);
	
	/**
	 * device interfaces.
	 * both the SIP protocol channel and the transport channels are declared.
	 * the SIP protocol channel is needed here because that's the channel that
	 * the SIP proxy needs to see in order to create the equivalent outbound
	 * chain to the container.
	 * the transport channels are the ones that are actually used locally. 
	 */
	private static final Class[] s_deviceInterfaces = {
		UDPContext.class,
		TCPConnectionContext.class,
		SSLConnectionContext.class
	};
	
	/**
	 * constructor
	 */
	public SipChannelFactory() {
	}
	
	/**
	 * @see com.ibm.wsspi.channelfw.ChannelFactory#init(com.ibm.wsspi.channelfw.framework.ChannelFactoryData)
	 */
	public void init(ChannelFactoryData data) throws ChannelFactoryException {
		//updateProperties(data.getProperties());
	}

	/**
	 * @see com.ibm.wsspi.channelfw.ChannelFactory#destroy()
	 */
	public void destroy() {
	}

	/**
	 * (non-Javadoc)
	 * @see com.ibm.wsspi.channelfw.ChannelFactory#getDeviceInterface()
	 */
	public Class[] getDeviceInterface() {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,"getDeviceInterface", Arrays.toString(s_deviceInterfaces));
        }
		return s_deviceInterfaces;
	}

	/**
	 *  @see com.ibm.wsspi.channelfw.ChannelFactory#getApplicationInterface()
	 */
	public Class getApplicationInterface() {
		// there is no application interface
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc,"getApplicationInterface", "");
        }
		return null;
	}

	/**
	 * Builds and returns the appropriate type of channel for running in the zOS control region.
	 * In the control region on Z, the SIP container does not run.  Rather, the SIP proxy
	 * runs in its place and will proxy connections to the SIP containers running in the 
	 * servant regions.  The SIP proxy component starts up in the CR, but does nothing with
	 * starting chains.  The XMEM component reads the SIP container configuration and starts
	 * up the chains which terminate in this channel factory.  This is why the channel 
	 * returned is a SIP proxy channel rather than the SIP container channel.  
	 * @param config
	 * @return a new SipProxyInboundChannel
	 * @throws ChannelException
	 */
	protected Channel createControlRegionChannel(ChannelData config) throws ChannelException {
		if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
			Tr.debug(this, tc, "in SipChannelFactory.createControlRegionChannel");
		}
		        
		Channel channel;

		try {
			Class proxyClass;
			// load the SIP proxy channel class dynamically
			try {
				proxyClass = Class.forName("com.ibm.ws.proxy.channel.sip.SipProxyInboundChannel");
			}
			catch (Exception e) {
				ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
				proxyClass = Class.forName(
					"com.ibm.ws.proxy.channel.sip.SipProxyInboundChannel",
					true,
					classLoader);
			}
			Class[] params = new Class[] { ChannelData.class };
			Constructor c = proxyClass.getConstructor(params);
			Object [] args = new Object[] { config };
			channel = (Channel)c.newInstance(args);
		}
		catch (Exception e) {
			if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()){
				Tr.debug(this, tc, "Error creating SipProxyInboundChannel on behalf of SIP Container " + e);
			}
			throw new ChannelException("Error creating SipProxyInboundChannel on behalf of SIP Container", e);
		}
		return channel;
	}
}
