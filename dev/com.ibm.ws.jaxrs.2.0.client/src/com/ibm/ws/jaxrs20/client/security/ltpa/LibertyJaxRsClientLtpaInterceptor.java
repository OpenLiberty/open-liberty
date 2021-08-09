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
package com.ibm.ws.jaxrs20.client.security.ltpa;

import javax.servlet.http.Cookie;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.spec.ClientRequestContextImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;
import com.ibm.ws.jaxrs20.client.component.JaxRsAppSecurity;

/**
 * If need more detail, can learn from org.apache.wink.client.handlers LtpaAuthSecurityHandler
 */
public class LibertyJaxRsClientLtpaInterceptor extends AbstractPhaseInterceptor<Message> {
    private static final TraceComponent tc = Tr.register(LibertyJaxRsClientLtpaInterceptor.class);

    public LibertyJaxRsClientLtpaInterceptor() {
        super(Phase.PRE_LOGICAL);
    }

    @Override
    public void handleMessage(Message message) throws Fault {

        //see if ltpa hanlder is used
        Object ltpaHandler = message.get(JAXRSClientConstants.LTPA_HANDLER);
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Please check if customer is using client configuration property: " + JAXRSClientConstants.LTPA_HANDLER + " and should be true");
        }
        if (ltpaHandler != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client configuration property " + JAXRSClientConstants.LTPA_HANDLER + " value is " + ltpaHandler);
            }
            String handler = ((String) ltpaHandler).toLowerCase();
            configClientLtpaHandler(message, handler);
        }

    }

    private void configClientLtpaHandler(Message message, String ltpaHander) {
        if (ltpaHander.equals("true")) {
            ClientRequestContext reqContext = new ClientRequestContextImpl(message, false);

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Entering LtpaAuthSecurityHandler.handle");
            }

            String address = (String) message.get(Message.ENDPOINT_ADDRESS);
            if (address.startsWith("https")) {
                // we will check if user require ssl by using ltpaHandler.setSSLRequired(true) in jaxrs-1.1, but we don't require check ssl in 2.0
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "user is using SSL connection");
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "About to get a LTPA authentication token");
            }

            // retrieve a ltpa cookie from the Subject in current thread 
            try {
                // this interceptor must depend on the appSecurity feature to use WebSecurityHelper.getSSOCookieFromSSOToken()
                Cookie ssoCookie = JaxRsAppSecurity.getSSOCookieFromSSOToken();

                if (ssoCookie != null &&
                    ssoCookie.getValue() != null && !ssoCookie.getValue().isEmpty() &&
                    ssoCookie.getName() != null) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Retrieved a LTPA authentication token. About to set a request cookie: " + ssoCookie.getName() + "=" + ssoCookie.getValue());
                    }
                    //This set cookie method in reqContext may not work, should use message as below to set headers
                    reqContext.getHeaders().putSingle("Cookie", ssoCookie.getName() + "=" + ssoCookie.getValue());
                    //PI56374 resolve map cast to MultivaluedMap exception
//                    Map<String, List<String>> headers = new HashMap<String, List<String>>();
                    MultivaluedMap<String, Object> headers = (MultivaluedMap) message.get(Message.PROTOCOL_HEADERS);
//                    headers.putSingle("Cookie", Arrays.asList(ssoCookie.getName() + "=" + ssoCookie.getValue()));
                    headers.putSingle("Cookie", ssoCookie.getName() + "=" + ssoCookie.getValue());
                    message.put(Message.PROTOCOL_HEADERS, headers);
                } else { // no user credential available
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cannot find a ltpa authentication token off of the thread, you may need enable feature appSecurity-2.0 or ssl-1.0");
                    }
                    //Because this is a client configuration property, we won't throws exception if it doesn't work, please analyze trace for detail
                    //throw new ProcessingException("Cannot find a ltpa authentication token off of the thread");
                }
            } catch (Exception e) {
                throw new ProcessingException(e);
            }

        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "No client ltpa handler configuration, skip");
            }
        }
    }
}