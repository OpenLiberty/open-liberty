/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    public HealthCheck30ResponseProviderImpl() {}

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
