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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.components.WSATInterceptorService;
import com.ibm.ws.wsat.utils.WSCoorConstants;

public class WSATInterceptorServiceImpl implements WSATInterceptorService {

    private static final TraceComponent tc = Tr.register(
                                                         WSATInterceptorServiceImpl.class, WSCoorConstants.TRACE_GROUP, null);

    protected void activate(ComponentContext cc) {

    }

    protected void deactivate(ComponentContext cc) {

    }

    @Override
    public AbstractPhaseInterceptor<Message> getCoorContextOutInterceptor() {
        return new CoorContextOutInterceptor(Phase.WRITE);
    }

    @Override
    public AbstractPhaseInterceptor<SoapMessage> getCoorContextInInterceptor() {
        return new CoorContextInInterceptor(Phase.PRE_PROTOCOL_FRONTEND);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.jaxws.wsat.components.WSATInterceptorService#getSSLServerInterceptor()
     */
    @Override
    public AbstractPhaseInterceptor<Message> getSSLServerInterceptor() {
        return new SSLServerInterceptor();
    }

}
