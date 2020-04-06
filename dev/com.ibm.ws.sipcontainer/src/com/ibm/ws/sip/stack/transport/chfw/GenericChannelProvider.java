/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sip.stack.transport.chfw;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sip.stack.transport.sip.SipChannelFactory;
import com.ibm.ws.sip.stack.transport.sip.SipInboundChannel;
import com.ibm.ws.sip.stack.transport.sip.SipInboundChannelFactoryWs;
import com.ibm.ws.sip.stack.transport.sip.SipTcpOutboundChannel;
import com.ibm.ws.sip.stack.transport.sip.SipTcpOutboundChannelFactory;
import com.ibm.ws.sip.stack.transport.sip.SipTlsOutboundChannel;
import com.ibm.ws.sip.stack.transport.sip.SipTlsOutboundChannelFactory;
import com.ibm.ws.sip.stack.transport.sip.SipUdpOutboundChannel;
import com.ibm.ws.sip.stack.transport.sip.SipUdpOutboundChannelFactory;
import com.ibm.wsspi.channelfw.ChannelFactory;

/**
 * Simple service to provide various Generic channels to the Channel Framework. This
 * service will activate the HttpDispatcher reference as soon as someone tries to use
 * one of those SIP channel types, either inbound or outbound.
 */
public class GenericChannelProvider implements ChannelFactoryProvider {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(GenericChannelProvider.class);

    /** Factories provided by this class */
    private final Map<String, Class<? extends ChannelFactory>> factories;

    private ServiceRegistration<ChannelFactoryProvider> registration = null;
    
    BundleContext bundleContext = null;

    /**
     * Constructor.
     * @throws Exception 
     */
    public GenericChannelProvider(BundleContext context) throws Exception {
        this.factories = new HashMap<String, Class<? extends ChannelFactory>>();
		
        bundleContext = context;
        // Save all the facotries which are used by the Channel Framework implementation 
        // in stack.
        // They will be later pulled by the ChannelFramework component by getTypes() method
        this.factories.put("SipChannel", SipChannelFactory.class);
		this.factories.put(SipInboundChannel.SipInboundChannelName, SipInboundChannelFactoryWs.class);
		this.factories.put(SipUdpOutboundChannel.SipUdpOutboundChannelName, SipUdpOutboundChannelFactory.class);
		this.factories.put(SipTcpOutboundChannel.SipTcpOutboundChannelName, SipTcpOutboundChannelFactory.class);
		this.factories.put(SipTlsOutboundChannel.SipTlsOutboundChannelName, SipTlsOutboundChannelFactory.class);
		
		startChannelProvider();
    }

    /**
     * startChannelProvider
     * @throws Exception
     */
    public void startChannelProvider() throws Exception {
    	 Hashtable<String, String> props = new Hashtable<String, String>();
	        props.put(Constants.SERVICE_VENDOR, "IBM");
	        props.put("type", "GenericChannel");
	        
	        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
	            Tr.event(this, tc, "Registering GenericChannelProvider");
	        }
		registration = bundleContext.registerService(ChannelFactoryProvider.class, this, props);
    }

    /**
     * stopChannelProvider
     * @throws Exception
     */
    public void stopChannelProvider() throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Unregistering GenericChannelProvider");
        }
        registration.unregister();
    }

    /**
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#getTypes()
     */
    @Override
    public Map<String, Class<? extends ChannelFactory>> getTypes() {
        return Collections.unmodifiableMap(factories);
    }

    /**
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#init()
     */
    @Override
    public void init() {}
}
