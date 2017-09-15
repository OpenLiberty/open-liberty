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
package com.ibm.webservices.handler.impl;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceSet;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

/**
 * Core of file global handler service. Looks for registered global handlers (via declarative services).
 * 
 */
@Component(immediate = true, property = { "service.vendor=IBM" })
public class GlobalHandlerServiceImpl implements GlobalHandlerService {
    static final String HANDLER = "Handler";

    static final TraceComponent tc = Tr.register(GlobalHandlerServiceImpl.class);

    private final ConcurrentServiceReferenceSet<Handler> jaxwsInFlowClientSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxwsOutFlowClientSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxwsInFlowServerSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxwsOutFlowServerSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxrsInFlowClientSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxrsInFlowServerSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxrsOutFlowClientSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);

    private final ConcurrentServiceReferenceSet<Handler> jaxrsOutFlowServerSideGlobalHandlers =
                    new ConcurrentServiceReferenceSet<Handler>(KEY_HANDLERS);
    private static final AtomicInteger saajCount = new AtomicInteger(0);

    public CopyOnWriteArrayList<Handler> jaxrsInFlowClientSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxrsInFlowServerSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxrsOutFlowClientSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxrsOutFlowServerSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxwsInFlowClientSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxwsOutFlowClientSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxwsInFlowServerSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();
    public CopyOnWriteArrayList<Handler> jaxwsOutFlowServerSideGlobalHandlers1 = new CopyOnWriteArrayList<Handler>();

    private volatile ComponentContext cContext = null;
    static final String KEY_HANDLERS = "Handler";

    /**
     * DS-driven component activation
     */
    @Activate
    protected void activate(ComponentContext cContext, Map<String, Object> properties) throws Exception {
        this.cContext = cContext;
        jaxwsInFlowClientSideGlobalHandlers.activate(cContext);
        jaxwsOutFlowClientSideGlobalHandlers.activate(cContext);
        jaxwsInFlowServerSideGlobalHandlers.activate(cContext);
        jaxwsOutFlowServerSideGlobalHandlers.activate(cContext);

        jaxrsInFlowClientSideGlobalHandlers.activate(cContext);
        jaxrsOutFlowClientSideGlobalHandlers.activate(cContext);
        jaxrsInFlowServerSideGlobalHandlers.activate(cContext);
        jaxrsOutFlowServerSideGlobalHandlers.activate(cContext);

        clearAllList();
        //jaxws

        Iterator<Handler> registeredHandlers = jaxwsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxwsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }
        //jaxrs
        registeredHandlers = jaxrsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxrsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

    }

    /**
     * DS-driven de-activation
     */
    @Deactivate
    protected void deactivate(ComponentContext cc) {

        this.cContext = null;
        jaxwsInFlowClientSideGlobalHandlers.deactivate(cContext);
        jaxwsOutFlowClientSideGlobalHandlers.deactivate(cContext);
        jaxwsInFlowServerSideGlobalHandlers.deactivate(cContext);
        jaxwsOutFlowServerSideGlobalHandlers.deactivate(cContext);

        jaxrsInFlowClientSideGlobalHandlers.deactivate(cContext);
        jaxrsOutFlowClientSideGlobalHandlers.deactivate(cContext);
        jaxrsInFlowServerSideGlobalHandlers.deactivate(cContext);
        jaxrsOutFlowServerSideGlobalHandlers.deactivate(cContext);

    }

    @Reference(service = Handler.class, policy = ReferencePolicy.DYNAMIC, cardinality = ReferenceCardinality.MULTIPLE)
    protected synchronized void setHandler(ServiceReference<Handler> handlerrRef) {
        String engineType = handlerrRef.getProperty(HandlerConstants.ENGINE_TYPE) == null ? HandlerConstants.ENGINE_TYPE_ALL : (String) handlerrRef.getProperty(HandlerConstants.ENGINE_TYPE);
        String flowType = handlerrRef.getProperty(HandlerConstants.FLOW_TYPE) == null ? HandlerConstants.FLOW_TYPE_INOUT : (String) handlerrRef.getProperty(HandlerConstants.FLOW_TYPE);
        boolean serverSide = handlerrRef.getProperty(HandlerConstants.IS_SERVER_SIDE) == null ? true : Boolean.parseBoolean(handlerrRef.getProperty(HandlerConstants.IS_SERVER_SIDE).toString());
        boolean clientSide = handlerrRef.getProperty(HandlerConstants.IS_CLIENT_SIDE) == null ? true : Boolean.parseBoolean(handlerrRef.getProperty(HandlerConstants.IS_CLIENT_SIDE).toString());

        String importPackage = handlerrRef.getBundle().getHeaders("").get("Import-package");
        String dynamicImportPackage = handlerrRef.getBundle().getHeaders("").get("DynamicImport-Package");

        if ((importPackage != null && importPackage.contains("javax.xml.ws.handler.soap")) ||
            (dynamicImportPackage != null && dynamicImportPackage.contains("javax.xml.ws.handler.soap")))
        {
            saajCount.getAndIncrement();
        }

        if (engineType.equalsIgnoreCase(HandlerConstants.ENGINE_TYPE_JAXWS)) {
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxwsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxwsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxwsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxwsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxwsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxwsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxwsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxwsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }

        } else if (engineType.equalsIgnoreCase(HandlerConstants.ENGINE_TYPE_JAXRS)) {

            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxrsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxrsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxrsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxrsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxrsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxrsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }

        } else if (engineType.equalsIgnoreCase(HandlerConstants.ENGINE_TYPE_ALL)) {
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxwsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }

            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxwsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxrsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (serverSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxwsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxwsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxrsOutFlowServerSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowServerSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
                jaxwsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
                jaxwsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxrsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }
            if (clientSide && flowType.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_INOUT)) {
                jaxwsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxwsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxrsOutFlowClientSideGlobalHandlers.addReference(handlerrRef);
                jaxrsInFlowClientSideGlobalHandlers.addReference(handlerrRef);
            }

        }

        clearAllList();

        //jaxws
        Iterator<Handler> registeredHandlers = jaxwsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxwsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }
        //jaxrs
        registeredHandlers = jaxrsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxrsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

    }

    /**
     * 
     */
    private synchronized void clearAllList() {
        jaxwsInFlowServerSideGlobalHandlers1.clear();
        jaxwsOutFlowServerSideGlobalHandlers1.clear();
        jaxwsInFlowClientSideGlobalHandlers1.clear();
        jaxwsOutFlowClientSideGlobalHandlers1.clear();

        jaxrsOutFlowServerSideGlobalHandlers1.clear();
        jaxrsInFlowServerSideGlobalHandlers1.clear();
        jaxrsOutFlowClientSideGlobalHandlers1.clear();
        jaxrsInFlowClientSideGlobalHandlers1.clear();

    }

    /**
     * Create a monitor holder for the given FileMonitor. The type of holder we create will
     * depend
     * 
     * @param handlerRef
     * @return
     */

    protected synchronized void unsetHandler(ServiceReference<Handler> handlerRef) {

        jaxwsInFlowClientSideGlobalHandlers.removeReference(handlerRef);
        jaxwsOutFlowClientSideGlobalHandlers.removeReference(handlerRef);
        jaxwsInFlowServerSideGlobalHandlers.removeReference(handlerRef);
        jaxwsOutFlowServerSideGlobalHandlers.removeReference(handlerRef);

        jaxrsInFlowClientSideGlobalHandlers.removeReference(handlerRef);
        jaxrsOutFlowClientSideGlobalHandlers.removeReference(handlerRef);
        jaxrsInFlowServerSideGlobalHandlers.removeReference(handlerRef);
        jaxrsOutFlowServerSideGlobalHandlers.removeReference(handlerRef);

        String importPackage = handlerRef.getBundle().getHeaders("").get("Import-package");
        String dynamicImportPackage = handlerRef.getBundle().getHeaders("").get("DynamicImport-Package");

        if ((importPackage != null && importPackage.contains("javax.xml.ws.handler.soap")) ||
            (dynamicImportPackage != null && dynamicImportPackage.contains("javax.xml.ws.handler.soap")))
        {
            saajCount.getAndDecrement();
        }

        clearAllList();

        //jaxws
        Iterator<Handler> registeredHandlers = jaxwsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxwsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxwsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxwsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }
        //jaxrs
        registeredHandlers = jaxrsOutFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowServerSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowServerSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowServerSideGlobalHandlers1.add(registeredHandlers.next());
        }

        registeredHandlers = jaxrsOutFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsOutFlowClientSideGlobalHandlers1.add(0, registeredHandlers.next());
        }

        registeredHandlers = jaxrsInFlowClientSideGlobalHandlers.getServices();
        while (registeredHandlers.hasNext()) {
            jaxrsInFlowClientSideGlobalHandlers1.add(registeredHandlers.next());
        }

    }

    @Override
    public List<Handler> getJAXWSServerSideInFlowGlobalHandlers() {
        return jaxwsInFlowServerSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXWSClientSideInFlowGlobalHandlers() {
        return jaxwsInFlowClientSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXWSServerSideOutFlowGlobalHandlers() {

        return jaxwsOutFlowServerSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXWSClientSideOutFlowGlobalHandlers() {

        return jaxwsOutFlowClientSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXRSServerSideInFlowGlobalHandlers() {

        return jaxrsInFlowServerSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXRSClientSideInFlowGlobalHandlers() {

        return jaxrsInFlowClientSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXRSServerSideOutFlowGlobalHandlers() {

        return jaxrsOutFlowServerSideGlobalHandlers1;
    }

    @Override
    public List<Handler> getJAXRSClientSideOutFlowGlobalHandlers() {

        return jaxrsOutFlowClientSideGlobalHandlers1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getSaajFlag() {
        boolean result = saajCount.get() > 0 ? true : false;
        return result;
    }

}