/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import java.util.ArrayList;

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
    private static final String INVALID_SPAN_ID = "0000000000000000";

    @Test
    public void callNotAnnotated() {
        //Returned spanId from methodNotAnnotated should not be the spanID of 0000000000000000
        //No span was created
        String spanId = spanBean.methodNotAnnotated();
        assertThat(spanId, equalTo(INVALID_SPAN_ID));
    }

    @Test
    public void callAnnotated() {
        String spanId = spanBean.methodAnnotated();
        //Span should end when returned to this method so the current span should have the default spanID of 0000000000000000
        Span span = Span.current();
        assertThat(span.getSpanContext().getSpanId(), equalTo(INVALID_SPAN_ID));

        //Returned spanId from methodAnnotated should not be the default spanID
        assertThat(spanId, not(equalTo(INVALID_SPAN_ID)));
        //Create another span
        String newSpanId = spanBean.methodAnnotated();
        assertThat(newSpanId, not(equalTo(INVALID_SPAN_ID)));
        assertThat(spanId, not(equalTo(newSpanId)));
    }

    @Test
    public void callMethodAnnotatedWithParameter() {
        String spanParameter = spanBean.methodAnnotatedWithParameter(PARAMETER_TEST);
        //Span should end when returned to this method so the current span should have the default spanID of 0000000000000000
        assertThat(spanParameter, equalTo(PARAMETER_TEST));
    }

    @Test
    public void callNestedAnnotated() {
        ArrayList<String> ids = spanBean.nestedAnnotated();
        assertThat(ids, hasSize(2));
        assertThat(ids.get(0), not(equalTo(ids.get(1))));
    }

    @ApplicationScoped
    public static class SpanBean {

        @Inject
        private SpanBean spanBean;

        @Inject
        private SecondSpanBean secondSpanBean;

        //Creates a span for this method
        @WithSpan
        public String methodAnnotated() {
            Span span = Span.current();
            return span.getSpanContext().getSpanId();
        }

        //Creates a span for this method
        @WithSpan
        public ArrayList<String> nestedAnnotated() {
            ArrayList<String> ids = new ArrayList<String>();
            Span span = Span.current();
            ids.add(span.getSpanContext().getSpanId());
            ids.add(secondSpanBean.methodAnnotated());
            return ids;
        }

        //SpanAttribute is applied to the method parameter
        @WithSpan
        public String methodAnnotatedWithParameter(@SpanAttribute("testParameter") String testParameter) {
            Span span = Span.current();
            ReadableSpan readableSpan = (ReadableSpan) span;
            return readableSpan.getAttribute(AttributeKey.stringKey("testParameter"));
        }

        //Method is not annotated so no span is created
        public String methodNotAnnotated() {
            Span span = Span.current();
            return span.getSpanContext().getSpanId();
        }

    }

    @ApplicationScoped
    public static class SecondSpanBean {

        //Creates a span for this method
        @WithSpan
        public String methodAnnotated() {
            Span span = Span.current();
            return span.getSpanContext().getSpanId();
        }

    }

}
