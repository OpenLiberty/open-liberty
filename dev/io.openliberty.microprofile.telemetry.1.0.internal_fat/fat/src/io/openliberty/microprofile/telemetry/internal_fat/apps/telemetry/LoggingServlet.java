/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/LoggingServlet")
@ApplicationScoped
public class LoggingServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    @Test
    public void testLoggingExporter() {
        Tracer tracer = openTelemetry.getTracer("logging-exporter-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        span.end();
    }
}