/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;

/**
 * Add our client filter when a new Rest Client is created.
 */
public class TelemetryRestClientBuilderListener implements RestClientBuilderListener {

    /** {@inheritDoc} */
    @Override
    public void onNewBuilder(RestClientBuilder builder) {
        TelemetryClientFilter currentFilter = TelemetryClientFilter.getCurrent();
        builder.register(currentFilter);
    }

}
