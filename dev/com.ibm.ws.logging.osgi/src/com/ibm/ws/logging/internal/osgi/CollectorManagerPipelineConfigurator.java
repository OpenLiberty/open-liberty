/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
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
import com.ibm.ws.logging.WsLogHandler;
import com.ibm.ws.logging.WsTraceHandler;
import com.ibm.ws.logging.source.LogSource;
import com.ibm.ws.logging.source.TraceSource;
import com.ibm.ws.logging.utils.CollectorManagerPipelineUtils;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;


public class CollectorManagerPipelineConfigurator {

    /**
     * A reference to the OSGI bundle framework.
     */
    private BundleContext bundleContext = null;
    private CollectorManagerPipelineUtils collectorMgrPipelineUtils = null;
    /**
     * The ServiceListener interface. Invoked by OSGI whenever a ServiceReference changes state.
     * Receives CollectorManager events and performs appropriate logic to set up the log and trace pipeline 
     * for use by future handlers.
     */
    private final ServiceListener collectorManagerListener = new ServiceListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void serviceChanged(ServiceEvent event) {
            switch (event.getType()) {
                case ServiceEvent.REGISTERED:
                	setCollectorManagerPipeline((ServiceReference<CollectorManager>) event.getServiceReference());
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
    public CollectorManagerPipelineConfigurator(BundleContext context) {
    	collectorMgrPipelineUtils = CollectorManagerPipelineUtils.getInstance();
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

    private Dictionary<String,String> returnSourceServiceProps(){
    	Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
    	serviceProperties.put("service.vendor", "IBM");
    	serviceProperties.put("id", "ANALYTICSLOGSOURCE");
    	return serviceProperties;
    }
    
    private Dictionary<String,String> returnConduitServiceProps(String sourceName){
    	Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
    	serviceProperties.put("source", sourceName);
    	return serviceProperties;
    }
    
    private Dictionary<String,String> returnHandlerServiceProps(){
    	Dictionary<String, String> serviceProperties = new Hashtable<String, String>();
    	serviceProperties.put("service.vendor", "IBM");
    	return serviceProperties;
    }
    
    protected void setCollectorManagerPipeline(ServiceReference<CollectorManager> ref) {
   	
    	//Retrieves the LogSource and TraceSource from the collectorManagerPipelineUtils singleton
        LogSource logSource = collectorMgrPipelineUtils.getLogSource();
        TraceSource traceSource = collectorMgrPipelineUtils.getTraceSource();

        //Retrieves the LogConduit and TraceConduit from the collectorManagerPipelineUtils singleton
    	BufferManagerImpl logConduit = collectorMgrPipelineUtils.getLogConduit();
    	BufferManagerImpl traceConduit = collectorMgrPipelineUtils.getTraceConduit();
        
    	//Register the Conduits/BufferManager with their 'source' property set to the type of source they are intended for.
    	bundleContext.registerService(BufferManager.class.getName(), logConduit, returnConduitServiceProps(logSource.getSourceName()));
    	bundleContext.registerService(BufferManager.class.getName(), traceConduit, returnConduitServiceProps(traceSource.getSourceName()));

        //Register the LogSource and TraceSource as Source
    	bundleContext.registerService(new String[] {Source.class.getName()}, logSource, returnSourceServiceProps());
    	bundleContext.registerService(new String[] {Source.class.getName()}, traceSource, returnSourceServiceProps());
    	/* Retrieve and register the Handler, if it exists. JsonTraceService creates it and sets it
    	 * into the collectorManagerPipelineUtils for retrieval now.
    	 * 
    	 * The responsibility of responsibly  tying the 'pipeline' together belongs with CollectorManager.
    	 * This includes successfully merging the source and conduit/bufferManager with 'other' Handlers 
    	 * (i.e. LogStashCollector, LogMetCollector)
    	 */
    	Handler messageLoghandler = collectorMgrPipelineUtils.getMessageLogHandler();
    	if (messageLoghandler != null){
    		bundleContext.registerService(Handler.class.getName(), messageLoghandler, returnHandlerServiceProps());
    	}
    		
    	Handler consoleLoghandler = collectorMgrPipelineUtils.getConsoleLogHandler();
    	if (consoleLoghandler != null){
    		bundleContext.registerService(Handler.class.getName(), consoleLoghandler, returnHandlerServiceProps());
    	}
    	
    }


    protected void unsetCollectorManagerHandler(ServiceReference<CollectorManager> ref) {
        //Do nothing, we want to keep LogSource and TraceSource and conduit actively registered
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
            	setCollectorManagerPipeline(servRef);
            }
        }
    }
}
