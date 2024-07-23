/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.hamcrest.CoreMatchers.containsString;
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
@WebServlet("/testResources")
@ApplicationScoped
public class ResourceServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    public void testServiceNameConfig() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();
        assertThat(openTelemetry.toString(), containsString("service.name=\"overridddddddeDone\""));
    }

}