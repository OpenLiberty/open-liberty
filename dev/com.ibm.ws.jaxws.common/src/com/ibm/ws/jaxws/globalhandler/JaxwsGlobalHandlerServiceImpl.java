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
package com.ibm.ws.jaxws.globalhandler;

import java.util.Map;

import org.apache.cxf.Bus;
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
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;

/**
 * Core of file global handler service. Looks for registered global handlers (via declarative services).
 * 
 */
@Component(immediate = true, property = { "service.vendor=IBM" })
public class JaxwsGlobalHandlerServiceImpl {

    static final TraceComponent tc = Tr.register(JaxwsGlobalHandlerServiceImpl.class);

    private volatile ComponentContext cContext = null;

    private JaxWSGlobalHandlerBusListener listener = null;

    public static final AtomicServiceReference<GlobalHandlerService> globalHandlerServiceSR =
                    new AtomicServiceReference<GlobalHandlerService>("GlobalHandlerService");

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) throws Exception {
        this.cContext = cContext;
        globalHandlerServiceSR.activate(cContext);
        listener = new JaxWSGlobalHandlerBusListener();
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

    class JaxWSGlobalHandlerBusListener implements LibertyApplicationBusListener {

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

            if (globalHandlerServiceSR.getService() != null) {

                //In Flow
                GlobalHandlerInterceptor globalHandlerInInterceptor = new GlobalHandlerInterceptor(Phase.PRE_PROTOCOL_FRONTEND, "in", busType);
                bus.getInInterceptors().add(globalHandlerInInterceptor);

                //Out Flow

                GlobalHandlerEntryOutInterceptor globalHandlerOutEntryInterceptor = new GlobalHandlerEntryOutInterceptor("out", busType);
                bus.getOutInterceptors().add(globalHandlerOutEntryInterceptor);

            }
        }

        @Override
        public void preShutdown(Bus bus) {}

        @Override
        public void postShutdown(Bus bus) {}

    }

}