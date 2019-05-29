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
package com.ibm.ws.jaxws.monitor;

import org.apache.cxf.Bus;
import org.apache.cxf.management.counters.CounterRepository;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ws.jaxws.bus.LibertyApplicationBusFactory;
import com.ibm.ws.jaxws.bus.LibertyApplicationBusListener;

public class LibertyJaxWsMonitor {

    /*
     * InstrumentationManager has been initialized by CXFActivator when jaxws feature is enabled.
     * No need to construct InstrumentationManager here explicitly.
     */

    private static final LibertyApplicationBusListener MONITOR_INITIALIZER = new LibertyApplicationBusListener() {

        @Override
        public void preInit(Bus bus) {
            if (bus.getExtension(CounterRepository.class) == null) {
                CounterRepository counterRepository = new CounterRepository();
                counterRepository.setBus(bus);
            }
        }

        @Override
        public void initComplete(Bus bus) {}

        @Override
        public void preShutdown(Bus bus) {}

        @Override
        public void postShutdown(Bus bus) {}

    };

    private ServiceTracker monitorServiceTracker;

    /**
     * Register Counter MBean to endpoints in webapps.
     * -Exist web service webapps, iterate webapps and register Counter MBean on them.
     * -Dynamic added web service webapps, register specific initializer
     */
    public LibertyJaxWsMonitor() {}

    /*
     * Called by Declarative Services to activate service
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void activate(ComponentContext cc) {
        BundleContext bundleContext = cc.getBundleContext();
        monitorServiceTracker = new ServiceTracker(bundleContext, "com.ibm.websphere.monitor.MonitorManager", new ServiceTrackerCustomizer() {

            @Override
            public Object addingService(ServiceReference serviceReference) {
                init();
                return this;
            }

            @Override
            public void modifiedService(ServiceReference arg0, Object serviceReference) {}

            @Override
            public void removedService(ServiceReference arg0, Object serviceReference) {
                preDestroy();
            }
        });;
        monitorServiceTracker.open();
    }

    /*
     * Called by Declarative Services to deactivate service
     */
    protected void deactivate(ComponentContext cc) {
        if (monitorServiceTracker != null) {
            monitorServiceTracker.close();
        }
    }

    void init() {
        //For dynamic added webapp, register bus initializer
        LibertyApplicationBusFactory.getInstance().registerApplicationBusListener(MONITOR_INITIALIZER);

        for (Bus bus : LibertyApplicationBusFactory.getInstance().getServerScopedBuses()) {
            if (bus.getExtension(CounterRepository.class) == null) {
                CounterRepository counterRepository = new CounterRepository();
                counterRepository.setBus(bus);
            }
        }
    }

    /**
     * When this monitor component disabled, unregister should be invoked by MonitorManager
     */
    void preDestroy() {
        //Unregister monitor initializer.
        LibertyApplicationBusFactory.getInstance().unregisterApplicationBusListener(MONITOR_INITIALIZER);
    }

}
