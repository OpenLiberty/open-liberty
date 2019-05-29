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
package com.ibm.ws.jaxws23.component;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.osgi.CXFActivator;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.osgi.HTTPTransportActivator;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.ExtensionProvider;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;
import com.ibm.ws.util.ThreadContextAccessor;

/**
 * JAXWSServiceImpl is used to initial global JAX-RS configurations
 */
@Component(property = { "service.vendor=IBM" }, configurationPolicy = ConfigurationPolicy.OPTIONAL)
public class JaxWsServiceActivator {
    private CXFActivator cxfActivator;
    private HTTPTransportActivator httpTransportActivator;

    private static final TraceComponent tc = Tr.register(JaxWsServiceActivator.class);

    private static final ThreadContextAccessor THREAD_CONTEXT_ACCESSOR = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());

    /*
     * Called by Declarative Services to activate service
     */
    @Activate
    protected void activate(ComponentContext cc) throws Exception {

        //This is a workaroud to avoid invoking createWoodstoxFactory in org.apache.cxf.staxutils.StaxUtils.createXMLInputFactor
        AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
            @Override
            public Boolean run() throws Exception {
                System.setProperty(StaxUtils.ALLOW_INSECURE_PARSER, "1"); // sets the bla property without throwing an exception -> ok
                return Boolean.TRUE;
            }
        });

        if (TraceComponent.isAnyTracingEnabled()) {
            Tr.info(tc, "Set BLA Property: " + StaxUtils.ALLOW_INSECURE_PARSER + " to: " + System.getProperty(StaxUtils.ALLOW_INSECURE_PARSER));
        }

        ClassLoader orignalClassLoader = THREAD_CONTEXT_ACCESSOR.getContextClassLoader(Thread.currentThread());
        try {
            //Make sure the current ThreadContextClassLoader is not an App classloader
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), JaxWsServiceActivator.class.getClassLoader());
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("org.apache.cxf.bus.id", "Default Bus");
            Bus defaultBus = LibertyApplicationBusFactory.getInstance().createBus(null, properties);
            LibertyApplicationBusFactory.setDefaultBus(defaultBus);
        } finally {
            THREAD_CONTEXT_ACCESSOR.setContextClassLoader(Thread.currentThread(), orignalClassLoader);
        }

    }

    /*
     * Called by Declarative Services to deactivate service
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) throws Exception {
        BusFactory.getDefaultBus().shutdown(false);

    }

    @Reference(name = "applicationBusListener", service = LibertyApplicationBusListener.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void registerApplicationBusListener(LibertyApplicationBusListener listener) {
        LibertyApplicationBusFactory.getInstance().registerApplicationBusListener(listener);
    }

    protected void unregisterApplicationBusListener(LibertyApplicationBusListener listener) {
        LibertyApplicationBusFactory.getInstance().unregisterApplicationBusListener(listener);
    }

    /**
     * Register a new extension provier
     *
     * @param provider The extension provider
     */
    @Reference(name = "extensionProvider", service = ExtensionProvider.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected void registerExtensionProvider(ExtensionProvider provider) {
        LibertyApplicationBusFactory.getInstance().registerExtensionProvider(provider);
    }

    /**
     * Unregister the extension provider
     *
     * @param provider The extension provider
     */
    protected void unregisterExtensionProvider(ExtensionProvider provider) {
        LibertyApplicationBusFactory.getInstance().unregisterExtensionProvider(provider);
    }

    public List<Bus> getServerScopedBuses() {
        return LibertyApplicationBusFactory.getInstance().getServerScopedBuses();
    }

    public List<Bus> getClientScopedBuses() {
        return LibertyApplicationBusFactory.getInstance().getClientScopedBuses();
    }
}
