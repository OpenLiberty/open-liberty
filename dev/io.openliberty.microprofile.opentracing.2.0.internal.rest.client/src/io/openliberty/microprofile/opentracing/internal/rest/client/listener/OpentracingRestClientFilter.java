/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.opentracing.internal.rest.client.listener;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.opentracing.Traced;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.opentracing.internal.OpentracingClientFilter;
import io.openliberty.opentracing.internal.OpentracingJaxRsProviderRegister;

/**
 *
 */
public class OpentracingRestClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final TraceComponent tc = Tr.register(OpentracingRestClientFilter.class);
    private OpentracingClientFilter clientFilter = null;

    public OpentracingRestClientFilter() {
        OpentracingJaxRsProviderRegister jaxRsProvider = OpentracingJaxRsProviderRegister.getInstance();
        if (jaxRsProvider == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpentracingJaxRsProviderRegister.getInstance() returned null");
            }
        } else {
            clientFilter = jaxRsProvider.getClientFilter();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        Object invokedMethod = clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        if (invokedMethod != null) {
            Method method = (Method) invokedMethod;
            Traced traced = method.getAnnotation(Traced.class);
            if ((traced != null) && (!traced.value())) { //@Traced(false)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "@Traced(false) on method " + method.getName());
                }
                return;
            }
        }

        if (clientFilter != null) {
            clientFilter.filter(clientRequestContext);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "clientFilter is null");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        if (clientFilter != null) {
            clientFilter.filter(clientRequestContext, clientResponseContext);
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "clientFilter is null");
            }
        }
    }

}
