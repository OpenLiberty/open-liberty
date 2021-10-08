/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.opentracing.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * <p>The open tracing filter service.</p>
 */
@Component(immediate = true)
public class OpentracingJaxRsProviderRegister {
    private static final TraceComponent tc = Tr.register(OpentracingJaxRsProviderRegister.class);
    private static final AtomicReference<OpentracingJaxRsProviderRegister> instance = new AtomicReference<OpentracingJaxRsProviderRegister>(null);

    // DSR activation API ...

    protected synchronized void activate(ComponentContext context) {
        instance.set(this);
    }

    protected void deactivate(ComponentContext context) {
        instance.compareAndSet(this, null);
    }

    public static OpentracingJaxRsProviderRegister getInstance() {
        return instance.get();
    }

    // The filters.  There is a single container filter and a single client
    // filter, both of which are shared by all applications.  The filters are
    // stateless.

    private OpentracingFilterHelper helper;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected synchronized void setOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
    }

    protected void unsetOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = null;
    }

    public OpentracingFilterHelper getOpentracingFilterHelper() {
        return helper;
    }
}