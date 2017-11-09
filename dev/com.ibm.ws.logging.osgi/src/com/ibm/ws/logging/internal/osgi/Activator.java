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

package com.ibm.ws.logging.internal.osgi;

import java.lang.instrument.Instrumentation;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TraceComponentChangeListener;
import com.ibm.ws.logging.utils.HandlerUtils;
import com.ibm.ws.ras.instrument.internal.main.LibertyJava8WorkaroundRuntimeTransformer;
import com.ibm.ws.ras.instrument.internal.main.LibertyRuntimeTransformer;
import com.ibm.wsspi.collector.manager.Source;

/**
 * Activator for the RAS/FFDC bundle.
 */
public class Activator implements BundleActivator, ServiceTrackerCustomizer<EventAdmin, EventAdmin> {

    private BundleContext bContext;
    private TrLogImpl logImpl;
    private TrLogServiceFactory logSvcFactory;
    private ServiceTracker<EventAdmin, EventAdmin> eventAdminTracker;
    private MessageRouterConfigurator msgRouter;
    private TraceRouterConfigurator traceRouter;
    private CollectorManagerConfigurator cmConfigurator; //DYKC

    private LoggingConfigurationService logCfgService;

    private Instrumentation inst;
    private RuntimeTransformerComponentListener runtimeTransformer = null;

    private static TraceComponentChangeListenerTracker listenerTracker = null;

    /**
     * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
     * @see com.ibm.liberty.kernel.internal.Activator#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start(BundleContext context) throws Exception {
        bContext = context;

        // We must register OSGi log services: if none is present, third party
        // bundles will log directly to stdout (metatype, scr, etc.)

        // Create the core of our service provider: 
        // TrLogImpl also acts as a service factory for LogReaderService
        logImpl = new TrLogImpl();
        context.registerService("org.osgi.service.log.LogReaderService", logImpl, getProperties());

        // Create and register the log service factory
        // TrLogServiceFactory also contains a listener for bundle/framework/service events
        logSvcFactory = new TrLogServiceFactory(logImpl, context.getBundle(0));
        context.registerService("org.osgi.service.log.LogService", logSvcFactory, getProperties());

        // 96353: Register the framework, service, and bundle listeners using
        // the system bundle's context so that events from regions other than
        // kernel are visible.
        BundleContext sysContext = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        sysContext.addBundleListener(logSvcFactory.getListener());
        sysContext.addFrameworkListener(logSvcFactory.getListener());
        sysContext.addServiceListener(logSvcFactory.getListener());

        eventAdminTracker = new ServiceTracker<EventAdmin, EventAdmin>(context, EventAdmin.class, this);
        eventAdminTracker.open();

        // Instrumentation, when available, is registered by the launcher
        // and will never go away.
        ServiceReference<Instrumentation> instReference = context.getServiceReference(Instrumentation.class);
        if (instReference != null) {
            inst = context.getService(instReference);
            LibertyRuntimeTransformer.setInstrumentation(inst);
            LibertyJava8WorkaroundRuntimeTransformer.setInstrumentation(inst);
        } else {
            LibertyRuntimeTransformer.setInstrumentation(null);
            LibertyJava8WorkaroundRuntimeTransformer.setInstrumentation(null);
        }

        // Register an adapter for the RuntimeTransformer to get notifications
        // from Tr (outside of the framework/in a different bundle) that a 
        // TraceComponent has changed
        runtimeTransformer = new RuntimeTransformerComponentListener();
        TrConfigurator.addTraceComponentListener(runtimeTransformer);

        // Track all registered TraceComponentChangeListeners regardless of 
        // classloader reachability
        listenerTracker = new TraceComponentChangeListenerTracker(context);
        listenerTracker.open(true);

        // Register a managed service for dynamic configuration changes
        logCfgService = new LoggingConfigurationService(context, (inst != null));

        // Create the MessageRouterConfigurator to ...
        // 1. create the MessageRouter and install it into the non-OSGI side of logging
        // 2. track bundles coming and going and process their MessageRouter.properties (if it exists)
        // 3. track LogHandler services and inject them into the MessageRouter
        msgRouter = new MessageRouterConfigurator(context);
        traceRouter = new TraceRouterConfigurator(context);
        
        //DYKC-CollectorManagerConfigurator, maybe a better name?
        cmConfigurator = new CollectorManagerConfigurator(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        eventAdminTracker.close();

        // 96353: Explicitly remove listeners using the system bundle's context.
        BundleContext sysContext = context.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        sysContext.removeServiceListener(logSvcFactory.getListener());
        sysContext.removeFrameworkListener(logSvcFactory.getListener());
        sysContext.removeBundleListener(logSvcFactory.getListener());

        logImpl.stop();
        msgRouter.stop();
        traceRouter.stop();

        // stop the TR service (listening for config changes)
        if (this.logCfgService != null) {
            this.logCfgService.stop();
            this.logCfgService = null;
        }

        TrConfigurator.removeTraceComponentListener(runtimeTransformer);
        listenerTracker.close();
    }

    public static class RuntimeTransformerComponentListener implements TraceComponentChangeListener {
        @Override
        public void traceComponentRegistered(TraceComponent tc) {}

        @Override
        public void traceComponentUpdated(TraceComponent tc) {
            LibertyRuntimeTransformer.traceStateChanged(tc);
            //The java8 workaround transformer doesn't need to respond to this since
            //it's not doing dynamic injection.
        }
    }

    /** @return Dictionary containing minimal default service properties */
    public Dictionary<String, ?> getProperties() {
        Hashtable<String, Object> h = new Hashtable<String, Object>();
        h.put("service.vendor", "IBM");
        h.put("service.ranking", Integer.valueOf(1));
        return h;
    }

    /** {@inheritDoc} */
    @Override
    public EventAdmin addingService(ServiceReference<EventAdmin> reference) {
        EventAdmin service = bContext.getService(reference);
        logImpl.setEventAdmin(service);
        return service;
    }

    /** {@inheritDoc} */
    @Override
    public void modifiedService(ServiceReference<EventAdmin> reference, EventAdmin service) {}

    /** {@inheritDoc} */
    @Override
    public void removedService(ServiceReference<EventAdmin> reference, EventAdmin service) {
        logImpl.setEventAdmin(null);
    }
}
