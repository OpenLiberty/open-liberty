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

import com.ibm.ws.opentracing.OpentracingTracerManager;

import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.tag.Tags;

@Traced
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE) //run this interceptor after platform interceptors but before application interceptors
public class TracedInterceptor {

    @AroundInvoke
    public Object executeFT(InvocationContext context) throws Throwable {
        String methodName = "BB -" + context.getMethod().getName();
        Tracer tracer = OpentracingTracerManager.getTracer();
        Tracer.SpanBuilder spanBuilder = tracer.buildSpan(methodName);

        spanBuilder.withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_SERVER);
        spanBuilder.withTag(Tags.HTTP_METHOD.getKey(), methodName);

        Span span = spanBuilder.startManual();
        tracer.makeActive(span);

        Object result = context.proceed();

        span.finish();
        return result;
    }

}