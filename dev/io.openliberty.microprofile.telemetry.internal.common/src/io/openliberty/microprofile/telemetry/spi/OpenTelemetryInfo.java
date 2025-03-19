/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.spi;

import io.opentelemetry.api.OpenTelemetry;

public interface OpenTelemetryInfo {
    /**
     * Checks if the contained OpenTelemetry object is a no-op at the time of creation. This method is intended to be
     * used to avoid activating functions that uses OpenTelemetry if OpenTelemetry will no-op for performance enhancement.
     *
     * @return true if the contained OpenTelemetry object was enabled at the time this OpenTelemetryInfo was created
     *         and false if it is a no-op
     */
    public boolean isEnabled();

    /**
     * Get the contained OpenTelemetry object
     *
     * @return the openTelemetry object contained within this OpenTelemetryInfo
     */
    public OpenTelemetry getOpenTelemetry();

}
