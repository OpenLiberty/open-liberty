/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal_fat.apps.multiapp2;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.microprofile.telemetry.internal_fat.apps.jaxrspropagation.InMemorySpanExporter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/multiapp2")
public class MultiApp2TestServlet extends FATServlet {

    @Inject
    private Tracer tracer;

    @Inject
    private InMemorySpanExporter exporter;

    @Test
    public void testResource() {
        Span span = tracer.spanBuilder("test").startSpan();
        span.end();

        SpanData spanData = exporter.getFinishedSpanItems(1).get(0);
        Resource resource = spanData.getResource();
        assertThat(resource.getAttribute(ResourceAttributes.SERVICE_NAME), equalTo("multiapp2"));
    }

    @Override
    protected void before() throws Exception {
        exporter.reset();
    }

}
