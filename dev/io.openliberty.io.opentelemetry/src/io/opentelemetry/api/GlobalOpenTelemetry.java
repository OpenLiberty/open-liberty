/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * Originally, if a global OpenTelemetry instance is not set then GlobalOpenTelemetry.get()
 * attempts to create and configure one automatically.
 *
 * To support per-application configuration, the global instance is set to a no-op
 * implementation in this class
 */
package io.opentelemetry.api;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.TracerBuilder;
import io.opentelemetry.api.trace.TracerProvider;

public final class GlobalOpenTelemetry {

    private static final TraceComponent tc = Tr.register(GlobalOpenTelemetry.class);
    private static boolean warned = false;

    public static OpenTelemetry get() {
        //Warns the user the first time they try to get the GlobalOpenTelemetry instance
        //The GlobalOpenTelemetry instance should not be used as the entrypoint to telemetry functionality.
        //OpenTCWMOT5000W: Cannot get GlobalOpenTelemetry.get
        if (!warned) {
            String msg = Tr.formatMessage(tc, "CWMOT5000.cannot.get.globalopentelemetry");
            Tr.warning(tc, msg);
            warned = true;
        }
        return OpenTelemetry.noop();
    }

    //GlobalOpenTelemetry cannot be set twice
    public static void set(OpenTelemetry openTelemetry) {
        throw new IllegalStateException(Tr.formatMessage(tc, "CWMOT5001.cannot.set.globalopentelemetry"));
    }

    public static TracerProvider getTracerProvider() {
        return get().getTracerProvider();
    }

    public static Tracer getTracer(String instrumentationScopeName) {
        return get().getTracer(instrumentationScopeName);
    }

    public static TracerBuilder tracerBuilder(String instrumentationScopeName) {
        return get().tracerBuilder(instrumentationScopeName);
    }
}
