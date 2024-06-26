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

/**
 * An impl of OpenTelemetryInfo that occurs if an error occurred creating an OpenTelemetryInfo object
 */
public class ErrorOpenTelemetryInfo implements OpenTelemetryInfoInternal {

    public final static ErrorOpenTelemetryInfo INSTANCE = new ErrorOpenTelemetryInfo();

    //For functional interfaces that expect a method
    public static ErrorOpenTelemetryInfo getInstance() {
        return INSTANCE;
    }

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
    public void dispose() {
        // No op

    }
}
