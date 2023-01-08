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

import org.eclipse.microprofile.opentracing.Traced;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.opentracing.internal.OpentracingClientFilter;

/**
 * Checks whether the interface of a new Rest Client Builder should be traced.
 * <p>
 * Either sets a property to indicate tracing is disabled, or adds a filter to do the same check on individual method calls.
 */
public class OpentracingRestClient30Listener implements RestClientListener {

    private static final TraceComponent tc = Tr.register(OpentracingRestClient30Listener.class);

    @Override
    public void onNewClient(Class<?> serviceInterface, RestClientBuilder builder) {
        Traced traced = serviceInterface.getAnnotation(Traced.class);
        if (traced != null && !traced.value()) {
            // tracing is disabled
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "@Traced(false) specified on service interface");
            }
            // Property read by OpentracingClientFilter
            builder.property(OpentracingClientFilter.CLIENT_FILTER_ENABLED_ID, false);
        } else {
            builder.register(OpentracingRestClient30Filter.class);
        }
    }

}
