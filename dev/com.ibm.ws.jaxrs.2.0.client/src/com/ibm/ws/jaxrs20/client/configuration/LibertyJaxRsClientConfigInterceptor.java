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

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.osgi.HttpConduitConfigApplier;
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


        Conduit conduit = message.getExchange().getConduit(message);

        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = ((HTTPConduit) conduit);

            Dictionary<String, String> cxfClientProps = new Hashtable<>();
            for (Map.Entry<String, Object> entry : message.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("client.")) {
                    Object valueObj = entry.getValue();
                    String value = entry == null ? null : valueObj.toString();
                    cxfClientProps.put(key, value);
                }
            }
            HttpConduitConfigApplier.applyClientPolicies(cxfClientProps, httpConduit);

            // IBM KeepAlive Constant takes priority over CXF constant - reset if necessary
            Object keepAlive = message.get(JAXRSClientConstants.KEEP_ALIVE_CONNECTION);
            HTTPClientPolicy clientPolicy = httpConduit.getClient();
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
            } else if (!message.containsKey("client.Connection")) {
                // nothing specified - use the default of KEEP_ALIVE
                clientPolicy.setConnection(ConnectionType.KEEP_ALIVE);
            }

            //IBM Connect/Receive timeouts take priority over CXF timeouts - reset here if necessary
            Object connection_timeout = message.get(JAXRSClientConstants.CONNECTION_TIMEOUT);
            Object receive_timeout = message.get(JAXRSClientConstants.RECEIVE_TIMEOUT);
            configClientTimeout(httpConduit, connection_timeout, receive_timeout);
        }
    }

    private void configClientTimeout(HTTPConduit httpConduit, Object connection_timeout, Object receive_timeout) {

        try {
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
