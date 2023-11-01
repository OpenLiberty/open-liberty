/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.rest.jakarta;

import static org.osgi.service.component.annotations.ConfigurationPolicy.IGNORE;

import javax.ws.rs.client.ClientBuilder;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;

import io.openliberty.microprofile.telemetry11.internal.rest.TelemetryClientFilter;
import io.openliberty.restfulWS.client.ClientBuilderListener;

/**
 * Adds the Telemetry client filter when a rest client is built.
 * <p>
 * This gets called for both JAX-RS Client and MP Rest Client.
 */
@Component(configurationPolicy = IGNORE)
public class TelemetryClientBuilderListener implements ClientBuilderListener {

    @Override
    public void building(ClientBuilder clientBuilder) {
        try {
            TelemetryClientFilter currentFilter = new TelemetryClientFilter();
            if (currentFilter.isEnabled()) {
                clientBuilder.register(currentFilter);
            }
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
        }
    }
}
