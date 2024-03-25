/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.interceptor;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.remote.RemoteTransactionController;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus;
import com.ibm.ws.jaxws.wsat.Constants.AssertionStatus;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATUtil;
import com.ibm.ws.wsat.utils.WSCoorConstants;
import com.ibm.ws.wsat.utils.WSCoorUtil;
import com.ibm.ws.wsat.webservice.client.wscoor.CoordinationContext;

/**
 *
 */
public class CoorContextOutInterceptor extends AbstractPhaseInterceptor<Message> {

    final TraceComponent tc = Tr.register(
                                          CoorContextOutInterceptor.class, WSCoorConstants.TRACE_GROUP, null);
    private AssertionStatus isOptional;

    private final RemoteTransactionController tranService = getService(RemoteTransactionController.class);

    private <T> T getService(Class<T> service) {
        T impl = null;
        BundleContext context = FrameworkUtil.getBundle(service).getBundleContext();
        ServiceReference<T> ref = context.getServiceReference(service);
        if (ref != null) {
            impl = context.getService(ref);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Unable to locate service: {0}", service);
            }
            //throw new WSATException("Cannot locate service " + service);
        }
        return impl;
    }

    /**
     * @param phase
     */
    public CoorContextOutInterceptor(String phase) {
        super(phase);
        getBefore().add(SoapOutInterceptor.class.getName());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        if (!WSCoorUtil.assertAT(message)) {
            isOptional = AssertionStatus.FALSE;
        } else {
            isOptional = WSCoorUtil.isOptional(message, false);
        }

        boolean inTrans = false;

        WSCoorUtil.checkHandlerServiceReady();

        inTrans = WSCoorUtil.getHandlerService().isTranActive();
        if (inTrans) {
            SoapHeader header = null;
            JAXBDataBinding dataBinding = null;

            if (message.getExchange().isOneWay()) {
                throw new Fault(new WSATException("WS-AT can not work on ONE-WAY webservice method"));
            }

            try {
                String regHost = WSCoorUtil.getConfigService().getWSATUrl()
                                 + "/"
                                 + WSCoorConstants.COORDINATION_REGISTRATION_ENDPOINT;

                EndpointReferenceType localRegEpr = WSATUtil.createEpr(regHost, tranService != null ? tranService.getRecoveryId() : null);

                WSATContext ctx = WSCoorUtil.getHandlerService().handleClientRequest();

                CoordinationContext cc = WSCoorUtil.createCoordinationContext(ctx, localRegEpr);
                dataBinding = new JAXBDataBinding(CoordinationContext.class);
                QName qname = new QName(WSCoorConstants.NAMESPACE_WSCOOR, WSCoorConstants.COORDINATION_CONTEXT_ELEMENT_STRING);
                header = new SoapHeader(qname, cc, dataBinding);
                header.setMustUnderstand(true);

                WSATCompleteTransInterceptor _comTransIn = new WSATCompleteTransInterceptor(Phase.PRE_PROTOCOL, LibertyApplicationBus.Type.CLIENT);
                Exchange ex = message.getExchange();
                Endpoint ep = ex.get(Endpoint.class);
                ep.getInInterceptors().add(_comTransIn);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(
                             tc,
                             "handleMessage",
                             "Generate a new CoordinationContext", header.toString(),
                             header.getName());
                }
                //Abandon using AbstractSoapInterceptor
                //List<Header> headers = message.getHeaders();
                @SuppressWarnings("unchecked")
                ArrayList<Header> headers = (ArrayList<Header>) message.get(WSCoorConstants.SOAP_HEADER_KEY);
                headers.add(header);
            } catch (Throwable e) {
                FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.CoorContextOutInterceptor", "119");
                throw new Fault(e);
            }
        } else {
            if (isOptional == AssertionStatus.FALSE)
                throw new Fault(new WSATException("Detected WS-AT policy, however there is no active transaction in current thread."));
        }
    }

    @Override
    public void handleFault(Message message) {
        WSCoorUtil.checkHandlerServiceReady();
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc,
                         "handleFault",
                         message,
                         new Exception("Stack trace for CoorContextOutInterceptor.handleFault()"));
            }
            WSCoorUtil.getHandlerService().handleClientFault();
        } catch (WSATException e) {
            FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.CoorContextOutInterceptor", "201");
        }
    }
}
