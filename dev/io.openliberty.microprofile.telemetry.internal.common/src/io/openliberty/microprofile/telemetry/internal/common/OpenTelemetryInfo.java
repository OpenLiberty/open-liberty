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

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

public interface OpenTelemetryInfo {
    /**
     * @return true if enabled
     */
    public boolean getEnabled();

    /**
     * @return the openTelemetry
     */
    public OpenTelemetry getOpenTelemetry();

    /**
     * @return the Tracer
     */
    public Tracer getTracer();

    /**
     * Disposes of the OpenTelemetry object within
     */
    void dispose();

}
