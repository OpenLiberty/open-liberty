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
package io.openliberty.microprofile.telemetry.internal.common;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 * This produces an OpenTelemetry wrapper with info to state whether it is enabled or disabled
 */
public class OpenTelemetryInfo {

    private static final String INSTRUMENTATION_NAME = "io.openliberty.microprofile.telemetry";

    private final boolean enabled;
    private final OpenTelemetry openTelemetry;
    private final Tracer tracer;

    /**
     * @param enabled
     * @param openTelemetry
     */
    public OpenTelemetryInfo(boolean enabled, OpenTelemetry openTelemetry) {
        super();
        this.enabled = enabled;
        this.openTelemetry = openTelemetry;
        tracer = openTelemetry.getTracer(INSTRUMENTATION_NAME);
    }

    /**
     * No-args constructor for CDI
     */
    public OpenTelemetryInfo() {
        this.enabled = false;
        this.openTelemetry = null;
        tracer = null;
    }

    /**
     * @return the enabled
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @return the openTelemetry
     */
    public OpenTelemetry getOpenTelemetry() {
        return openTelemetry;
    }

    /**
     * @return the Tracer
     */
    public Tracer getTracer() {
        return tracer;
    }

}