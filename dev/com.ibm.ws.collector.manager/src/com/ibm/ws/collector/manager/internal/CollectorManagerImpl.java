/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.internal;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;
import com.ibm.ws.logging.collector.CollectorConstants;
import com.ibm.wsspi.collector.manager.BufferManager;
import com.ibm.wsspi.collector.manager.CollectorManager;
import com.ibm.wsspi.collector.manager.Handler;
import com.ibm.wsspi.collector.manager.Source;
import com.ibm.wsspi.collector.manager.SynchronousHandler;

public class CollectorManagerImpl implements CollectorManager {

    private BundleContext bundleContext;
    private ServiceRegistration<?> buffMgrRegistration;
    private final Map<String, ServiceRegistration<?>> activeBuffMgrServices = new HashMap<String, ServiceRegistration<?>>();
    private final Map<String, BufferManager> bufferManagerMap = new HashMap<String, BufferManager>();

    private static final TraceComponent tc = Tr.register(CollectorManagerImpl.class);

    /* Reference to configuration admin for creating source instances */
    private ConfigurationAdmin configAdmin;

    /* Map of bound sources and their PIDs */
    private final Map<String, String> sourcePids = new HashMap<String, String>();

    /* Map of bound sources */
    private final Map<String, SourceManager> sourceMgrs = new HashMap<String, SourceManager>();

    /* Map of bound handlers */
    private final Map<String, HandlerManager> handlerMgrs = new HashMap<String, HandlerManager>();

