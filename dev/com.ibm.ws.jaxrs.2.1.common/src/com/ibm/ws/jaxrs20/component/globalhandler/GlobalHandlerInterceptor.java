/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.component.globalhandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.cxf.jaxrs.interceptor.JAXRSInInterceptor;
import org.apache.cxf.jaxrs.interceptor.JAXRSOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

import com.ibm.webservices.handler.impl.GlobalHandlerService;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus;
import com.ibm.ws.jaxrs20.bus.LibertyApplicationBus.Type;
import com.ibm.ws.jaxrs20.component.Jaxrs20GlobalHandlerServiceImpl;
import com.ibm.wsspi.webservices.handler.Handler;
import com.ibm.wsspi.webservices.handler.HandlerConstants;

/**
 *
 */
public class GlobalHandlerInterceptor<T extends Message> extends AbstractPhaseInterceptor<T> {
    private final String flow;
    private final LibertyApplicationBus.Type busType;
    private List<Handler> handlersList;

    public GlobalHandlerInterceptor(String phase, String flowType, LibertyApplicationBus.Type bus) {
        super(phase);

        flow = flowType.toLowerCase();
        busType = bus;
        // when in inbound chain, call global handers BEFORE JAXRS SPEC Filter
        if (flowType.equalsIgnoreCase("in")) {
            addBefore(JAXRSInInterceptor.class.getName());
        }
        // when in outbound chain, call global handers AFTER JAXRS SPEC Filter
        if (flowType.equalsIgnoreCase("out")) {
            addAfter(JAXRSOutInterceptor.class.getName());
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message message) {

        handlersList = getHanderList(busType, flow);

        if (handlersList == null || handlersList.size() == 0) {

            return;
        }

        ArrayList<Handler> excutedHandlers = new ArrayList<Handler>();
        JAXRS20MessageContextImpl jaxrsMessageContext = new JAXRS20MessageContextImpl(message);
        jaxrsMessageContext.isServerSide = busType.equals(Type.SERVER);
        jaxrsMessageContext.isClientSide = busType.equals(Type.CLIENT);
        jaxrsMessageContext.setProperty(HandlerConstants.FLOW_TYPE, flow.toUpperCase());
        try {
            Iterator<Handler> it = handlersList.iterator();
            while (it.hasNext()) {
                Handler handler = it.next();
                handler.handleMessage(jaxrsMessageContext);
                excutedHandlers.add(handler);
            }
        } catch (Exception e) {
            for (int i = excutedHandlers.size() - 1; i >= 0; i--) {
                excutedHandlers.get(i).handleFault(jaxrsMessageContext);
            }
        }
    }

    private List<Handler> getHanderList(LibertyApplicationBus.Type busType, String flow) {
        GlobalHandlerService globalHandlerService = Jaxrs20GlobalHandlerServiceImpl.globalHandlerService;

        if (globalHandlerService == null)
            return Collections.emptyList();

        if (busType == Type.SERVER && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
            return globalHandlerService.getJAXRSServerSideInFlowGlobalHandlers();
        }
        if (busType == Type.SERVER && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            return globalHandlerService.getJAXRSServerSideOutFlowGlobalHandlers();
        }
        if (busType == Type.CLIENT && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_IN)) {
            return globalHandlerService.getJAXRSClientSideInFlowGlobalHandlers();
        }
        if (busType == Type.CLIENT && flow.equalsIgnoreCase(HandlerConstants.FLOW_TYPE_OUT)) {
            return globalHandlerService.getJAXRSClientSideOutFlowGlobalHandlers();
        }
        return Collections.emptyList();

    }

}
