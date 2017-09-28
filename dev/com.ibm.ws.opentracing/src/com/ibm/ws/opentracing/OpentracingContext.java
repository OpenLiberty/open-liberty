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
package com.ibm.ws.opentracing;

import com.ibm.websphere.ras.annotation.Trivial;

import io.opentracing.Tracer;

/**
 * <p>Open tracing context information: Values associated with an incoming request
 * and which are to be conveyed to child requests.</p>
 *
 * <p>The context stores an application name and a tracer.</p>
 *
 * <p>An open tracing context is set by the filter which handles incoming requests,
 * and is accessed by the filter which handles outgoing requests.</p>
 *
 * <p>The current implementation stores the open tracing context in a thread local
 * variable. See {@link OpentracingTracerManager} for more information.</p>
 */
public class OpentracingContext {
    /** <p>The name of the application which is active in the thread.</p> */
    private String appName;

    /** <p>The trace which is active in the thread.</p> */
    private Tracer tracer;

    @Trivial
    public void setTracer(String appName, Tracer tracer) {
        this.appName = appName;
        this.tracer = tracer;
    }

    @Trivial
    public String getAppName() {
        return appName;
    }

    @Trivial
    public Tracer getTracer() {
        return tracer;
    }
}
