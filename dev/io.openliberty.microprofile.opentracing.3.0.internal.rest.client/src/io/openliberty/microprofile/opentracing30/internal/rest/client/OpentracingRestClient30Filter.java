/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package io.openliberty.microprofile.opentracing30.internal.rest.client;

import java.io.IOException;
import java.lang.reflect.Method;

import org.eclipse.microprofile.opentracing.Traced;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.opentracing.internal.OpentracingClientFilter;
import jakarta.annotation.Priority;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

/**
 * Checks whether a method on a rest client interface has tracing disabled.
 */
@Priority(Integer.MIN_VALUE + 1) // Early, but after the filter which sets the invokedMethod property
public class OpentracingRestClient30Filter implements ClientRequestFilter {
    
    private static final TraceComponent tc = Tr.register(OpentracingRestClient30Filter.class);

    @Override
    public void filter(ClientRequestContext clientRequestContext) throws IOException {
        Object invokedMethod = clientRequestContext.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");
        if (invokedMethod != null) {
            Method method = (Method) invokedMethod;
            Traced traced = method.getAnnotation(Traced.class);
            
            if ((traced != null) && (!traced.value())) { //@Traced(false)
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(this, tc, "@Traced(false) on method " + method.getName());
                }
                clientRequestContext.setProperty(OpentracingClientFilter.CLIENT_FILTER_ENABLED_ID, false);
            }
        }
        
    }

}
