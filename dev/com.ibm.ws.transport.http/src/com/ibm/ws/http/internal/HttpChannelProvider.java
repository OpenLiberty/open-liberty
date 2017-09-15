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
package com.ibm.ws.http.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.http.channel.internal.inbound.HttpInboundChannelFactory;
import com.ibm.ws.http.channel.internal.outbound.HttpOutboundChannelFactory;
import com.ibm.ws.http.dispatcher.internal.channel.HttpDispatcherFactory;
import com.ibm.wsspi.channelfw.ChannelFactory;

/**
 * Simple service to provide various HTTP channels to the Channel Framework. This
 * service will activate the HttpDispatcher reference as soon as someone tries to use
 * one of those HTTP channel types, either inbound or outbound. If nobody uses the
 * channels, then the bulk of the HTTP bundle is never started.
 */
public class HttpChannelProvider implements BundleActivator, ChannelFactoryProvider {
    /** Trace service */
    private static final TraceComponent tc = Tr.register(HttpChannelProvider.class);

    /** Factories provided by this class */
    private final Map<String, Class<? extends ChannelFactory>> factories;

    private ServiceRegistration<ChannelFactoryProvider> registration = null;

    /**
     * Constructor.
     */
    public HttpChannelProvider() {
        this.factories = new HashMap<String, Class<? extends ChannelFactory>>();
        this.factories.put("HTTPInboundChannel", HttpInboundChannelFactory.class);
        this.factories.put("HTTPOutboundChannel", HttpOutboundChannelFactory.class);
        this.factories.put("HTTPDispatcherChannel", HttpDispatcherFactory.class);
    }

    /** {@inheritDoc} */
    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_VENDOR, "IBM");
        props.put("type", "HTTPChannel");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Registering HTTPChannelProvider", props);
        }

        registration = context.registerService(ChannelFactoryProvider.class, this, props);
    }

    /** {@inheritDoc} */
    @Override
    public void stop(BundleContext context) throws Exception {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(this, tc, "Unregistering HTTPChannelProvider");
        }
        registration.unregister();
    }

    /*
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#getTypes()
     */
    @Override
    public Map<String, Class<? extends ChannelFactory>> getTypes() {
        return Collections.unmodifiableMap(factories);
    }

    /*
     * @see com.ibm.websphere.channelfw.osgi.ChannelFactoryProvider#init()
     */
    @Override
    public void init() {}
}
