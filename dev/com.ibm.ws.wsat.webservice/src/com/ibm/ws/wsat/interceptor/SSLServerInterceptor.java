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

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.jaxws.wsat.components.WSATConfigService;
import com.ibm.ws.wsat.utils.WSATOSGIService;
import com.ibm.ws.wsat.utils.WSCoorConstants;

/**
 *
 */
public class SSLServerInterceptor extends AbstractPhaseInterceptor<Message> {

    final TraceComponent tc = Tr.register(
                                          SSLServerInterceptor.class, WSCoorConstants.TRACE_GROUP, null);

    private static final String PEER_CERTIFICATES = "javax.net.ssl.peer_certificates";

    public SSLServerInterceptor() {
        super(Phase.RECEIVE);
        getAfter().add(Constants.WS_INTERCEPTOR_CLASSNAME);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.cxf.interceptor.Interceptor#handleMessage(org.apache.cxf.message.Message)
     */
    @Override
    public void handleMessage(Message msg) throws Fault {
        WSATConfigService s = WSATOSGIService.getInstance().getConfigService();
        if (null == s) {
            throw new Fault("WSAT configuration service is not avaliable", tc.getLogger());
        }
        if (s.isSSLEnabled() && s.isClientAuthEnabled()) {
            HttpServletRequest request = (HttpServletRequest) msg.get(AbstractHTTPDestination.HTTP_REQUEST);
            X509Certificate certChain[] = (X509Certificate[]) request.getAttribute(PEER_CERTIFICATES);
            if (null == certChain || 0 == certChain.length) {
                throw new Fault("NOT be able to get any certificate to verify, the certificate from client is either INVALID or NULL", tc.getLogger());
            }
        }

    }
}
