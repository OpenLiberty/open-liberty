/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.rest;

public abstract class AbstractTelemetryContainerFilter {

    //This is here to prevent a build time dependency from the common package to the versioned packages.
    protected static final String SPAN_SCOPE = "otel.span.server.scope";

    /**
     * @return whether telemetry is enabled (i.e. whether the filter will actually do anything)
     */
    public abstract boolean isEnabled();
}
