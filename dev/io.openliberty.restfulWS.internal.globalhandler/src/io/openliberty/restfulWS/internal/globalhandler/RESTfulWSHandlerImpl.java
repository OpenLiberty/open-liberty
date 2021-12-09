/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS.internal.globalhandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.webservices.handler.impl.GlobalHandlerService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.webservices.handler.GlobalHandlerMessageContext;
import com.ibm.wsspi.webservices.handler.Handler;



@Component(immediate = true, service = RESTfulWSHandlerImpl.class, property = { "service.vendor=IBM" })
@Provider
public class RESTfulWSHandlerImpl implements ClientRequestFilter, ClientResponseFilter,
                                             ContainerRequestFilter, ContainerResponseFilter {
    private static final TraceComponent tc = Tr.register(RESTfulWSHandlerImpl.class);
    private static Optional<GlobalHandlerService> globalHandlerService;

    @Context
    HttpServletRequest servletRequest;
    
    @Context
    HttpServletResponse servletResponse;

    @Reference(name = "GlobalHandlerService",
               service = GlobalHandlerService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.STATIC,
               policyOption = ReferencePolicyOption.GREEDY)
    protected void setGlobalHandlerService(GlobalHandlerService service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "setGlobalHandlerService " + service);
        }
        globalHandlerService = Optional.of(service);
    }

    protected void unsetGlobalHandlerService(GlobalHandlerService service) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(this, tc, "setGlobalHandlerService " + service);
        }
        globalHandlerService = Optional.empty();
    }

    @Override
    public void filter(ClientRequestContext arg0) throws IOException {
        globalHandlerService.ifPresent(ghs -> {
            GlobalHandlerMessageContext context = new RESTfulGlobalHandlerMessageContext(false, //client
                                                                                         true, //outbound
                                                                                         arg0::getPropertyNames,
                                                                                         arg0::getProperty,
                                                                                         arg0::setProperty,
                                                                                         arg0::removeProperty,
                                                                                         servletRequest,
                                                                                         servletResponse);
            execute(ghs.getJAXRSClientSideOutFlowGlobalHandlers(), context);
        });
    }

    @Override
    public void filter(ClientRequestContext arg0, ClientResponseContext arg1) throws IOException {
        globalHandlerService.ifPresent(ghs -> {
            GlobalHandlerMessageContext context = new RESTfulGlobalHandlerMessageContext(false, //client
                                                                                         false, //inbound
                                                                                         arg0::getPropertyNames,
                                                                                         arg0::getProperty,
                                                                                         arg0::setProperty,
                                                                                         arg0::removeProperty,
                                                                                         servletRequest,
                                                                                         servletResponse);
            execute(ghs.getJAXRSClientSideInFlowGlobalHandlers(), context);
        });
    }

    @Override
    public void filter(ContainerRequestContext arg0) throws IOException {
        globalHandlerService.ifPresent(ghs -> {
            GlobalHandlerMessageContext context = new RESTfulGlobalHandlerMessageContext(true, //server
                                                                                         false, //inbound
                                                                                         arg0::getPropertyNames,
                                                                                         arg0::getProperty,
                                                                                         arg0::setProperty,
                                                                                         arg0::removeProperty,
                                                                                         servletRequest,
                                                                                         servletResponse);
            execute(ghs.getJAXRSServerSideInFlowGlobalHandlers(), context);
        });
    }

    @Override
    public void filter(ContainerRequestContext arg0, ContainerResponseContext arg1) throws IOException {
        globalHandlerService.ifPresent(ghs -> {
            GlobalHandlerMessageContext context = new RESTfulGlobalHandlerMessageContext(true, //server
                                                                                         true, //outbound
                                                                                         arg0::getPropertyNames,
                                                                                         arg0::getProperty,
                                                                                         arg0::setProperty,
                                                                                         arg0::removeProperty,
                                                                                         servletRequest,
                                                                                         servletResponse);
            execute(ghs.getJAXRSServerSideOutFlowGlobalHandlers(), context);
        });
    }

    private void execute(List<Handler> handlers, GlobalHandlerMessageContext context) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "execute", new Object[]{handlers, context});
        }

        List<Handler> executedHandlers = new ArrayList<Handler>();
        try {
            for (Handler handler : handlers) {
                handler.handleMessage(context);
                executedHandlers.add(handler);
            }
        } catch (Exception e) {
            for (int i = executedHandlers.size() - 1; i >= 0; i--) {
                executedHandlers.get(i).handleFault(context);
            }
        }
    }
}
