/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.openliberty.microprofile.telemetry.internal_fat.apps.bean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.logging.Logger;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/")
@ApplicationScoped
public class TelemetryBeanTestServlet extends FATServlet {

    Logger logger = Logger.getLogger(this.getClass().getCanonicalName());

    @Inject
    Tracer tracer;

    @Inject
    Span injectedSpan;

    @Inject
    Baggage injectedBaggage;

    @Test
    public void testSpanUpdates() {
        injectedSpan.makeCurrent(); //This should do nothing, and putting it here ensures the proxy doesn't delegate to itself in an infinite loop. 
        injectedSpan.toString(); //Just poke the object so CDI fetches the real object behind the proxy.

        Span span = Span.current();
        logger.info("Span.current at the start of hte method : " + span.toString());
        assertEquals("The injected span was not the same as the current span before calling makeCurrent()", span.getSpanContext(), injectedSpan.getSpanContext());

        Span newSpan = tracer.spanBuilder("my span").startSpan();
        logger.info("Span created during the test: " + newSpan.toString());

        try (Scope s = newSpan.makeCurrent()) {
            Span spanTwo = Span.current();
            logger.info("Span.current() after creating a new span and making it current : " + spanTwo.toString());

            assertFalse("We got the same span from span.current() before and after calling makeCurrent() on a newly created span",
                        span.getSpanContext().equals(spanTwo.getSpanContext()));
            assertEquals("The current span was not the one we created and invoked makeCurrent() with", newSpan.getSpanContext(), spanTwo.getSpanContext());
            assertEquals("The injected span was not the same as the current span after calling makeCurrent()", spanTwo.getSpanContext(), injectedSpan.getSpanContext());
        } finally {
            newSpan.end();
        }
    }

    @Test
    public void testBaggageUpdates() {
        injectedBaggage.toString(); //Just poke the object so CDI fetches the real object behind the proxy.

        Baggage baggage = Baggage.current();
        logger.info("Baggage.current at the start of hte method : " + baggage.toString());
        assertEquals("The injected baggage was not the same as the current baggage before calling makeCurrent()", baggage.asMap(), injectedBaggage.asMap());

        Baggage newBaggage = Baggage.builder().put("foo", "bar").build();
        logger.info("Baggage created during the test: " + newBaggage.toString());
        try (Scope s = newBaggage.makeCurrent()) {
            Baggage baggageTwo = Baggage.current();
            logger.info("Baggage.current() after creating a new baggage and making it current : " + baggageTwo.asMap());

            assertEquals("Didn't find expected value in injected baggage", "bar", injectedBaggage.getEntryValue("foo"));
            assertEquals("The current baggage was not the one we created and invoked makeCurrent() with", newBaggage.asMap(), baggageTwo.asMap());
            assertEquals("The injected baggage was not the same as the current baggage after calling makeCurrent()", baggageTwo.asMap(), injectedBaggage.asMap());
        }

    }
}
