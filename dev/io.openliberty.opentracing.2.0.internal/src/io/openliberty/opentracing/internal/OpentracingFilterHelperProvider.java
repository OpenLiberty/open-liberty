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
@Component(immediate = true, service = OpentracingFilterHelperProvider.class)
public class OpentracingFilterHelperProvider {
    private static final TraceComponent tc = Tr.register(OpentracingFilterHelperProvider.class);
    private static final AtomicReference<OpentracingFilterHelperProvider> instance = new AtomicReference<OpentracingFilterHelperProvider>(null);

    // DSR activation API ...

    protected void activate(ComponentContext context) {
        instance.set(this);
    }

    protected void deactivate(ComponentContext context) {
        instance.compareAndSet(this, null);
    }

    public static OpentracingFilterHelperProvider getInstance() {
        return instance.get();
    }

    private volatile OpentracingFilterHelper helper;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void setOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = helper;
    }

    protected void unsetOpentracingFilterHelper(OpentracingFilterHelper helper) {
        this.helper = null;
    }

    public OpentracingFilterHelper getOpentracingFilterHelper() {
        return helper;
    }
}
