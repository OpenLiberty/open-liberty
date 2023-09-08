/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.rest;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import org.osgi.service.component.annotations.Component;

import io.openliberty.restfulWS.client.ClientBuilderListener;
import jakarta.ws.rs.client.ClientBuilder;

/**
 * Adds the Telemetry client filter when a rest client is built.
 * <p>
 * This gets called for both JAX-RS Client and MP Rest Client.
 */
@Component(configurationPolicy = IGNORE)
public class TelemetryClientBuilderListener implements ClientBuilderListener {

    @Override
    public void building(ClientBuilder clientBuilder) {
        TelemetryClientFilter currentFilter = new TelemetryClientFilter();
        if (currentFilter.isEnabled()) {
            clientBuilder.register(currentFilter);
        }
    }
}
