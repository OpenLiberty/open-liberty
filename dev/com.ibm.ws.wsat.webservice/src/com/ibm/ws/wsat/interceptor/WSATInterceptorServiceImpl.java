/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.osgi.service.component.ComponentContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
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
    @Trivial
    public AbstractPhaseInterceptor<Message> getCoorContextOutInterceptor() {
        return new CoorContextOutInterceptor(Phase.WRITE);
    }

    @Override
    @Trivial
    public AbstractPhaseInterceptor<SoapMessage> getCoorContextInInterceptor() {
        return new CoorContextInInterceptor(Phase.PRE_PROTOCOL_FRONTEND);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.jaxws.wsat.components.WSATInterceptorService#getSSLServerInterceptor()
     */
    @Override
    @Trivial
    public AbstractPhaseInterceptor<Message> getSSLServerInterceptor() {
        return new SSLServerInterceptor();
    }

}
