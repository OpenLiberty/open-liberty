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
/**
 * This produces an OpenTelemetry bean with info to state whether it is enabled or disabled
 */
public class OpenTelemetryInfo {

    private final boolean enabled;
    private final OpenTelemetry openTelemetry;

    /**
     * @param enabled
     * @param openTelemetry
     */
    public OpenTelemetryInfo(boolean enabled, OpenTelemetry openTelemetry) {
        super();
        this.enabled = enabled;
        this.openTelemetry = openTelemetry;
    }

    /**
     * No-args constructor for CDI
     */
    public OpenTelemetryInfo() {
        this.enabled = false;
        this.openTelemetry = null;
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

}