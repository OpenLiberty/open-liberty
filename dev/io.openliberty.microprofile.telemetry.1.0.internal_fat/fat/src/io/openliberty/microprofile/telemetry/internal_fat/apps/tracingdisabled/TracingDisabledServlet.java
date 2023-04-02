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
package io.openliberty.microprofile.telemetry.internal_fat.apps.tracingdisabled;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.TracerProvider;
import jakarta.servlet.annotation.WebServlet;
import jakarta.inject.Inject;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@WebServlet("/TracingDisabledServlet")
public class TracingDisabledServlet extends FATServlet {

    @Inject
    OpenTelemetry openTelemetry;

    @Test
    public void testTelemetryDisabled() {
        assertEquals(openTelemetry.getTracerProvider(), TracerProvider.noop());
    }
}
