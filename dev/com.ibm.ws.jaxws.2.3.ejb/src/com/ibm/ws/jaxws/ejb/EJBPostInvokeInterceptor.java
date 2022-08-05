/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.ejb;

import java.rmi.RemoteException;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.ejb.internal.JaxWsEJBConstants;
import com.ibm.wsspi.ejbcontainer.WSEJBEndpointManager;

/**
 *
 */
public class EJBPostInvokeInterceptor extends AbstractPhaseInterceptor<SoapMessage> {

    private static final TraceComponent tc = Tr.register(EJBPostInvokeInterceptor.class);

    public EJBPostInvokeInterceptor() {
//        super(Phase.POST_PROTOCOL_ENDING);
        super(Phase.WRITE);
    }

    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        Exchange exchange = message.getExchange();

        // retrieve the ejb endpoint manager
        WSEJBEndpointManager endpointManager = (WSEJBEndpointManager) exchange.get(JaxWsEJBConstants.WS_EJB_ENDPOINT_MANAGER);

        if (endpointManager != null) {
            try {
                endpointManager.ejbPostInvoke();
            } catch (RemoteException e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Exception occurred when attempting ejbPostInvoke: " + e.getMessage());
                }
                throw new IllegalStateException(e);
            }
        }

    }
}
