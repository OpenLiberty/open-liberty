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

import componenttest.annotation.SkipForRepeat;
import componenttest.app.FATServlet;
import componenttest.rules.repeater.MicroProfileActions;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.openliberty.microprofile.telemetry.internal_fat.shared.TelemetryActions;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.trace.data.SpanData;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

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
    @SkipForRepeat({ MicroProfileActions.MP70_EE11_ID, MicroProfileActions.MP70_EE10_ID, TelemetryActions.MP61_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_ID, TelemetryActions.MP50_MPTEL20_JAVA8_ID, TelemetryActions.MP41_MPTEL20_ID,
                     TelemetryActions.MP14_MPTEL20_ID })
    public void testExporter() {
        AttributeKey<String> FOO_KEY = AttributeKey.stringKey("foo");
        Span span = tracer.spanBuilder("test span").setAttribute(FOO_KEY, "bar").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1, span).get(0);
        assertThat(spanData, hasName("test span"));
        assertThat(spanData, hasAttribute(FOO_KEY, "bar"));
    }

}
