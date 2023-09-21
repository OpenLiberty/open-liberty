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

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasExceptionLog;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasKind;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasName;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasStatus;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/WithSpanErrorServlet")
public class WithSpanErrorServlet extends FATServlet {

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private SpanBean spanBean;

    @Test
    public void callMethodRuntimeException() {
        try {
            spanBean.methodRuntimeException();
            fail("Error not thrown");
        } catch (RuntimeException e) {
            //Span should still be created with the error status present
            SpanData spanData = exporter.getFinishedSpanItems(1).get(0);
            assertThat(spanData, hasName("SpanBean.methodRuntimeException"));
            assertThat(spanData, hasKind(INTERNAL));
            assertThat(spanData, hasStatus(StatusCode.ERROR));
            assertThat(spanData, hasExceptionLog(RuntimeException.class));
        }
    }

    @Test
    public void callMethodNullPointerException() {
        try {
            spanBean.methodNullPointerException();
            fail("Error not thrown");
        } catch (RuntimeException e) {
            //Span should still be created with the error status present
            SpanData spanData = exporter.getFinishedSpanItems(1).get(0);
            assertThat(spanData, hasName("SpanBean.methodNullPointerException"));
            assertThat(spanData, hasKind(INTERNAL));
            assertThat(spanData, hasStatus(StatusCode.ERROR));
            assertThat(spanData, hasExceptionLog(NullPointerException.class));
        }
    }

    @ApplicationScoped
    public static class SpanBean {

        //Creates a span for this method that throws an exception
        @WithSpan
        public String methodRuntimeException() {
            throw new RuntimeException("runtime exception");
        }

                //Creates a span for this method that throws an exception
        @WithSpan
        public String methodNullPointerException() {
            throw new NullPointerException("runtime exception");
        }
    }

}
