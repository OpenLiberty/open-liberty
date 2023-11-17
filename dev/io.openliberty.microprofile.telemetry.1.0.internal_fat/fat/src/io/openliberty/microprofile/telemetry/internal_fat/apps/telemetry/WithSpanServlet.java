/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;

@SuppressWarnings("serial")
@WebServlet("/testWithSpan")
public class WithSpanServlet extends FATServlet {

    @Inject
    private SpanBean spanBean;

    private static final String PARAMETER_TEST = "testString";
    private static final String TEST_NAME = "testSpan";

    @Test
    public void callNotAnnotated() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        //No span was created
        String spanId = spanBean.methodNotAnnotated();
        assertThat(spanId, equalTo(originalSpanId));
    }

    @Test
    public void callAnnotated() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        String spanId = spanBean.methodAnnotated();
        //Span should end when returned to this method so the current span should have the default spanID of 0000000000000000
        Span span = Span.current();
        assertThat(span.getSpanContext().getSpanId(), equalTo(originalSpanId));

        //Returned spanId from methodAnnotated should not be the default spanID
        assertThat(spanId, not(equalTo(originalSpanId)));
        //Create another span
        String newSpanId = spanBean.methodAnnotated();
        assertThat(newSpanId, not(equalTo(originalSpanId)));
        assertThat(spanId, not(equalTo(newSpanId)));
    }

    @Test
    public void callAnnotatedWithName() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span before call

        ReadableSpan testSpan = spanBean.methodAnnotatedWithName();

        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span after call

        assertThat(testSpan.getSpanContext().getSpanId(), not(equalTo(originalSpanId)));
        assertThat(testSpan.getName(), equalTo(TEST_NAME));
        assertThat(testSpan.getKind(), equalTo(SpanKind.INTERNAL));
    }

    @Test
    public void callAnnotatedWithKind() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span before call

        ReadableSpan testSpan = spanBean.methodAnnotatedWithKind();

        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span after call

        assertThat(testSpan.getSpanContext().getSpanId(), not(equalTo(originalSpanId)));
        assertThat(testSpan.getName(), equalTo("SpanBean.methodAnnotatedWithKind"));
        assertThat(testSpan.getKind(), equalTo(SpanKind.PRODUCER));
    }

    @Test
    public void callAnnotatedWithNameAndKind() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span before call

        ReadableSpan testSpan = spanBean.methodAnnotatedWithNameAndKind();

        assertThat(Span.current().getSpanContext().getSpanId(), equalTo(originalSpanId)); // No current span after call

        assertThat(testSpan.getSpanContext().getSpanId(), not(equalTo(originalSpanId)));
        assertThat(testSpan.getName(), equalTo(TEST_NAME));
        assertThat(testSpan.getKind(), equalTo(SpanKind.CONSUMER));
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

    @Test
    public void callAnnotatedViaExtension() {
        String originalSpanId = Span.current().getSpanContext().getSpanId();
        ReadableSpan span = spanBean.methodAnnotatedViaExtension();
        assertThat(span.getSpanContext().getSpanId(), not(equalTo(originalSpanId)));
        assertThat(span.getParentSpanContext().getSpanId(), equalTo(originalSpanId));
        assertThat(span.getName(), equalTo("nameFromExtension")); // Set in WithSpanExtension
        assertThat(span.getKind(), equalTo(SpanKind.PRODUCER)); // Set in WithSpanExtension
    }

    @ApplicationScoped
    public static class SpanBean {

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

        @WithSpan(TEST_NAME)
        public ReadableSpan methodAnnotatedWithName() {
            return (ReadableSpan) Span.current();
        }

        @WithSpan(kind = SpanKind.PRODUCER)
        public ReadableSpan methodAnnotatedWithKind() {
            return (ReadableSpan) Span.current();
        }

        @WithSpan(value = TEST_NAME, kind = SpanKind.CONSUMER)
        public ReadableSpan methodAnnotatedWithNameAndKind() {
            return (ReadableSpan) Span.current();
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

        public ReadableSpan methodAnnotatedViaExtension() {
            return (ReadableSpan) Span.current();
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
