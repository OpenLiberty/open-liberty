/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal.config_fat.apps.telemetry;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@SuppressWarnings("serial")
@WebServlet("/testRuntime")
@ApplicationScoped
public class RuntimeInstanceServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    private static final String INVALID_SPAN_ID = "0000000000000000";

    //Tests otel.service.name is undefined on the runtime instance if we do not set it an environment variable or system property
    //See ConfigServlet for the tests that ensure it is correct when it is defined.
    //See also ServiceNameServlet which shows what happens when it is undefind on Tel 2.0 but we
    //are not using the runtime instance.
    @Test
    public void testServiceNameUnknownOnRuntimeInstance() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();
        assertThat(openTelemetry.toString(), containsString("service.name=\"unknown_service\""));
    }

    //Tests otel.sdk.disabled and spans work using the runtime mode
    @Test
    public void testSDKNotDisabledOnRuntimeInstanceServlet() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        assertThat(span.getSpanContext().getSpanId(), not(equalTo(INVALID_SPAN_ID)));
        span.end();
    }

}
