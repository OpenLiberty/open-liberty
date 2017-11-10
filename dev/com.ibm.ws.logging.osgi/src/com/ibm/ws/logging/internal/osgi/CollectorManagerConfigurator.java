/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.logging.internal.osgi;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.utils.HandlerUtils;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Source;


public class CollectorManagerConfigurator {

    /**
     * A reference to the OSGI bundle framework.
     */
    private BundleContext bundleContext = null;
    private HandlerUtils myHandlerUtils = null;
    /**
     * The ServiceListener interface. Invoked by OSGI whenever a ServiceReference changes state.
     * Receives WsLogHandler events and passes them along to WsTraceRouterImpl.
     */
    private final ServiceListener collectorManagerListener = new ServiceListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void serviceChanged(ServiceEvent event) {
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                	setCollectorManagerHandler((ServiceReference<CollectorManager>) event.getServiceReference());
                    break;
                case ServiceEvent.UNREGISTERING:
                    unsetCollectorManagerHandler((ServiceReference<CollectorManager>) event.getServiceReference());
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * First, this guy registers itself as a ServiceListener, listening specifically for
     * CollectorManager services. Second, it scans the current set services looking for any CollectorManager
     * services that are already active.
     * 
     * @param context The BundleContext.
     */
    public CollectorManagerConfigurator(BundleContext context) {
    	myHandlerUtils = HandlerUtils.getInstance();
        bundleContext = context;

        try {
            // Register ServiceListeners, to be informed when new CollectorManager services are registered.
            bundleContext.addServiceListener(collectorManagerListener, "(" + Constants.OBJECTCLASS + "=com.ibm.wsspi.collector.manager.CollectorManager)");

            processInitialCollectorManagerServices();

        } catch (InvalidSyntaxException ise) {
            // This should really never happen.  Blow up if it does.
            throw new RuntimeException(ise);
        }
    }

    private Dictionary<String,String> returnProps(){
    	Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
    	serviceProperties.put("service.vendor", "IBM");
    	serviceProperties.put("id", "ANALYTICSLOGSOURCE");
    	return serviceProperties;
    }
    
    protected void setCollectorManagerHandler(ServiceReference<CollectorManager> ref) {
   	
        System.out.println("CollectorManagerConfigurator.java - setCollectorManagerHandler()");
        Source logSource = myHandlerUtils.getLogSource();
        Source traceSource = myHandlerUtils.getTraceSource();
        System.out.println("CollectorManagerConfigurator.java - I got a Log Source " + logSource);
        System.out.println("CollectorManagerConfigurator.java - I got a Trace Source " + traceSource);
        

    	bundleContext.registerService(Source.class.getName(), logSource, returnProps());
    	bundleContext.registerService(Source.class.getName(), traceSource, returnProps());
        
    	BufferManagerImpl logConduit = myHandlerUtils.getLogConduit();
    	BufferManagerImpl traceConduit = myHandlerUtils.getTraceConduit();
    	
    	
    	//DYKC-TODO Need to do something regarding setting BufferManager/Conduit into CollectorManager?!?
    	//i.e. register it?
    	
//DYKC     _______ ____    _____   ____  
//    	  |__   __/ __ \  |  __ \ / __ \ 
//    	     | | | |  | | | |  | | |  | |
//    	     | | | |  | | | |  | | |  | |
//    	     | | | |__| | | |__| | |__| |
//    	     |_|  \____/  |_____/ \____/ 

    }


    protected void unsetCollectorManagerHandler(ServiceReference<CollectorManager> ref) {
        //do nothing, we want to keep LogSource and TraceSource and conduit actively registered?
    }

    /**
     * Search for and add any LogHandler ServiceReferences that were already started
     * by the time we registered our ServiceListener.
     */
    @SuppressWarnings("unchecked")
    protected void processInitialCollectorManagerServices() throws InvalidSyntaxException {
        ServiceReference<CollectorManager>[] servRefs = (ServiceReference<CollectorManager>[])
                        bundleContext.getServiceReferences(CollectorManager.class.getName(), null);

        if (servRefs != null) {
            for (ServiceReference<CollectorManager> servRef : servRefs) {
            	setCollectorManagerHandler(servRef);
            }
        }
    }
}
