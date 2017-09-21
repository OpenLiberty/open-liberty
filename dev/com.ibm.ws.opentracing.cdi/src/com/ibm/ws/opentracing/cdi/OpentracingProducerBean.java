/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.opentracing.cdi;

import javax.enterprise.inject.Produces;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.opentracing.OpentracingTracerManager;
import com.ibm.ws.opentracing.OpentracingUtils;

import io.opentracing.Tracer;

public class OpentracingProducerBean {
    private static final TraceComponent tc = Tr.register(OpentracingProducerBean.class);

    @Produces
    @Trivial
    public Tracer getTracer() {
        String methodName = "getTracer";
        if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
            Tr.entry(tc, methodName);
        }

        Tracer tracer = OpentracingTracerManager.getTracer();

        if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
            Tr.exit(tc, methodName, OpentracingUtils.getTracerText(tracer));
        }
        return tracer;
    }
}
