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
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/opentelemetry")
@ApplicationScoped
public class OpenTelemetryBeanServlet extends FATServlet {

    @Inject
    OpenTelemetry openTelemetry;

    private static final String SPAN_NAME = "MySpanName";
    private static final String INVALID_SPAN_ID = "0000000000000000";

    @Test
    public void testOpenTelemetryBean() {
        assertNotNull(openTelemetry);
        Tracer tracer = openTelemetry.getTracer("instrumentation-test", "1.0.0");
        assertNotNull(tracer);
        Span span = tracer.spanBuilder(SPAN_NAME).startSpan();
        assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
    }

}