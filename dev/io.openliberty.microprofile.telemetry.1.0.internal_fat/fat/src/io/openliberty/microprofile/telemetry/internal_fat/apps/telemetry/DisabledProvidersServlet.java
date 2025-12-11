/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
import static org.hamcrest.Matchers.not;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;

@SuppressWarnings("serial")
@WebServlet("/disabledProviders")
@ApplicationScoped
public class DisabledProvidersServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    public void testDisabledProviders() throws UnknownHostException {
        Tracer tracer = openTelemetry.getTracer("disabled-providers-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();

        String output = openTelemetry.toString().toLowerCase();

        //Disabled by io.opentelemetry.instrumentation.resources.HostResourceProvider
        assertThat(output, not(containsString("host.arch")));
        assertThat(output, not(containsString("host.name")));

        //Disabled by io.opentelemetry.instrumentation.resources.OsResourceProvider
        assertThat(output, not(containsString("os.type")));
        assertThat(output, not(containsString("os.description")));

        //Disabled by io.opentelemetry.instrumentation.resources.ProcessResourceProvider
        assertThat(output, not(containsString("process.command_line")));
        assertThat(output, not(containsString("process.executable.path")));
        assertThat(output, not(containsString("process.pid")));

        //Disabled by io.opentelemetry.instrumentation.resources.ProcessRuntimeResourceProvider
        assertThat(output, not(containsString("process.runtime.name")));
        assertThat(output, not(containsString("process.runtime.version")));
        assertThat(output, not(containsString("process.runtime.description")));
    }

}