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

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.opentracing.tracer.OpentracingTracerFactory;

import io.opentracing.Tracer;

/**
 * <p>Tracer factory service.</p>
 *
 * <p>This is the service transition from the tracer manager to the service defined tracer
 * factory.  The expectation is that a tracer factory will be supplied through a user feature,
 * with the tracer factory handling all details of creating and initializing tracers.  For example,
 * tracers are expected to connect to a trace event handler -- a server which accepts and collates
 * trace events.</p>
 */
@Component(immediate = true, service = { OpentracingUserFeatureAccessService.class })
public class OpentracingUserFeatureAccessService {
    private static final TraceComponent tc = Tr.register(OpentracingUserFeatureAccessService.class);

    //

    private static OpentracingTracerFactory opentracingTracerFactory;

    @Reference
    public void setOpentracingTracerFactory(OpentracingTracerFactory opentracingTracerFactory) {
        OpentracingUserFeatureAccessService.opentracingTracerFactory = opentracingTracerFactory;

        if ( TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled() ) {
            Tr.debug(tc, "getTracerInstance", opentracingTracerFactory);
        }
    }

    //

    /**
     * <p>Answer a tracer instance for a named application.</p>
     *
     * <p>Invoked from {@link com.ibm.ws.opentracing.OpentracingTracerManager#createTracer(String)}.</p>
     *
     * @param appName The name of the application for which to create the tracer instance.
     *
     * @return The new tracer.
     */
    @Trivial
    public static Tracer getTracerInstance(String appName) {
        String prefix = "getTracerInstance";
        if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
            Tr.entry(tc, prefix, appName);
        }

        Tracer tracer;
        try {
            tracer = opentracingTracerFactory.newInstance(appName);
            if ( tracer == null ) {
                Tr.error(tc, "OPENTRACING_TRACERFACTORY_RETURNED_NULL");
            }
        } catch ( Exception e ) {
            tracer = null;
            Tr.error(tc, "OPENTRACING_COULD_NOT_CREATE_TRACER", e.getMessage());
        }

        if ( TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled() ) {
            Tr.exit(tc, prefix, OpentracingUtils.getTracerText(tracer));
        }
        return tracer;
    }
}
