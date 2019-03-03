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
package com.ibm.ws.microprofile.opentracing.rest.client.listener;

import java.io.IOException;
import java.lang.reflect.Method;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;

import org.eclipse.microprofile.opentracing.Traced;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.opentracing.OpentracingClientFilter;
import com.ibm.ws.opentracing.OpentracingJaxRsProviderRegister;

/**
 *
 */
public class OpentracingRestClientFilter implements ClientRequestFilter, ClientResponseFilter {

    private static final TraceComponent tc = Tr.register(OpentracingRestClientFilter.class);
    private OpentracingClientFilter clientFilter = null;

    public OpentracingRestClientFilter() {
        System.out.println("OpentracingRestClientFilter constructor");
        OpentracingJaxRsProviderRegister jaxRsProvider = OpentracingJaxRsProviderRegister.getInstance();
        if (jaxRsProvider == null) {
            System.out.println("OpentracingJaxRsProviderRegister.getInstance() returned null");
        } else {
            clientFilter = jaxRsProvider.getClientFilter();
        }
//        OpentracingRestClientInit init = OpentracingRestClientInit.getInstance();
//        if (init == null) {
//            System.out.println("OpentracingRestClientInit service has not been initialized");
//        } else {
//            OpentracingFilterProvider filterProvider = init.getOpentracingFilterProvider();
//            if (filterProvider == null) {
//                System.out.println("No OpentracingFilterProvider is set in OpentracingRestClientInit");
//            } else {
//                clientFilter = filterProvider.getClientFilter();
//            }
//    }

    }

    /** {@inheritDoc} */
    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        System.out.println("OpentracingRestClientFilter outgoing");

        Object invokedMethod = clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        if (invokedMethod != null) {
            Method method = (Method) invokedMethod;
            Traced traced = method.getAnnotation(Traced.class);
            if ((traced != null) && (!traced.value())) { //@Traced(false)
                System.out.println("clientFilter @Traced(false)");
                return;
            }
        }

        if (clientFilter != null) {
            clientFilter.filter(clientRequestContext);
        } else {
            System.out.println("clientFilter is null");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void filter(ClientRequestContext clientRequestContext, ClientResponseContext clientResponseContext) throws IOException {
        System.out.println("OpentracingRestClientFilter incoming");
        if (clientFilter != null) {
            clientFilter.filter(clientRequestContext, clientResponseContext);
        } else {
            System.out.println("clientFilter is null");
        }
    }

}
