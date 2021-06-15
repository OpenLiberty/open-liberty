/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package io.openliberty.microprofile.health30.spi.impl;

import java.io.IOException;

import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.spi.HealthCheckResponseProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.health30.impl.HealthCheck30ResponseBuilderImpl;

@Component(service = { HealthCheckResponseProvider.class }, property = { "service.vendor=IBM" }, immediate = true)
public class HealthCheck30ResponseProviderImpl implements HealthCheckResponseProvider {

    private static final TraceComponent tc = Tr.register(HealthCheck30ResponseProviderImpl.class);

    /**
     * constructor
     */
    public HealthCheck30ResponseProviderImpl() {
    }

    /**
     * set the provider
     *
     */
    @Activate
    public void activate() {
        HealthCheckResponse.setResponseProvider(this);
    }

    /**
     * Set the provider to null
     *
     */
    @Deactivate
    public void deactivate() throws IOException {
        HealthCheckResponse.setResponseProvider(null);
    }

    @Override
    public HealthCheckResponseBuilder createResponseBuilder() {
        HealthCheckResponseBuilder builder = new HealthCheck30ResponseBuilderImpl();
        return builder;
    }

}
