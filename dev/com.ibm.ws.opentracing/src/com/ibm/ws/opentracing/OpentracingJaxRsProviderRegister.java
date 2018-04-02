/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.opentracing;

import java.util.List;
import java.util.Set;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxrs20.providers.api.JaxRsProviderRegister;

/**
 * <p>The open tracing filter service.</p>
 */
@Component(immediate = true, service = { JaxRsProviderRegister.class })
public class OpentracingJaxRsProviderRegister implements JaxRsProviderRegister {
    private static final TraceComponent tc = Tr.register(OpentracingJaxRsProviderRegister.class);

    // DSR activation API ...

    protected void activate(ComponentContext context) {
        setContainerFilter();
        setClientFilter();
    }

    protected void deactivate(ComponentContext context) {
        clearContainerFilter();
        clearClientFilter();
    }

    // The filters.  There is a single container filter and a single client
    // filter, both of which are shared by all applications.  The filters are
    // stateless.

    private OpentracingContainerFilter containerFilter;
    private OpentracingClientFilter clientFilter;
    private OpentracingFilterHelper helper;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
        if (containerFilter != null) {
            containerFilter.setFilterHelper(helper);
        }
        if (clientFilter != null) {
            clientFilter.setFilterHelper(helper);
        }
    }

    protected void unsetOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = null;
        if (containerFilter != null) {
            containerFilter.setFilterHelper(null);
        }
        if (clientFilter != null) {
            clientFilter.setFilterHelper(null);
        }
    }

    @Trivial
    protected void setContainerFilter() {
        containerFilter = new OpentracingContainerFilter(helper);
    }

    @Trivial
    protected void clearContainerFilter() {
        containerFilter = null;
    }

    @Trivial
    protected OpentracingContainerFilter getContainerFilter() {
        return containerFilter;
    }

    @Trivial
    protected void setClientFilter() {
        clientFilter = new OpentracingClientFilter(helper);
    }

    @Trivial
    protected void clearClientFilter() {
        clientFilter = null;
    }

    @Trivial
    protected OpentracingClientFilter getClientFilter() {
        return clientFilter;
    }

    //

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