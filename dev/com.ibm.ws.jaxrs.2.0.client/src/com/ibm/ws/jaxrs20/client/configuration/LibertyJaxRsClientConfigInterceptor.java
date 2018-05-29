/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.configuration;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

/**
 *
 */
public class LibertyJaxRsClientConfigInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientConfigInterceptor.class);

    /**
     * @param phase
     */
    public LibertyJaxRsClientConfigInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        Object keepAlive = message.get(JAXRSClientConstants.KEEP_ALIVE_CONNECTION);
        Conduit conduit = message.getExchange().getConduit(message);

        if (conduit instanceof HTTPConduit) {
            HTTPClientPolicy clientPolicy = ((HTTPConduit) conduit).getClient();
            if (keepAlive != null) {
                String keepAliveString = keepAlive.toString();
                switch (keepAliveString.toLowerCase()) {
                    case "keep-alive":
                    case "keepalive":
                        clientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
                        break;
                    case "close":
                        clientPolicy.setConnection(ConnectionType.CLOSE);
                        break;
                    default:
                        Tr.warning(tc, "warn_unknown_keepalive_setting", keepAliveString, "keep-alive, close");
                        clientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
                }
            } else {
                // nothing specified - use the default of KEEP_ALIVE
                clientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
            }
        }
    }
}
