/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/WithSpanServlet")
public class WithSpanServlet extends FATServlet {

    @Inject
    private SpanBean spanBean;

    private static final String PARAMETER_TEST = "testString";

    @Test
    public void callNotAnnotated() {
        //Returned spanId from methodNotAnnotated should not be the spanID of 0000000000000000
        //No span was created
        String spanId = spanBean.methodNotAnnotated();
        assertThat(spanId, equalTo("0000000000000000"));
    }

    @Test
    public void callAnnotated() {
        String spanId = spanBean.methodAnnotated();
        //Span should end when returned to this method so the current span should have the default spanID of 0000000000000000
        Span span = Span.current();
        assertThat(span.getSpanContext().getSpanId(), equalTo("0000000000000000"));

        //Returned spanId from methodAnnotated should not be the default spanID
        assertThat(spanId, not(equalTo("0000000000000000")));
    }

    @Test
    public void callMethodAnnotatedWithParameter() {
        String spanParameter = spanBean.methodAnnotatedWithParameter(PARAMETER_TEST);
        //Span should end when returned to this method so the current span should have the default spanID of 0000000000000000
        assertThat(spanParameter, equalTo(PARAMETER_TEST));
    }

    /*
     * Make multiple calls to
     *
     * @Test
     * public void callRecursiveAnnotated() {
     * int calls = 2;
     * Span span = Span.current();
     * String previousId = span.getSpanContext().getSpanId();
     * spanBean.methodRecursiveAnnotated(calls, previousId);
     * }
     */

    @ApplicationScoped
    public static class SpanBean {

        //Creates a span for this method
        @WithSpan
        String methodAnnotated() {
            Span span = Span.current();
            return span.getSpanContext().getSpanId();
        }

        //Creates a span for this method
        //Method calls itself a number of times and compares the current span with the parent span
        //TO DO
        //The currentId remains the same as the previousId - It's not clear if this is expected behaviour or not
        @WithSpan
        void methodRecursiveAnnotated(int calls, String previousId) {
            Span span = Span.current();
            String currentId = span.getSpanContext().getSpanId();
            System.out.println(currentId + " " + previousId);
            if (calls > 0) {
                methodRecursiveAnnotated(calls - 1, currentId);
            }
        }

        //SpanAttribute is applied to the method parameter
        @WithSpan
        String methodAnnotatedWithParameter(@SpanAttribute("testParameter") String testParameter) {
            Span span = Span.current();
            ReadableSpan readableSpan = (ReadableSpan) span;
            return readableSpan.getAttribute(AttributeKey.stringKey("testParameter"));
        }

        //Method is not annotated so no span is created
        String methodNotAnnotated() {
            Span span = Span.current();
            return span.getSpanContext().getSpanId();
        }

    }
}
