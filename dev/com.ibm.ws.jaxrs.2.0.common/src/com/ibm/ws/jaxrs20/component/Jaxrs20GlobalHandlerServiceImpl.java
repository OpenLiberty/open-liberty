/*******************************************************************************
 * Copyright (c) 2011, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.component;

import java.util.Map;

import org.apache.cxf.Bus;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.webservices.handler.impl.GlobalHandlerService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBusListener;
import com.ibm.ws.jaxrs20.component.globalhandler.GlobalHandlerInterceptor;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Core of file global handler service. Looks for registered global handlers (via declarative services).
 * 
 */
@Component(immediate = true, property = { "service.vendor=IBM" })
public class Jaxrs20GlobalHandlerServiceImpl {

    static final TraceComponent tc = Tr.register(Jaxrs20GlobalHandlerServiceImpl.class);

    private volatile ComponentContext cContext = null;

    private JaxRSGlobalHandlerBusListener listener = null;

    public final AtomicServiceReference<GlobalHandlerService> globalHandlerServiceSR =
                    new AtomicServiceReference<GlobalHandlerService>("GlobalHandlerService");

    public static GlobalHandlerService globalHandlerService = null;

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) throws Exception {
        this.cContext = cContext;
        globalHandlerServiceSR.activate(cContext);
        listener = new JaxRSGlobalHandlerBusListener();
        LibertyApplicationBusFactory.getInstance().registerApplicationBusListener(listener);
    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {
        globalHandlerServiceSR.deactivate(cc);

        this.cContext = null;

        if (listener != null) {
            LibertyApplicationBusFactory.getInstance().unregisterApplicationBusListener(listener);
        }

    }

    @Reference(service = GlobalHandlerService.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.OPTIONAL)
    protected void setGlobalHandlerService(ServiceReference<GlobalHandlerService> serviceRef) {
        globalHandlerServiceSR.setReference(serviceRef);

    }

    protected void unsetGlobalHandlerService(ServiceReference<GlobalHandlerService> serviceRef) {

        globalHandlerServiceSR.unsetReference(serviceRef);
    }

    class JaxRSGlobalHandlerBusListener implements LibertyApplicationBusListener {

        @Override
        public void preInit(Bus bus) {}

        @Override
        public void initComplete(Bus bus) {

            if (bus == null) {
                return;
            }

            LibertyApplicationBus.Type busType = bus.getExtension(LibertyApplicationBus.Type.class);
            if (busType == null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Unable to recognize the bus type from bus, Global handlers will not be registered and execeuted");
                }
                return;
            }

            if (globalHandlerService == null) {
                globalHandlerService = globalHandlerServiceSR.getService();
            }

            if (globalHandlerService != null) {

                //In Flow
                GlobalHandlerInterceptor<Message> globalHandlerInInterceptor = new GlobalHandlerInterceptor<Message>(Phase.UNMARSHAL, "in", busType);
                bus.getInInterceptors().add(globalHandlerInInterceptor);

                //Out Flow
                GlobalHandlerInterceptor<Message> globalHandlerOutInterceptor = new GlobalHandlerInterceptor<Message>(Phase.MARSHAL, "out", busType);
                bus.getOutInterceptors().add(globalHandlerOutInterceptor);

            }
        }

        @Override
        public void preShutdown(Bus bus) {}

        @Override
        public void postShutdown(Bus bus) {}

    }

}