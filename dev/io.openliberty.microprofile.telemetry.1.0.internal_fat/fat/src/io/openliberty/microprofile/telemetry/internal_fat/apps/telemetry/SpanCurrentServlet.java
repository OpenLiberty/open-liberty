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
package io.openliberty.microprofile.telemetry.internal_fat.apps.telemetry;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import javax.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/testSpanCurrent")
public class SpanCurrentServlet extends FATServlet {

    @Test
    public void testGetCurrentSpan_Default() {
        // Need to create a new thread so that the current context created by the HTTP tracing would not propagate.
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(() -> {
            Span span = Span.current();
            assertEquals(span, Span.getInvalid()); //Current span has no context as none was created
        });
        executorService.shutdown();
    }

    @Test
    public void testGetCurrentSpan_SetSpan() {
        Span span = Span.wrap(SpanContext.getInvalid()); //Creates span with no context
        try (Scope ignored = Context.current().with(span).makeCurrent()) {
            assertEquals(Span.current(), span);
        }
    }

}
