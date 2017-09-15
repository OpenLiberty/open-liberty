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
package com.ibm.ws.jaxrs20.client.configuration;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

/**
 *
 */
public class LibertyJaxRsClientTimeOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientTimeOutInterceptor.class);

    /**
     * @param phase
     */
    public LibertyJaxRsClientTimeOutInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        Object connection_timeout = message.get(JAXRSClientConstants.CONNECTION_TIMEOUT);
        Object receive_timeout = message.get(JAXRSClientConstants.RECEIVE_TIMEOUT);
        Conduit conduit = message.getExchange().getConduit(message);

        configClientTimeout(conduit, connection_timeout, receive_timeout);

    }

    private void configClientTimeout(Conduit conduit, Object connection_timeout, Object receive_timeout) {

        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = (HTTPConduit) conduit;

            try {
                //httpConduit.getClient(message).setConnectionTimeout(Long.parseLong(timeout));
                if (connection_timeout != null) {
                    String sconnection_timeout = connection_timeout.toString();
                    httpConduit.getClient().setConnectionTimeout(Long.parseLong(sconnection_timeout));
                }

            } catch (NumberFormatException e) {
                //The time out value that user specified in the property on JAX-RS Client side is invalid, set the time out value to default.
                httpConduit.getClient().setConnectionTimeout(JAXRSClientConstants.TIMEOUT_DEFAULT);

                Tr.error(tc, "error.jaxrs.client.configuration.timeout.valueinvalid", connection_timeout, JAXRSClientConstants.CONNECTION_TIMEOUT,
                         JAXRSClientConstants.TIMEOUT_DEFAULT, e.getMessage());

            }
            try {
                if (receive_timeout != null) {
                    String sreceive_timeout = receive_timeout.toString();
                    httpConduit.getClient().setReceiveTimeout(Long.parseLong(sreceive_timeout));
                }
            } catch (NumberFormatException e) {

                httpConduit.getClient().setReceiveTimeout(JAXRSClientConstants.TIMEOUT_DEFAULT);
                Tr.error(tc, "error.jaxrs.client.configuration.timeout.valueinvalid", receive_timeout, JAXRSClientConstants.RECEIVE_TIMEOUT, JAXRSClientConstants.TIMEOUT_DEFAULT,
                         e.getMessage());

            }

        }
    }
}
