/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;

/**
 * An impl of OpenTelemetryInfo that occurs if an error occurred creating an OpenTelemetryInfo object
 */
public class ErrorOpenTelemetryInfo implements OpenTelemetryInfo {

    /** {@inheritDoc} */
    @Override
    public boolean getEnabled() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetry getOpenTelemetry() {
        return OpenTelemetry.noop();
    }

    /** {@inheritDoc} */
    @Override
    public Tracer getTracer() {
        return OpenTelemetry.noop().getTracer("");
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        // No op

    }
}
