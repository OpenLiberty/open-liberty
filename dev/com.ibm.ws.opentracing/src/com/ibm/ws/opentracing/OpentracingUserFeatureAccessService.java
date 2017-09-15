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
import com.ibm.ws.opentracing.tracer.OpentracingTracerFactory;

import io.opentracing.Tracer;

/**
 * <p>The open tracing filter service.</p>
 */
@Component(immediate = true, service = { OpentracingUserFeatureAccessService.class })
public class OpentracingUserFeatureAccessService {
    private static final TraceComponent tc = Tr.register(OpentracingUserFeatureAccessService.class);

    private static OpentracingTracerFactory opentracingTracerFactory = null;

    @Reference
    public void setOpentracingTracerFactory(OpentracingTracerFactory opentracingTracerFActory) {
        OpentracingUserFeatureAccessService.opentracingTracerFactory = opentracingTracerFActory;
    }

    public static Tracer getTracerInstance(String serviceName) {
        Tracer tracer = null;
        try {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "OpentracingUserFeatureAccessService.getTracerInstance() Construct Tracer");
            }

            tracer = opentracingTracerFactory.newInstance(serviceName);
            if (tracer == null) {
                // We could provide our default Tracer here
                Tr.error(tc, "OPENTRACING_TRACERFACTORY_RETURNED_NULL");
            }
        } catch (Exception e) {
            // We could provide our default Tracer here
            Tr.error(tc, "OPENTRACING_COULD_NOT_CREATE_TRACER", e.getMessage());
        }
        return tracer;
    }
}
