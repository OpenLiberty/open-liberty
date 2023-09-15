/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.common;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasNoParent;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasParentSpanId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.logging.Logger;

import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility methods for working with spans in tests
 */
@ApplicationScoped
public class TestSpans {

    private static final Logger LOGGER = Logger.getLogger(TestSpans.class.getName());

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private HttpServletRequest request;

    @Inject
    private Tracer tracer;

    /**
     * Asserts that the first span in the list has no parent and that every other span is a child of the span preceding it in the list.
     *
     * @param spans the list of spans
     */
    public static void assertLinearParentage(List<SpanData> spans) {
        if (spans.isEmpty()) {
            return;
        }

        SpanData first = spans.get(0);
        assertThat("Span 1", first, hasNoParent());

        SpanData prev = first;
        for (int i = 1; i < spans.size(); i++) {
            SpanData current = spans.get(i);
            assertThat("Span " + i, current, hasParentSpanId(prev.getSpanId()));
            prev = current;
        }
    }

    /**
     * Creates a test span, makes it current, runs an action and ends the span
     * <p>
     * If {@code runnable} throws an exception or error, we assume that a test failure has occurred and instruct {@link InMemorySpanExporter} to ignore stray spans with the same
     * traceId as the test span.
     * <p>
     * This method is intended to remove common boilerplate code in tests which use FATServlet
     *
     * @param runnable the action to run
     * @return the test span
     * @throws Exception if {@code runnable} throws an exception
     */
    public Span withTestSpan(ThrowingRunnable runnable) {
        String spanName = "testSpan-" + request.getRequestURI();
        Span span = tracer.spanBuilder(spanName)
                        .setNoParent()
                        .startSpan();

        LOGGER.info("Created test span. SpanId: " + span.getSpanContext().getSpanId() + " TraceId: " + span.getSpanContext().getTraceId() + " Name: " + spanName);

        try {
            Context contextBefore = Context.current();
            try (Scope scope = span.makeCurrent()) {
                runnable.run();
            } finally {
                span.end();
            }
            assertEquals("Context Leak: initial context was not restored after test span", contextBefore, Context.current());
        } catch (RuntimeException e) {
            exporter.addFailedTestTraceId(span.getSpanContext().getTraceId());
            throw e;
        } catch (Exception e) {
            // Wrap non-runtime exceptions
            exporter.addFailedTestTraceId(span.getSpanContext().getTraceId());
            throw new RuntimeException(e);
        } catch (Throwable e) {
            exporter.addFailedTestTraceId(span.getSpanContext().getTraceId());
            throw e;
        }

        return span;
    }

    @FunctionalInterface
    public static interface ThrowingRunnable {
        public void run() throws Exception;
    }

}
