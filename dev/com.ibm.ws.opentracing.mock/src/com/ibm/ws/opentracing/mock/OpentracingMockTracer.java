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

import java.util.List;
import java.util.Map;

import io.opentracing.ActiveSpan;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.propagation.Format;
import io.opentracing.util.ThreadLocalActiveSpanSource;

public class OpentracingMockTracer implements Tracer {
    MockTracer tracer;

    public OpentracingMockTracer() {
        tracer = new MockTracer(new ThreadLocalActiveSpanSource(), Propagator.TEXT_MAP);
    }

    /** {@inheritDoc} */
    @Override
    public ActiveSpan activeSpan() {
        ActiveSpan active = tracer.activeSpan();
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer.activeSpan Thread:" + threadId + " Span = " + active);
        return active;
    }

    /** {@inheritDoc} */
    @Override
    public ActiveSpan makeActive(Span arg0) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer.makeActive Thread:" + threadId + " Span = " + arg0);
        ActiveSpan active = tracer.makeActive(arg0);
        System.out.println("OpentracingMockTracer.makeActive Thread:" + threadId + " ActiveSpan = " + arg0);
        return active;
    }

    /** {@inheritDoc} */
    @Override
    public SpanBuilder buildSpan(String arg0) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer.buildSpan Thread:" + threadId + " name = " + arg0);
        return tracer.buildSpan(arg0);
    }

    /** {@inheritDoc} */
    @Override
    public <C> SpanContext extract(Format<C> arg0, C arg1) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer.extract Thread:" + threadId);
        return tracer.extract(arg0, arg1);
    }

    /** {@inheritDoc} */
    @Override
    public <C> void inject(SpanContext arg0, Format<C> arg1, C arg2) {
        long threadId = Thread.currentThread().getId();
        System.out.println("OpentracingMockTracer.inject Thread:" + threadId + " SpanContext = " + arg0);
        tracer.inject(arg0, arg1, arg2);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        String result = "[";
        List<MockSpan> finishedSpans = tracer.finishedSpans();
        String elementPrefix = "\n  ";
        for (MockSpan finishedSpan : finishedSpans) {
            Long elapsed = finishedSpan.finishMicros() - finishedSpan.startMicros();
            result += elementPrefix + "{ " +
                      "\"traceID\":\"" + finishedSpan.context().traceId() + "\", " +
                      "\"parentID\":\"" + finishedSpan.parentId() + "\", " +
                      "\"spanID\":\"" + finishedSpan.context().spanId() + "\", " +
                      "\"operationName\":\"" + finishedSpan.operationName() + "\", " +
                      "\"startTime\":\"" + finishedSpan.startMicros() + "\", " +
                      "\"finishTime\":\"" + finishedSpan.finishMicros() + "\", " +
                      "\"elapsedTime\":\"" + elapsed + "\", ";
            result += "\"Tags\": {";
            Map<String, Object> tags = finishedSpan.tags();
            String tagPrefix = "";
            for (String tag : tags.keySet()) {
                result += tagPrefix + "\"" + tag + "\":\"" + tags.get(tag) + "\"";
                tagPrefix = ", ";
            }
            result += "}}";
            elementPrefix = ",\n  ";
        }
        result = result + "\n]\n";
        return result;
    }
}
