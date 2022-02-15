/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.support;

import java.security.AccessController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.staxutils.StaxUtils;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.ExtensionProvider;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;
import com.ibm.ws.jaxws.utils.StAXUtils;
import com.ibm.ws.util.ThreadContextAccessor;
import com.ibm.wsspi.jaxws.JaxWsService;

/**
 * JAXWSServiceImpl is used to initial global JAX-WS configurations
 */
public class JaxWsServiceImpl implements JaxWsService {

    private static final TraceComponent tc = Tr.register(JaxWsServiceImpl.class);

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /*
     * Called by Declarative Services to activate service
     */
    protected void activate(ComponentContext cc) {
        //Create default server side global bus
        ClassLoader orignalClassLoader = THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
        try {
            //Make sure the current ThreadContextClassLoader is not an App classloader
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), JaxWsServiceImpl.class.getClassLoader());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("org.apache.cxf.bus.id", "Default Bus");
            Bus defaultBus = LibertyApplicationBusFactory.getInstance().createBus(null, properties);
            LibertyApplicationBusFactory.setDefaultBus(defaultBus);

        } finally {
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), orignalClassLoader);
        }

        //Eager initialize the StaxUtils
        try {
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), StAXUtils.getStAXProviderClassLoader());
            if(System.getProperty(StaxUtils.ALLOW_INSECURE_PARSER) == null) {
                System.setProperty(StaxUtils.ALLOW_INSECURE_PARSER, "true");
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Insecure Stax property was null setting it to true.");
                }
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Insecure Stax property is set to: " + System.getProperty(StaxUtils.ALLOW_INSECURE_PARSER));
            }
            
            Class.forName("org.apache.cxf.staxutils.StaxUtils");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        } finally {
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), orignalClassLoader);
        }
    }

    /*
     * Called by Declarative Services to modify service configuration properties
     */
    protected void modified(Map<String, Object> newProps) {
    }

    /*
     * Called by Declarative Services to deactivate service
     */
    protected void deactivate(ComponentContext cc) {
        BusFactory.getDefaultBus().shutdown(false);
    }

    public void registerApplicationBusListener(LibertyApplicationBusListener listener) {
        LibertyApplicationBusFactory.getInstance().registerApplicationBusListener(listener);
    }

    public void unregisterApplicationBusListener(LibertyApplicationBusListener listener) {
        LibertyApplicationBusFactory.getInstance().unregisterApplicationBusListener(listener);
    }

    /**
     * Register a new extension provier
     *
     * @param provider The extension provider
     */
    public void registerExtensionProvider(ExtensionProvider provider) {
        LibertyApplicationBusFactory.getInstance().registerExtensionProvider(provider);
    }

    /**
     * Unregister the extension provider
     *
     * @param provider The extension provider
     */
    public void unregisterExtensionProvider(ExtensionProvider provider) {
        LibertyApplicationBusFactory.getInstance().unregisterExtensionProvider(provider);
    }

    public List<Bus> getServerScopedBuses() {
        return LibertyApplicationBusFactory.getInstance().getServerScopedBuses();
    }

    public List<Bus> getClientScopedBuses() {
        return LibertyApplicationBusFactory.getInstance().getClientScopedBuses();
    }
}
