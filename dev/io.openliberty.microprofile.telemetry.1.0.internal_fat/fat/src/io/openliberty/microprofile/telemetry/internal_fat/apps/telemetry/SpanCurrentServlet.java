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

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.annotation.WebServlet;

@SuppressWarnings("serial")
@WebServlet("/SpanCurrentServlet")
public class SpanCurrentServlet extends FATServlet {

    @Test
    public void testGetCurrentSpan_Default() {
        Span span = Span.current();
        assertEquals(span, Span.getInvalid()); //Current span has no context as none was created
    }

    @Test
    public void testGetCurrentSpan_SetSpan() {
        Span span = Span.wrap(SpanContext.getInvalid()); //Creates span with no context
        try (Scope ignored = Context.current().with(span).makeCurrent()) {
            assertEquals(Span.current(), span);
        }
    }

}
