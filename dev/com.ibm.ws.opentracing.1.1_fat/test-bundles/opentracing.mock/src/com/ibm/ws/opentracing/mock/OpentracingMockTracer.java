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

import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalScopeManager;

/**
 * <p>Open liberty mock tracer implementation.</p>
 *
 * <p>This tracer implementation provides two key functions:</p>
 *
 * <p>This tracer wraps a {@link MockTracer}, to which all tracer operations
 * are forwarded.</p>
 *
 * <p>This tracer provides an implementation of {@link #toString} which places
 * details of spans managed by the wrapped tracer.  These details are provided
 * through {@link #toString} so to provide access to span details without adding
 * a new API.</p>
 */
public class OpentracingMockTracer implements Tracer {
    /** <p>Mock tracer wrapped by this tracer.</p> */
    private final MockTracer tracer;

    /**
     * <p>Answer the tracer wrapped by this mock tracer.</p>
     *
     * @return The traccer wrapped by this tracer.
     */
    private MockTracer getTracer() {
        return tracer;
    }

    /**
     * <p>Create and return a new mock tracer.</p>
     *
     * <p>This implementation wraps an instance of {@link MockTracer}.</p>
     */
    public OpentracingMockTracer() {
        this.tracer = new MockTracer(new ThreadLocalScopeManager(), Propagator.TEXT_MAP);
        System.out.println("OpentracingMockTracer(0.31.0).<init>");
    }

    // Tracer pass-throughs.
    //
    // 'finishedSpans' is an operation on MockTracer, not on Tracer, and is not implemented as a
    // pass through.

    /** {@inheritDoc} */
    @Override
    public Span activeSpan() {
        Span activeSpan = getTracer().activeSpan();
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer(0.31.0).activeSpan Thread = " + threadId + " Span = " + activeSpan);
        return activeSpan;
    }

//    /** {@inheritDoc} */
//    @Override
//    public ActiveSpan makeActive(Span span) {
//        long threadId = Thread.currentThread().getId();
//        System.out.println("OpentracingMockTracer(0.31.0).makeActive Thread = " + threadId + " Span = " + span);
//        ActiveSpan active = getTracer().makeActive(span);
//        System.out.println("OpentracingMockTracer(0.31.0).makeActive Thread = " + threadId + " ActiveSpan = " + active);
//        return active;
//    }

    /** {@inheritDoc} */
    @Override
    public SpanBuilder buildSpan(String operationName) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer(0.31.0).buildSpan Thread = " + threadId + " OperationName = " + operationName);
        return getTracer().buildSpan(operationName);
    }

    /** {@inheritDoc} */
    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer(0.31.0).extract Thread = " + threadId);
        return getTracer().extract(format, carrier);
    }

    /** {@inheritDoc} */
    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer(0.31.0).inject Thread = " + threadId + " SpanContext = " + spanContext);
        getTracer().inject(spanContext, format, carrier);
    }

    /**
     * <p>Subclass re-implementation: Implement to answer a JSON formatted list of
     * the finished spans of the wrapped tracer.</p>
     *
     * @return A print string for this mock tracer.  This implementation
     *     emits a JSON formatted list of the finished spans.
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("{ ");

        result.append("\"completedSpans\": [");

        String elementPrefix = "\n  ";
        for ( MockSpan finishedSpan : getTracer().finishedSpans() ) {
            long elapsedMicros = finishedSpan.finishMicros() - finishedSpan.startMicros();
            result.append(elementPrefix);
            result.append("{ ");
            result.append("\"traceId\": \"" + finishedSpan.context().traceId() + "\", ");
            result.append("\"parentId\": \"" + finishedSpan.parentId() + "\", ");
            result.append("\"spanId\": \"" + finishedSpan.context().spanId() + "\", ");
            result.append("\"operation\": \"" + finishedSpan.operationName() + "\", ");
            result.append("\"start\": " + Long.toString(finishedSpan.startMicros()) + ", ");
            result.append("\"finish\": " + Long.toString(finishedSpan.finishMicros()) + ", ");
            result.append("\"elapsed\": " + Long.toString(elapsedMicros) + ", ");

            result.append("\"tags\": {");
            String tagPrefix = " ";
            for ( Map.Entry<String, Object> tagEntry : finishedSpan.tags().entrySet() ) {
                result.append(tagPrefix);
                result.append("\"" + tagEntry.getKey() + "\": \"" + tagEntry.getValue() + "\"");
                tagPrefix = ", ";
            }
            result.append(" }");

            result.append(" }");

            elementPrefix = ",\n  ";
        }

        result.append(" ]");

        result.append(" }");

        return result.toString();
    }

    /* (non-Javadoc)
     * @see io.opentracing.Tracer#scopeManager()
     */
    @Override
    public ScopeManager scopeManager() {
        return getTracer().scopeManager();
    }
}