    protected void activate(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Activating " + this);
        }
    }

    protected void deactivate(int reason) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, " Deactivating " + this, " reason = " + reason);
        }
        //Unregister all BufferManagers created. This will also deactivate sources.
        unregisterAllBufferManagers();
    }

    protected void modified(Map<String, Object> configuration) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, " Modified");
    }

    protected synchronized void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    protected synchronized void unsetConfigurationAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = null;
    }

    /*
     * OSGi runtime calls this to notify collector manager when a new source provider
     * becomes available. This method will be used by the collector manager to bind sources.
     * When a source is bound, handle all pending subscriptions for the source.
     */
    public synchronized void setSource(Source source) {
        String sourceId = CollectorManagerUtils.getSourceId(source);
        SourceManager srcMgr = null;
        if (!sourceMgrs.containsKey(sourceId)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Adding source to the list", source.getClass());
            }
            srcMgr = new SourceManager(source);
            sourceMgrs.put(srcMgr.getSourceId(), srcMgr);
            /*
             * Obtain the conduit/bufferManager and put it in bufferManagerMap
             * if the source being set is message/log or trace.
             *
             * This is to make up for the prior logic where conduit/bufferManager
             * were created before the source was created and were set into the bufferManagerMap
             *
             * With the new logic concerning LogSource and TraceSource for JsonLogging, the LogSource
             * and TraceSource and their respective Conduit/BufferManager were created outside of
             * CollectorManager and OSGI serviceability. Thus, the following call to processInitializedConduits
             * retrieves the conduits/bufferManagers that were created 'outside' collectorManager's realm of
             * control and places them into bufferManagerMap because the following logic expects the prior
             * statement to be true.
             *
             * Alas, continue 'as normal' afterwards.
             */
            processInitializedConduits(source);

            //Passes BufferManager onto SourceManager which will then associate a Handler to it.
            srcMgr.setBufferManager(bufferManagerMap.get(sourceId));
            //Handle pending subscriptions for this source
            for (Entry<String, HandlerManager> entry : handlerMgrs.entrySet()) {
                HandlerManager hdlrMgr = entry.getValue();
                //Check if source is in the pending subscription list for this handler
                if (hdlrMgr.getPendingSubscriptions().contains(sourceId)) {
                    List<String> sourceIds = new ArrayList<String>();
                    sourceIds.add(sourceId);
                    try {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "Handling pending subscription " + sourceId,
                                     hdlrMgr.getHandlerId());
                        }
                        subscribe(hdlrMgr.getHandler(), sourceIds);
                    } catch (Exception e) {

                    }
                }
            }
        }
    }

    /*
     * Process Conduits created by Trace and Message due to the CollectorManagerPipelineConfigurator
     * and CollectorManagerPipelineUtils.
     */
    private void processInitializedConduits(Source source) {
        String sourceId = CollectorManagerUtils.getSourceId(source);
        String sourceName = source.getSourceName();
        /*
         * Check that the current source initialized is Log/Message and Trace.
         * These are the unique special sources along with their respective Conduits/BufferManagers
         * that were started before OSGI and thusly were not configured via 'the osgi' way that CollectorManager
         * had in place before the JSON Logging work.
         */
        if (sourceName.equals(CollectorConstants.MESSAGES_SOURCE) || sourceName.equals(CollectorConstants.TRACE_SOURCE)) {
            //Make sure we have a bundleContext, we need this to play with osgi (i.e. register/listen to services)
            if (bundleContext == null) {
                retrieveBundleContext();
            }
            /*
             * We should really check if the source and conduit/bufferManager is already set into the
             * SourceManager and BufferManager maps. If so, no need to continue the rest of this method.
             */
            if (sourceMgrs.containsKey(sourceId) && bufferManagerMap.containsKey(sourceId)) {
                return;
            }

            ServiceReference<BufferManager>[] servRefs;
            try {
                servRefs = (ServiceReference<BufferManager>[]) bundleContext.getServiceReferences(BufferManager.class.getName(), null);
                if (servRefs != null) {
                    for (ServiceReference<BufferManager> servRef : servRefs) {
                        /*
                         * Ensure that the Conduit/BufferManager retrieved is the right one for the source
                         * by checking that the 'source' that the conduit/bufferManager was assinged to
                         * was for the Source that we are registering.
                         */
                        if (sourceName.equals(servRef.getProperty("source"))) {
                            /*
                             * Retrieve the actual Conduit/BufferManager fro mthe service Reference
                             * and put it into the bufferManagerMap.
                             */
                            Object object = bundleContext.getService(servRef);
                            BufferManager conduit = (BufferManager) object;
                            bufferManagerMap.put(sourceId, conduit);
                        }
                    }
                }

            } catch (InvalidSyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * OSGi runtime calls this method to notify the collector manager when an existing source provider
     * is no longer available. This method will be used by the collector manager to un-bind sources.
     * When a source is unbound, change all its subscriptions to pending subscriptions.
     */
    public synchronized void unsetSource(Source source) {
        String sourceId = CollectorManagerUtils.getSourceId(source);
        SourceManager srcMgr = null;
        if (sourceMgrs.containsKey(sourceId)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Removing source from the list", source.getClass());
            }
            srcMgr = sourceMgrs.get(sourceId);
            /*
             * Change all subscriptions to pending subscriptions
             * Need to make a copy to avoid a ConcurrentModification Exception when unsubscribe is called
             * and subsequent call to SrcMgr to remove the handler from the subscriptions list.
             */
            Set<String> srcMgrSubscriptions = new HashSet<String>(srcMgr.getSubscriptions());
            for (String handlerId : srcMgrSubscriptions) {
                List<String> sourceIds = new ArrayList<String>();
                sourceIds.add(sourceId);
                try {
                    HandlerManager hdlrMgr = handlerMgrs.get(handlerId);
                    Handler handler = hdlrMgr.getHandler();
                    unsubscribe(handler, sourceIds);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Source not available, adding to pending subscription list " + sourceId,
                                 handlerId);
                    }
                    hdlrMgr.addPendingSubscription(sourceId);
                } catch (Exception e) {

                }
            }
            srcMgr.unsetSource(source);
            sourceMgrs.remove(sourceId);
        }
    }

    /*
     * OSGi runtime calls this to notify collector manager when a new handler provider
     * becomes available. This method will be used by the collector manager to bind handlers.
     * When a handler is bound, call the init method of the handler.
     */
    public synchronized void setHandler(Handler handler) {
        String handlerId = CollectorManagerUtils.getHandlerId(handler);
        HandlerManager hdlrMgr;
        if (!handlerMgrs.containsKey(handlerId)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Adding handler to the list", handler.getClass());
            }
            hdlrMgr = new HandlerManager(handler);
            handlerMgrs.put(hdlrMgr.getHandlerId(), hdlrMgr);
            hdlrMgr.getHandler().init(this);
        }
    }

    /*
     * OSGi runtime calls this method to notify the collector manager when an existing handler provider
     * is no longer available. This method will be used by the collector manager to un-bind handlers.
     * When a handler is un-bound all its subscriptions are terminated.
     */
    public synchronized void unsetHandler(Handler handler) {
        String handlerId = CollectorManagerUtils.getHandlerId(handler);
        HandlerManager hdlrMgr;
        if (handlerMgrs.containsKey(handlerId)) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Removing handler from the list", handler.getClass());
            }
            hdlrMgr = handlerMgrs.get(handlerId);
            //Terminate all subscriptions
            try {
                unsubscribe(handler, new ArrayList<String>(hdlrMgr.getSubsribedSources()));
            } catch (Exception e) {

            }
            hdlrMgr.unsetHandler(handler);
            handlerMgrs.remove(handlerId);
        }
    }

    @Override
    public synchronized void subscribe(Handler handler, List<String> sourceIds) throws Exception {
        if (sourceIds != null) {
            String handlerId = CollectorManagerUtils.getHandlerId(handler);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Subscribe to sources " + sourceIds, handlerId);
            }
            //Only bound handlers are allowed to subscribe
            if (handlerId != null && !handlerMgrs.containsKey(handlerId)) {
                throw new Exception("Handler not bound : " + handlerId);
            }
            for (String sourceId : sourceIds) {
                HandlerManager hdlrMgr = handlerMgrs.get(handlerId);
                //Check if the source is available, if so do relevant bootstrapping
                if (sourceMgrs.containsKey(sourceId) && sourceMgrs.get(sourceId).getSource() != null) {
                    SourceManager srcMgr = sourceMgrs.get(sourceId);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Subscribing to source " + sourceId, handlerId);
                    }
                    srcMgr.addSubscriber(handler);

                    //This does nothing for a SynchronousHandler
                    handler.setBufferManager(sourceId, srcMgr.getBufferManager());

                    //Add as subscribed source to the handler's Handler Manager
                    hdlrMgr.addSubscribedSource(srcMgr.getSource());

                } else {
                    //Source is not available
                    //Add this source to the pending subscription list of this handler
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Source not available, adding to pending subscription list " + sourceId,
                                 handlerId);
                    }
                    hdlrMgr.addPendingSubscription(sourceId);
                    //Go ahead start a BufferManager which will activate the source
                    startSourceWithBufferManager(sourceId, handler);
                }
            }
        }
    }

    @Override
    public synchronized void unsubscribe(Handler handler, List<String> sourceIds) throws Exception {
        if (sourceIds != null) {
            String handlerId = CollectorManagerUtils.getHandlerId(handler);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Unsubscribe from sources" + sourceIds, handlerId);
            }
            //Only bound handlers are allowed to unsubscribe
            if (handlerId != null && !handlerMgrs.containsKey(handlerId)) {
                throw new Exception("Handler not bound : " + handlerId);
            }
            for (String sourceId : sourceIds) {
                //Check if the source is available
                if (sourceMgrs.containsKey(sourceId) && sourceMgrs.get(sourceId).getSource() != null) {
                    SourceManager srcMgr = sourceMgrs.get(sourceId);

                    //This does nothing for a SynchronousHandler
                    handler.unsetBufferManager(sourceId, srcMgr.getBufferManager());

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unsubscribing from source " + sourceId, handlerId);
                    }

                    HandlerManager hdlrMgr = handlerMgrs.get(handlerId);
                    hdlrMgr.removeSubscribedSource(srcMgr.getSource());

                    /*
                     * We need to unregister the service if the handler unsubscribing is the last handler.
                     * This will deactivate the Source.
                     */
                    if (srcMgr.removeSubscriber(handler)) {
                        ServiceRegistration<?> entry = activeBuffMgrServices.get(sourceId);
                        if (entry != null) {
                            entry.unregister();
                            activeBuffMgrServices.remove(sourceId);
                        }
                    }

                    // This can lead to issues, so the clean up will only happen in the deactivate method.
                    //if (srcMgr.getSubscriptions().isEmpty()) {
                    //No subscribers for this source, destroy the source instance
                    //    destroySourceInstance(sourceId);
                    //}
                }
            }
        }
    }

    private synchronized void startSourceWithBufferManager(String sourceId, Handler handler) {
        /*
         * An active Buffer already exists. This must mean we subscribed two handlers wanting
         * the same source really quickly back-to-back before the source was set into CollectorManager
         *
         * Also if it is audit, there is no point of creating a bufferManager here, it won't start up the audit source.
         */
        if (activeBuffMgrServices.containsKey(sourceId) || sourceId.contains(CollectorConstants.AUDIT_LOG_SOURCE)) {
            return;
        }
        //result[0] is sourceName
        //result[1] is location
        String[] result = sourceId.split("\\|");
        if (result.length != 2 || result[0].equals("") || result[1].equals("")) {
            //One of the required fields source name or location is missing, throw an error.
            throw new IllegalArgumentException("Incorrect source identifier format : " + sourceId);
        }
        Dictionary<String, String> props = new Hashtable<String, String>();

        //The 'source' property is used by Source classes to filter if this BufferManager service is applicable for them.
        props.put("source", result[0]);

        BufferManagerImpl bufferMgr = new BufferManagerImpl(10000, sourceId);

        if (handler instanceof SynchronousHandler) {
            bufferMgr.addSyncHandler((SynchronousHandler) handler);
        }

        //Add BufferManager into a Map. This will be retrieved later to be passed onto a SourceManager so that it can associate a Handler to it.
        bufferManagerMap.put(sourceId, bufferMgr);

        //Create the BufferManager Service and store the ServiceRegistration into a Map so that the service can be unregistered later.
        if (bundleContext == null) {
            retrieveBundleContext();
        }
        buffMgrRegistration = bundleContext.registerService(BufferManager.class.getName(), bufferMgr, props);
        activeBuffMgrServices.put(sourceId, buffMgrRegistration);
    }

    private synchronized void unregisterAllBufferManagers() {
        Map<String, ServiceRegistration<?>> shutDownCopyBuffMgrService = new HashMap<String, ServiceRegistration<?>>(activeBuffMgrServices);
        activeBuffMgrServices.clear();
        for (ServiceRegistration<?> entry : shutDownCopyBuffMgrService.values()) {
            if (entry != null) {
                entry.unregister();
            }
        }
        shutDownCopyBuffMgrService.clear();
    }

    private synchronized void retrieveBundleContext() {
        bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    }
}
