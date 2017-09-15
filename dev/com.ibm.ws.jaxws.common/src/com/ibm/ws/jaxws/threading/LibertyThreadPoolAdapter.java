/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.threading;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.cxf.Bus;
import org.apache.cxf.workqueue.AutomaticWorkQueue;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;
import com.ibm.wsspi.kernel.service.utils.AtomicServiceReference;
import com.ibm.wsspi.threading.WSExecutorService;

/**
 * LibertyThreadPool Adapter will register an initializer for set the AutomaticWorkQueue for each created bus instance.
 */
public class LibertyThreadPoolAdapter {

    private static final TraceComponent tc = Tr.register(LibertyThreadPoolAdapter.class);

    private static final AtomicServiceReference<WSExecutorService> executorServiceRef = new AtomicServiceReference<WSExecutorService>("executorService");

    private static final AtomicServiceReference<ScheduledExecutorService> scheduledExecutorServiceRef = new AtomicServiceReference<ScheduledExecutorService>("scheduledExecutorService");

    private AutomaticWorkQueue automaticeWorkQueue;

    private LibertyAutomaticWorkQueueBusListener workQueueBusListener;

    /*
     * Called by Declarative Services to activate service
     */
    protected void activate(ComponentContext cc) {
        executorServiceRef.activate(cc);
        scheduledExecutorServiceRef.activate(cc);

        WSExecutorService executorService = executorServiceRef.getService();
        ScheduledExecutorService scheduledExecutorService = scheduledExecutorServiceRef.getService();
        if (executorService != null && scheduledExecutorService != null) {
            automaticeWorkQueue = new LibertyJaxWsAutomaticWorkQueueImpl(scheduledExecutorService, executorService);
            workQueueBusListener = new LibertyAutomaticWorkQueueBusListener();
            LibertyApplicationBusFactory.getInstance().registerApplicationBusListener(workQueueBusListener);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "LibertyAutomaticWorkQueueBusListener is registered into LibertyApplicationBusFactory");
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "LibertyAutomaticWorkQueueBusListener is not registered into LibertyApplicationBusFactory due to executorService {0} or scheduledExecutorService {1}",
                         (executorService == null ? "NULL" : "NOT NULL"), (scheduledExecutorService == null ? "NULL" : "NOT NULL"));
            }
        }
    }

    /*
     * Called by Declarative Services to modify service configuration properties
     */
    protected void modified(Map<String, Object> newProps) {}

    /*
     * Called by Declarative Services to deactivate service
     */
    protected void deactivate(ComponentContext cc) {
        executorServiceRef.deactivate(cc);
        scheduledExecutorServiceRef.deactivate(cc);

        if (workQueueBusListener != null) {
            LibertyApplicationBusFactory.getInstance().unregisterApplicationBusListener(workQueueBusListener);
        }
    }

    public void setExecutorService(ServiceReference<WSExecutorService> reference) {
        executorServiceRef.setReference(reference);
    }

    public void unsetExecutorService(ServiceReference<WSExecutorService> reference) {
        executorServiceRef.unsetReference(reference);
    }

    public void setScheduledExecutorService(ServiceReference<ScheduledExecutorService> reference) {
        scheduledExecutorServiceRef.setReference(reference);
    }

    public void unsetScheduledExecutorService(ServiceReference<ScheduledExecutorService> reference) {
        scheduledExecutorServiceRef.unsetReference(reference);
    }

    class LibertyAutomaticWorkQueueBusListener implements LibertyApplicationBusListener {

        @Override
        public void preInit(Bus bus) {}

        @Override
        public void initComplete(Bus bus) {
            if (automaticeWorkQueue != null) {
                bus.getExtension(WorkQueueManager.class).addNamedWorkQueue("default", automaticeWorkQueue);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The default automticWorkQueue is added to bus " + bus.getId());
                }
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "The default automticWorkQueue is NOT added to bus " + bus.getId() + " due to null automaticWorkQueue");
                }
            }
        }

        @Override
        public void preShutdown(Bus bus) {}

        @Override
        public void postShutdown(Bus bus) {}

    }
}
