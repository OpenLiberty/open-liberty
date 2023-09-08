/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.rest;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Add our client filter when a new Rest Client is created.
 */
public class TelemetryRestClientBuilderListener implements RestClientBuilderListener {

    private static final TraceComponent tc = Tr.register(TelemetryRestClientBuilderListener.class);

    /** {@inheritDoc} */
    @Override
    public void onNewBuilder(RestClientBuilder builder) {
        try {
            TelemetryClientFilter currentFilter = new TelemetryClientFilter();
            if (currentFilter.isEnabled()) {
                builder.register(currentFilter);
            }
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }

}
