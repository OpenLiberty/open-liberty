/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfoFactoryImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

/**
 * This class contains version specific configuration for OpenTelemetryInfoFactory
 */
@Component
public class OpenTelemetryVersionedConfigurationImpl implements OpenTelemetryInfoFactoryImpl.OpenTelemetryVersionedConfiguration {

    @Override
    public OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser, ClassLoader classLoader) {

        openTelemetryProperties.putAll(getTelemetryPropertyDefaults());

        return AutoConfiguredOpenTelemetrySdk.builder()
                        .addPropertiesCustomizer(x -> openTelemetryProperties) //Overrides OpenTelemetry's property order
                        .addResourceCustomizer(resourceCustomiser) //Defaults service name to application name
                        .setServiceClassLoader(classLoader)
                        .setResultAsGlobal(false)
                        .build()
                        .getOpenTelemetrySdk();

    }

    // Version specific default properties
    private Map<String, String> getTelemetryPropertyDefaults() {
        Map<String, String> telemetryProperties = new HashMap<String, String>();
        //Metrics and logs are disabled by default
        telemetryProperties.put(OpenTelemetryConstants.CONFIG_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.CONFIG_LOGS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.ENV_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.ENV_LOGS_EXPORTER_PROPERTY, "none");
        return telemetryProperties;
    }

    /** {@inheritDoc} */
    @Override
    public OpenTelemetry createServerOpenTelemetryInfo(HashMap<String, String> telemetryProperties) {
        // TODO Auto-generated method stub
        return null;
    }
}
