/*******************************************************************************
 * Copyright (c) 2024, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.telemetry.user.feature;

import static org.junit.Assert.fail;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.ws.rs.ext.Provider;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.spi.OpenTelemetryAccessor;
import io.openliberty.microprofile.telemetry.spi.OpenTelemetryInfo;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

@Provider
public class UserFeatureServletFilter implements Filter {

    private static final TraceComponent tc = Tr.register(UserFeatureServletFilter.class);

    private static final String SPAN_NAME = "UserFeatureSpanName";
    private static final AttributeKey<String> SPAN_ATTRIBUTE_KEY = AttributeKey.stringKey("FromUserFeature");
    private static final String SPAN_ATTRIBUTE_VALUE = "True";

    @Override
    public void destroy() {
        //do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        System.out.println("UserFeatureTest: Entering user feature  doFilter");

        //Test we can use an OpenTelemetry we got via the SPI by using it to create a span.
        OpenTelemetryInfo openTelemetryInfo = OpenTelemetryAccessor.getOpenTelemetryInfo();
        if (openTelemetryInfo.isEnabled()) {
            Tracer tracer = openTelemetryInfo.getOpenTelemetry().getTracer("user-feature-test", "1.0.0");
            Span span = tracer.spanBuilder(SPAN_NAME)
                            .setAttribute(SPAN_ATTRIBUTE_KEY, SPAN_ATTRIBUTE_VALUE)
                            .startSpan();
            try (Scope scope = span.makeCurrent()) {
                chain.doFilter(request, response);
            } finally {
                span.end();
            }
        } else {
            // If this was a real app we'd do nothing here, giving a performance gain over sending stuff
            // to a no-op OpenTelemetry object but in this test we know its should be enabled so we
            fail("Open Telemetry was not enabled");
        }
    }

    @Override
    public void init(FilterConfig arg0) throws ServletException {
        //do nothing
    }

}
