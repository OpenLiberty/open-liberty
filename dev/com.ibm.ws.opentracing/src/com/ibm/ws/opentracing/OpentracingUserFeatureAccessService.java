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
 * factory. The expectation is that a tracer factory will be supplied through a user feature,
 * with the tracer factory handling all details of creating and initializing tracers. For example,
 * tracers are expected to connect to a trace event handler -- a server which accepts and collates
 * trace events.</p>
 */
@Component(immediate = true, service = { OpentracingUserFeatureAccessService.class })
public class OpentracingUserFeatureAccessService {
    private static final TraceComponent tc = Tr.register(OpentracingUserFeatureAccessService.class);

    //

    private static OpentracingTracerFactory opentracingTracerFactory;

    private static boolean factoryFirstUse = true;

    @Reference
    public void setOpentracingTracerFactory(OpentracingTracerFactory opentracingTracerFactory) {
        OpentracingUserFeatureAccessService.opentracingTracerFactory = opentracingTracerFactory;
        factoryFirstUse = false;

        if (opentracingTracerFactory == null) {
            Tr.error(tc, "OPENTRACING_NO_TRACERFACTORY");
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
        String methodName = "getTracerInstance";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, appName);
        }

        Tracer tracer = null;

        if (opentracingTracerFactory != null) {
            try {
                tracer = opentracingTracerFactory.newInstance(appName);
            } catch (Throwable t) {
                Tr.error(tc, "OPENTRACING_COULD_NOT_CREATE_TRACER", t);
            }
        }

        // It's not worth synchronizing around this and creating a
        // bottleneck, since the only purpose is to print an error
        // message if there is no factory configured. In the worst
        // (and unlikely) case, this may cause a handful of those error messages to
        // be printed instead of the expected 1, and this is fine
        // to avoid the performance impact.
        if (factoryFirstUse) {
            factoryFirstUse = false;

            if (opentracingTracerFactory == null) {
                Tr.error(tc, "OPENTRACING_NO_TRACERFACTORY");
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, OpentracingUtils.getTracerText(tracer));
        }
        return tracer;
    }
}
