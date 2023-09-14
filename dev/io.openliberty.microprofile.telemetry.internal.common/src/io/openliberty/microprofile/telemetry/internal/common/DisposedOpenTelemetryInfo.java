/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 *
 */
public class DisposedOpenTelemetryInfo implements OpenTelemetryInfo {

    private static final TraceComponent tc = Tr.register(DisposedOpenTelemetryInfo.class);

    private String appName = "Unkown";

    public DisposedOpenTelemetryInfo() {
    }

    public DisposedOpenTelemetryInfo(String appName) {
        this.appName = appName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean getEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetry getOpenTelemetry() {
        logWarning();
        return OpenTelemetry.noop();
    }

    /** {@inheritDoc} */
    @Override
    public Tracer getTracer() {
        logWarning();
        return OpenTelemetry.noop().getTracerProvider().get("");
    }

    @Override
    public void dispose() {
        //no op
    }

    private void logWarning() {
        Tr.warning(tc, Tr.formatMessage(tc, "CWMOT5003.factory.used.after.shutdown", appName));
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Exception e = new Exception();
            ByteArrayOutputStream stackStream = new ByteArrayOutputStream();
            PrintStream stackPrintStream = new PrintStream(stackStream);
            e.printStackTrace(stackPrintStream);

            Tr.event(tc, "OpenTelemetryInfoFactory", "The stack that led to OpenTelemetryInfoFactory being called after " + appName + " has shutdown is:.");
            Tr.event(tc, stackStream.toString());
        }
    }
}
