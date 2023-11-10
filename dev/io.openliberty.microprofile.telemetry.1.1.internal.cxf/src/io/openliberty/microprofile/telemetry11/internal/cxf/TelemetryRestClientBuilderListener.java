/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry11.internal.cxf;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.spi.RestClientBuilderListener;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.microprofile.rest.client.component.CxfRestClientBeanBuilderListener;

import io.openliberty.microprofile.telemetry11.internal.rest.TelemetryClientFilter;

/**
 * This is the builder listener especially for CXF when it's constructing a client for injection.
 * <p>
 * It's needed because CXF doesn't respect the standard {@link RestClientBuilderListener} when doing injection, it only calls then when a builder is created via the API.
 */
@Component
public class TelemetryRestClientBuilderListener implements CxfRestClientBeanBuilderListener {

    /** {@inheritDoc} */
    @Override
    public void onNewBuilder(RestClientBuilder builder) {
        TelemetryClientFilter filter = new TelemetryClientFilter();
        if (filter.isEnabled()) {
            builder.register(filter);
        }
    }

}
