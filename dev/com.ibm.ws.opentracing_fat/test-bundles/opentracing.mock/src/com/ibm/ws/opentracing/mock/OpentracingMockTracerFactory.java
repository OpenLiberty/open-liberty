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

/**
 * <p>Mock tracer factory.  Provides an implementation of
 * {@link OpentracingTracerFactory} which creates mock tracer
 * instances.</p>
 */
@Component(immediate = true, service = { OpentracingTracerFactory.class })
public class OpentracingMockTracerFactory implements OpentracingTracerFactory {
    /**
     * <p>Service API: Activate this mock tracer factory within a specified
     * context and using the specified configuration values.
     *
     * @param ctx The context relative which to activate the factory.
     * @param config Configuration values used to activate the factory.
     */
    @Activate
    protected void activate(ComponentContext ctx, Map<String, Object> config) {
        System.out.println("OpentracingMockTracerFactory.activate");
    }

    /**
     * <p>Service API: Update this mock tracer factory within a specified
     * context and using the specified configuration values.
     *
     * @param ctx The context relative which to update the factory.
     * @param config Configuration values used to update the factory.
     */
    @Modified
    protected void modified(ComponentContext ctx, Map<String, Object> config) {
        System.out.println("OpentracingMockTracerFactory.modified");
    }

    /**
     * <p>Factory API: Create and return a new tracer.  As this is the
     * mock tracer factory, a mock tracer is created.</p>
     *
     * @param serviceName
     *
     * @return A new mock tracer.
     */
    @Override
    public OpentracingMockTracer newInstance(String serviceName) {
        System.out.println("OpentracingMockTracerFactory.newInstance");
        return new OpentracingMockTracer();
    }
}
