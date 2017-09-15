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

package com.ibm.ws.opentracing.mock;

import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

import com.ibm.ws.opentracing.tracer.OpentracingTracerFactory;

import io.opentracing.Tracer;

/**
 *
 */
@Component(immediate = true, service = { OpentracingTracerFactory.class })
public class OpentracingMockTracerFactory implements OpentracingTracerFactory {

    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        System.out.println("OpentracingMockTracerFactory.activate");
        modified(ctx, config);
    }

    @Modified
    protected void modified(ComponentContext ctx, Map<String, Object> config) {

    }

    /** {@inheritDoc} */
    @Override
    public Tracer newInstance(String serviceName) {
        return new OpentracingMockTracer();
    }

}
