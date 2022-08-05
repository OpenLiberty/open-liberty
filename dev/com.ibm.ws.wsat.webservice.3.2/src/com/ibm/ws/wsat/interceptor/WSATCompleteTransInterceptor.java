/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.interceptor;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.ws.addressing.ContextUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.jaxws.bus.LibertyApplicationBus.Type;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.utils.WSATOSGIService;
import com.ibm.ws.wsat.utils.WSCoorConstants;
import com.ibm.ws.wsat.utils.WSCoorUtil;

/**
 *
 */
public class WSATCompleteTransInterceptor extends AbstractPhaseInterceptor<Message> {

    final TraceComponent tc = Tr.register(
                                          WSATCompleteTransInterceptor.class, WSCoorConstants.TRACE_GROUP, null);

    private final Type busType;

    /**
     * @param phase
     */
    public WSATCompleteTransInterceptor(String phase, Type type) {
        super(phase);
        this.busType = type;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        boolean isOutbound = ContextUtils.isOutbound(message);
        try {
            WSCoorUtil.checkHandlerServiceReady();
            if (!isOutbound)
            {
                if (busType == Type.CLIENT) {
                    final Handler handler = WSATOSGIService.getInstance().getHandlerService();

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc,
                                 "handleMessage",
                                 "Execute handleClientResponse in Client Inbound Interceptor, " + Message.RESPONSE_CODE + "="
                                                 + message.get(Message.RESPONSE_CODE));

                    handler.handleClientResponse();
                }
            }
            else
            {
                if (busType == Type.SERVER) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        Tr.debug(tc, "handleMessage",
                                 "Execute handleServerResponse in Server Outbound Interceptor");
                    WSATOSGIService.getInstance().getHandlerService().handleServerResponse();
                }
            }
        } catch (WSATException e) {
            FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.WSATCompleteTransInterceptor", "102");
            throw new Fault(e);
        } finally {
            // Fix defect 204023: Remove this interceptor after handle for EACH endpoint
            Exchange ex = message.getExchange();
            Endpoint ep = ex.get(Endpoint.class);
            if (ep != null) {
                if (busType == Type.SERVER) {
                    ep.getOutInterceptors().remove(this);
                } else if (busType == Type.CLIENT) {
                    ep.getInInterceptors().remove(this);
                }
            }
        }
    }

    @Override
    public void handleFault(Message message) {
        WSCoorUtil.checkHandlerServiceReady();
        boolean isOutbound = ContextUtils.isOutbound(message);
        try {
            if (!isOutbound) {
                WSATOSGIService.getInstance().getHandlerService().handleClientFault();
            } else {
                WSATOSGIService.getInstance().getHandlerService().handleServerFault();
            }
        } catch (Throwable e) {
            FFDCFilter.processException(e, "com.ibm.ws.wsat.interceptor.WSATCompleteTransInterceptor", "119");
            throw new Fault(e);
        }
    }

}
