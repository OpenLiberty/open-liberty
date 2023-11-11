/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.injected;

import static io.openliberty.microprofile.telemetry.internal_fat.common.SpanDataMatcher.isSpan;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.TestSpans;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.sdk.trace.data.SpanData;

/**
 * Test that spans are created with an injected MP Rest Client
 */
@SuppressWarnings("serial")
@WebServlet("/testInjectedClient")
public class InjectedClientTestServlet extends FATServlet {

    @Inject
    private InjectedClientTestClient client;

    @Inject
    private TestSpans testSpans;

    @Inject
    private InMemorySpanExporter exporter;

    @Test
    public void testInjectedClient() {
        Span span = testSpans.withTestSpan(() -> {
            assertThat(client.get(), equalTo("OK"));
        });

        List<SpanData> spans = exporter.getFinishedSpanItems(3, span);
        TestSpans.assertLinearParentage(spans);

        SpanData client = spans.get(1);
        assertThat(client, isSpan().withKind(SpanKind.CLIENT));

        SpanData server = spans.get(2);
        assertThat(server, isSpan().withKind(SpanKind.SERVER));
    }

}
