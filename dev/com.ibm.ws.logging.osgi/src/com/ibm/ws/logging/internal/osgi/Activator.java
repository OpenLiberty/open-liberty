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

import org.eclipse.equinox.log.ExtendedLogReaderService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.TraceComponentChangeListener;
import com.ibm.ws.ras.instrument.internal.main.LibertyJava8WorkaroundRuntimeTransformer;
import com.ibm.ws.ras.instrument.internal.main.LibertyRuntimeTransformer;

/**
 * Activator for the RAS/FFDC bundle.
 */
public class Activator implements BundleActivator {

    private MessageRouterConfigurator msgRouter;
    private TraceRouterConfigurator traceRouter;
    private CollectorManagerPipelineConfigurator collectorMgrPipeConfigurator; 

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

        // The LogService comes from the framework and is always there;
        // We also never remove our listener because that will be done automatically when 
        // we are stopped.
        LogReaderService logReader = context.getService(context.getServiceReference(ExtendedLogReaderService.class));
        logReader.addLogListener(new TrOSGiLogForwarder());

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
        
        collectorMgrPipeConfigurator = new CollectorManagerPipelineConfigurator(context);
    }

    /**
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop(BundleContext context) throws Exception {
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

}
