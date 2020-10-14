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
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.ConnectionType;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.transports.http.configuration.ProxyServerType;

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
            applyClientPolicies(cxfClientProps, httpConduit);

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

    // This method was "lifted" and modified slightly from CXF's HttpConduitConfigApplier
    private static void applyClientPolicies(Dictionary<String, String> d, HTTPConduit c) {
        Enumeration<String> keys = d.keys();
        HTTPClientPolicy p = c.getClient();
        while (keys.hasMoreElements()) {
            String k = keys.nextElement();
            if (k.startsWith("client.")) {
                if (p == null) {
                    p = new HTTPClientPolicy();
                    c.setClient(p);
                }
                String v = d.get(k);
                k = k.substring("client.".length());
                if ("ConnectionTimeout".equals(k)) {
                    p.setConnectionTimeout(Long.parseLong(v.trim()));
                } else if ("ReceiveTimeout".equals(k)) {
                    p.setReceiveTimeout(Long.parseLong(v.trim()));
                } else if ("AsyncExecuteTimeout".equals(k)) {
                    p.setAsyncExecuteTimeout(Long.parseLong(v.trim()));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The Liberty threadpool maximum length is infinite.  The AsyncExecuteTimeout property will be ignored.");
                    }
                } else if ("AsyncExecuteTimeoutRejection".equals(k)) {
                    p.setAsyncExecuteTimeoutRejection(Boolean.parseBoolean(v.trim()));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "The Liberty threadpool maximum length is infinite.  The AsyncExecuteTimeoutRejection property will be ignored.");
                    }
                } else if ("AutoRedirect".equals(k)) {
                    p.setAutoRedirect(Boolean.parseBoolean(v.trim()));
                } else if ("MaxRetransmits".equals(k)) {
                    p.setMaxRetransmits(Integer.parseInt(v.trim()));
                } else if ("AllowChunking".equals(k)) {
                    p.setAllowChunking(Boolean.parseBoolean(v.trim()));
                } else if ("ChunkingThreshold".equals(k)) {
                    p.setChunkingThreshold(Integer.parseInt(v.trim()));
                } else if ("ChunkLength".equals(k)) {
                    p.setChunkLength(Integer.parseInt(v.trim()));
                } else if ("Connection".equals(k)) {
                    p.setConnection(ConnectionType.valueOf(v));
                } else if ("DecoupledEndpoint".equals(k)) {
                    p.setDecoupledEndpoint(v);
                } else if ("ProxyServer".equals(k)) {
                    p.setProxyServer(v);
                } else if ("ProxyServerPort".equals(k)) {
                    p.setProxyServerPort(Integer.parseInt(v.trim()));
                } else if ("ProxyServerType".equals(k)) {
                    p.setProxyServerType(ProxyServerType.fromValue(v));
                } else if ("NonProxyHosts".equals(k)) {
                    p.setNonProxyHosts(v);
                }
            }
        }
    }
}
