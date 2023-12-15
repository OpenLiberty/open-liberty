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
package io.openliberty.microprofile.telemetry.internal_fat.apps.shim;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.opentracing.Tracer;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;

/**
 * JAXRS service.
 */
@ApplicationScoped
@WebServlet("/testShim")
public class OpenTracingShimServlet extends FATServlet {
    /**
     * <p>The open tracing tracer. Will bew injected as a shim from OpenTelemetry.</p>
     */
    private Tracer tracer;

    @Inject
    void createShim(OpenTelemetry ot) {
        tracer = OpenTracingShim.createTracerShim(ot);
    }

    /**
     * Injected class with Traced annotation on the class.
     */
    @Inject
    private TracedBean bean;

    @Test
    public void testOpenTracingShim() {
        bean.annotatedClassMethodImplicitlyTraced(tracer);
    }

}
