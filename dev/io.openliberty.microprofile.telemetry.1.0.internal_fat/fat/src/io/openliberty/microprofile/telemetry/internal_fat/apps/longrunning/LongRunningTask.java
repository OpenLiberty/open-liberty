/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.longrunning;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/longrunning")
@ApplicationScoped
public class LongRunningTask extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    public static final String SUCCESS_MESSAGE = "COMPLETED SUCCESSFULLY";

    @Test
    public void testLongRunningTask() {
        Span span = tracer.spanBuilder("longRunningTask").startSpan();
        try (Scope scope = span.makeCurrent()) {
            assertThat(doWork(), equalTo(SUCCESS_MESSAGE));
            assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        } finally {
            span.end();
        }
    }

    private String doWork() {
        Span workSpan = tracer.spanBuilder("workSpan").startSpan();
        try {
            assertThat(workSpan.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
            return SUCCESS_MESSAGE;
        } finally {
            workSpan.end();
        }
    }

}