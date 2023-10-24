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
package io.openliberty.microprofile.telemetry.internal_fat.apps.spi.exporter;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasAttribute;
import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.hasName;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Basic test of a span exporter
 */
@SuppressWarnings("serial")
@WebServlet("/testExporter")
public class ExporterTestServlet extends FATServlet {

    @Inject
    private InMemorySpanExporter exporter;

    @Inject
    private Tracer tracer;

    @Test
    public void testExporter() {
        AttributeKey<String> FOO_KEY = AttributeKey.stringKey("foo");
        Span span = tracer.spanBuilder("test span").setAttribute(FOO_KEY, "bar").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1, span).get(0);
        assertThat(spanData, hasName("test span"));
        assertThat(spanData, hasAttribute(FOO_KEY, "bar"));
    }

}
