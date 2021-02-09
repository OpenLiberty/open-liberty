/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.opentracing.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.opentracing.jaeger.JaegerTracerFactory;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;

/**
 * <p>Open tracing context management.</p>
 *
 * <p>Associate incoming requests with a unique open tracing context. This is made
 * available to outgoing requests by storing the context to a thread local variable.
 */
public class OpentracingTracerManager {
    private static final TraceComponent tc = Tr.register(OpentracingTracerManager.class);

    /**
     * <p>The table of active tracers, keyed on the application name.</p>
     *
     * <p>Static: Each application gets exactly one tracer.</p>
     */
    private static final Map<String, Tracer> applicationTracers = new ConcurrentHashMap<String, Tracer>();

    @Trivial
    private static Tracer ensureTracer(String appName) {
        String methodName = "ensureTracer";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, appName);
        }

        Tracer tracer = null;
        boolean found = true;
        if (appName != null) {
            tracer = applicationTracers.get(appName);
            if (tracer == null) {
                found = false;
                tracer = applicationTracers.computeIfAbsent(appName, TracerCreator.INSTANCE);
            }
            if (tracer == TRACER_NOT_FOUND) {
                tracer = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            String tracerCase;
            if (appName == null) {
                tracerCase = "appName is null";
            } else {
                tracerCase = found ? "previously created" : "newly created or previously created if lost race condition";
            }
            Tr.exit(tc, methodName + " (" + tracerCase + ")", OpentracingUtils.getTracerText(tracer));
        }
        return tracer;
    }

    private static Tracer createJaegerTracer(String appName) {
        return JaegerTracerFactory.createJaegerTracer(appName);
    }

    @Trivial
    private static class TracerCreator implements Function<String, Tracer> {
        static TracerCreator INSTANCE = new TracerCreator();

        @Override
        public Tracer apply(String appName) {
            Tracer tracer = createJaegerTracer(appName);
            if (tracer == null) {
                tracer = createTracer(appName);
            }
            if (tracer == null) {
                tracer = TRACER_NOT_FOUND;
            }
            return tracer;
        }
    }

    private static final Tracer TRACER_NOT_FOUND = new Tracer() {

        @Override
        public Span activeSpan() {
            return null;
        }

        @Override
        public SpanBuilder buildSpan(String arg0) {
            return null;
        }

        @Override
        public <C> SpanContext extract(Format<C> arg0, C arg1) {
            return null;
        }

        @Override
        public <C> void inject(SpanContext arg0, Format<C> arg1, C arg2) {
        }

        @Override
        public ScopeManager scopeManager() {
            return null;
        }

        @Override
        public Scope activateSpan(Span arg0) {
            return null;
        }

        @Override
        public void close() {
        }
    };

    /**
     * <p>Have the open tracer factory service create the tracer. That
     * bridges to a user feature, which enables user supplied tracer
     * implementations.</p>
     *
     * @param appName The name of the application for which to create a tracer.
     *
     * @return The new tracer.
     */
    @Trivial
    private static Tracer createTracer(String appName) {
        return OpentracingUserFeatureAccessService.getTracerInstance(appName);
    }
    // Open tracing context pass through ...

    /**
     * <p>Answer the tracer of the active open tracing context.</p>
     *
     * @Return The tracer of the active open tracing context.
     */
    @Trivial
    public static Tracer getTracer() {
        String methodName = "getTracer";
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }

        String appName = null;
        Tracer tracer = null;
        appName = OpentracingUtils.lookupAppName();
        tracer = ensureTracer(appName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Thread currentThread = Thread.currentThread();
            String threadName = currentThread.getName();
            long threadId = currentThread.getId();
            Tr.exit(tc,
                    methodName + " (" + threadName + ":" + Long.toString(threadId) + ")",
                    OpentracingUtils.getTracerText(tracer));
        }
        return tracer;
    }

    public static void removeTracer(String appName) {
        if (appName != null) {
            applicationTracers.remove(appName);
        }
    }

    public static void clearTracers() {
        applicationTracers.clear();
    }
}