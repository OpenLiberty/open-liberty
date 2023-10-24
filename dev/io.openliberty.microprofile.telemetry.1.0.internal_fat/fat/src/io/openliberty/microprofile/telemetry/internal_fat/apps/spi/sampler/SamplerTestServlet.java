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
package io.openliberty.microprofile.telemetry.internal_fat.apps.spi.sampler;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;

/**
 * Test that a sampler can be provided via SPI
 */
@SuppressWarnings("serial")
@WebServlet("/testSampler")
@ApplicationScoped // Make this a bean so that there's one bean in the archive, otherwise CDI gets disabled and @Inject doesn't work
public class SamplerTestServlet extends FATServlet {

    @Inject
    private Tracer tracer;

    @Test
    public void testSampler() {
        // Span 1 does not set SAMPLE_ME, so it should not be sampled
        Span span1 = tracer.spanBuilder("span1").startSpan();
        try {
            assertFalse(span1.isRecording());
            assertFalse(span1.getSpanContext().isSampled());
        } finally {
            span1.end();
        }

        Span span2 = tracer.spanBuilder("span2").setAttribute(TestSampler.SAMPLE_ME, true).startSpan();
        try {
            assertTrue(span2.isRecording());
            assertTrue(span2.getSpanContext().isSampled());
        } finally {
            span2.end();
        }
    }
}
