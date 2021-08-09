/*******************************************************************************
 * Copyright (c) 2012, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.ibm.websphere.ras.TrConfigurator;
import com.ibm.ws.logging.WsTraceHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;

/**
 * This class scans the existing services and registers itself as a ServiceListener,
 * looking/listening for LogHandlers services. As soon as the first LogHandler is
 * discovered, this class creates the WsTraceRouterImpl and "injects" the LogHandler(s)
 * into it.
 * 
 * In other words, this class acts as a pseudo-injector of services and config for
 * the WsTraceRouterImpl. I can't just register the WsTraceRouterImpl as a DS service
 * component, because the DS injection stuff happens late in init, and we want the
 * TraceRouter up as early as possible.
 */
public class TraceRouterConfigurator {

    /**
     * A reference to the OSGI bundle framework.
     */
    private BundleContext bundleContext = null;

    /**
     * A reference to the actual MessageRouter.
     */
    private WsTraceRouterImpl traceRouter = null;

    /**
     * The ServiceListener interface. Invoked by OSGI whenever a ServiceReference changes state.
     * Receives WsLogHandler events and passes them along to WsTraceRouterImpl.
     */
    private final ServiceListener wsTraceHandlerListener = new ServiceListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void serviceChanged(ServiceEvent event) {
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                    setWsTraceHandler((ServiceReference<WsTraceHandler>) event.getServiceReference());
                    break;
                case ServiceEvent.UNREGISTERING:
                    unsetWsTraceHandler((ServiceReference<WsTraceHandler>) event.getServiceReference());
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * First, this guy registers itself as a ServiceListener, listening specifically for
     * LogHandler services. Second, it scans the current set services looking for any LogHandler
     * services that are already active.
     * 
     * @param context The BundleContext.
     */
    public TraceRouterConfigurator(BundleContext context) {
        bundleContext = context;

        try {
            // Register ServiceListeners, to be informed when new LogHandler and WsLogHandler services are registered.
            bundleContext.addServiceListener(wsTraceHandlerListener, "(" + Constants.OBJECTCLASS + "=com.ibm.ws.logging.WsTraceHandler)");

            processInitialTraceHandlerServices();

        } catch (InvalidSyntaxException ise) {
            // This should really never happen.  Blow up if it does.
            throw new RuntimeException(ise);
        }
    }

    /**
     * Add the LogHandler ref. 1 or more LogHandlers may be set.
     * This method is called from the ServiceListener.
     */
    protected void setWsTraceHandler(ServiceReference<WsTraceHandler> ref) {
    	getTraceRouter().setWsTraceHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Remove the LogHandler ref.
     * This method is called from the ServiceListener.
     */
    protected void unsetWsTraceHandler(ServiceReference<WsTraceHandler> ref) {
        getTraceRouter().unsetWsTraceHandler((String) ref.getProperty("id"), bundleContext.getService(ref));
    }

    /**
     * Search for and add any LogHandler ServiceReferences that were already started
     * by the time we registered our ServiceListener.
     */
    @SuppressWarnings("unchecked")
    protected void processInitialTraceHandlerServices() throws InvalidSyntaxException {
        ServiceReference<WsTraceHandler>[] servRefs = (ServiceReference<WsTraceHandler>[])
                        bundleContext.getServiceReferences(WsTraceHandler.class.getName(), null);

        if (servRefs != null) {
            for (ServiceReference<WsTraceHandler> servRef : servRefs) {
                setWsTraceHandler(servRef);
            }
        }
    }

    /**
     * Lazy activation and retrieval of the MessageRouter.
     */
    protected WsTraceRouterImpl getTraceRouter() {
        if (traceRouter == null) {
            // First activation.
            traceRouter = WsTraceRouterSingleton.singleton;

            // Pass the MessageRouter to the TrService via the TrConfigurator.
            TrConfigurator.setTraceRouter(traceRouter);
        }

        return traceRouter;
    }

    /**
     * http://www.cs.umd.edu/~pugh/java/memoryModel/DoubleCheckedLocking.html
     * ... tells me to do it this way.
     */
    private static class WsTraceRouterSingleton {
        public static final WsTraceRouterImpl singleton = new WsTraceRouterImpl();
    }

    /**
     * The bundle is stopping. Inform the TrConfigurator, who in turn will inform
     * the TrService.
     */
    public void stop() {
        if (traceRouter != null) {
            TrConfigurator.unsetTraceRouter(traceRouter);
        }
    }
}
