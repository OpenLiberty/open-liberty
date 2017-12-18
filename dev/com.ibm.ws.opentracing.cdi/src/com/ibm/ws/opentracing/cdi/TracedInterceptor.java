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

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.eclipse.microprofile.opentracing.Traced;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.opentracing.OpentracingTracerManager;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.Tracer;

@Traced
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE) //run this interceptor after platform interceptors but before application interceptors
public class TracedInterceptor {

    private static final TraceComponent tc = Tr.register(TracedInterceptor.class);

    @AroundInvoke
    public Object executeFT(InvocationContext context) throws Throwable {
        String methodName = "executeFT";

        // If the annotated method is not a JAX-RS endpoint, the default
        // operation name of the new Span for the method is:
        // <package name>.<class name>.<method name>"
        // https://github.com/eclipse/microprofile-opentracing/blob/master/spec/src/main/asciidoc/microprofile-opentracing.asciidoc#321-the-traced-annotation

        String operationName = context.getClass().getName() + "." + context.getMethod().getName();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " operationName", operationName);
        }

        Tracer tracer = OpentracingTracerManager.getTracer();

        ActiveSpan activeSpan = tracer.activeSpan();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(operationName);
        if (activeSpan != null) {
            spanBuilder.asChildOf(activeSpan.context());
        }
        Span span = spanBuilder.startManual();
        if (activeSpan == null) {
            tracer.makeActive(span);
        }

        Object result = context.proceed();

        span.finish();
        return result;
    }

}