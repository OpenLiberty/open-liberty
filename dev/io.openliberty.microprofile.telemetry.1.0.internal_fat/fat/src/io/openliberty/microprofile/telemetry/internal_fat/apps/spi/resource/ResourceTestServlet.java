/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.spi.resource;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.common.spanexporter.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Test that the Resource can be customized via SPI
 */
@SuppressWarnings("serial")
@WebServlet("/resource")
public class ResourceTestServlet extends FATServlet {

    public static final String TEST_VALUE1 = "test1";
    public static final String TEST_VALUE2 = "test2";

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter exporter;

    @Test
    public void testResource() {
        Span span = tracer.spanBuilder("span").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1).get(0);

        // Attributes added by TestResourceProvider should have been merged into the default resource
        Resource resource = spanData.getResource();
        assertThat(resource.getAttribute(TestResourceProvider.TEST_KEY1), equalTo(TEST_VALUE1));
        assertThat(resource.getAttribute(TestResourceProvider.TEST_KEY2), equalTo(TEST_VALUE2));
    }
}
