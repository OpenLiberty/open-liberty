/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.opentracing.internal.jaxrs;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

import io.openliberty.opentracing.internal.OpentracingClientFilter;
import io.openliberty.opentracing.internal.OpentracingContainerFilter;
import io.openliberty.opentracing.internal.OpentracingFilterHelperProvider;
import io.openliberty.opentracing.internal.OpentracingFilterProvider;

/**
 * <p>The open tracing filter service.</p>
 */
@Component(immediate = true, service = { JaxRsProviderRegister.class, OpentracingFilterProvider.class })
public class OpentracingJaxRsProviderRegister implements JaxRsProviderRegister, OpentracingFilterProvider {
    private static final TraceComponent tc = Tr.register(OpentracingJaxRsProviderRegister.class);
    
    // This reference ensures that we don't try to create any filter objects
    // until the OpentracingFilterHelperProvider is active
    @Reference
    protected OpentracingFilterHelperProvider helper;

    // DSR activation API ...

    protected void activate(ComponentContext context) {
        containerFilter = new OpentracingContainerFilter();
        clientFilter = new OpentracingClientFilter();
    }

    protected void deactivate(ComponentContext context) {
        containerFilter = null;
        clientFilter = null;
    }

    // The filters.  There is a single container filter and a single client
    // filter, both of which are shared by all applications.  The filters are
    // stateless.

    private OpentracingContainerFilter containerFilter;
    private OpentracingClientFilter clientFilter;

    @Override
    @Trivial
    public OpentracingContainerFilter getContainerFilter() {
        return containerFilter;
    }

    @Override
    @Trivial
    public OpentracingClientFilter getClientFilter() {
        return clientFilter;
    }

    @Override
    @Trivial
    public void installProvider(boolean clientSide, List<Object> providers, Set<String> features) {
        String methodName = "installProvider";

        if (clientSide) {
            OpentracingClientFilter useClientFilter = getClientFilter();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Client Filter", useClientFilter);
            }
            if (useClientFilter != null) {
                providers.add(useClientFilter);
            } else {
                // Ignore: The component is not active.
            }

        } else {
            OpentracingContainerFilter useContainerFilter = getContainerFilter();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName, "Container Filter", useContainerFilter);
            }
            if (useContainerFilter != null) {
                providers.add(useContainerFilter);
            } else {
                // Ignore: The component is not active.
            }
        }
    }
}