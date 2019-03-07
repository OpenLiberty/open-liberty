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
package com.ibm.ws.microprofile.rest.client.component;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Hashtable;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 *
 */
public class LibertyFTTimeoutInterceptor extends AbstractPhaseInterceptor<Message>  {
    private static final TraceComponent tc = Tr.register(LibertyFTTimeoutInterceptor.class);

    public LibertyFTTimeoutInterceptor(String phase) {
        super(phase);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        Map<String, Object> filterProps = (Map<String, Object>) message.getExchange().get("jaxrs.filter.properties");
        if (filterProps == null) {
            return;
        }
        Method method = (Method) filterProps.get("org.eclipse.microprofile.rest.client.invokedMethod");
        if (method == null) {
            return;
        }

        Timeout timeoutAnnotation = method.getAnnotation(Timeout.class);
        if (timeoutAnnotation == null) {
            return;
        }

        Conduit conduit = message.getExchange().getConduit(message);

        if (conduit instanceof HTTPConduit) {
            HTTPConduit httpConduit = ((HTTPConduit) conduit);

            long value = timeoutAnnotation.value();
            ChronoUnit unit = timeoutAnnotation.unit();
            
            long timeoutMillis = Duration.of(value, unit).plusMillis(2).toMillis();

            httpConduit.getClient().setConnectionTimeout(timeoutMillis);
            httpConduit.getClient().setReceiveTimeout(timeoutMillis);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "set connection and read timeout to " + timeoutMillis);
            }
        }
    }
}