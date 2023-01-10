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

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@WebServlet("/ConfigServlet")
@ApplicationScoped
public class ConfigServlet extends FATServlet {

    @Inject
    Tracer tracer;

    @Inject
    Span span;

    @Inject
    OpenTelemetry openTelemetry;

    //Tests otel.service.name and otel.sdk.disabled
    //Scenario 1 (Telemetry10): appProperties in server.xml should override all other properties and variables
    //Scenario 2 (Telemetry10ConfigServerVar): variables in server.xml should override all other properties and variables
    //Scenario 3 (Telemetry10ConfigSystemProp): properties in bootstrap.properties should override all other properties and variables
    //Scenario 4 (Telemetry10ConfigEnv) environment variables should override all other properties and variables

    @Test
    public void testServiceNameConfig() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();
        assertThat(openTelemetry.toString(), containsString("service.name=\"overrideDone\""));
    }

    //Tests 
    @Test
    public void testSDKDisabledConfig() {
        Tracer tracer = openTelemetry.getTracer("config-test", "1.0.0");
        Span span = tracer.spanBuilder("testSpan").startSpan();
        span.end();
        System.out.println(span);
    }

}