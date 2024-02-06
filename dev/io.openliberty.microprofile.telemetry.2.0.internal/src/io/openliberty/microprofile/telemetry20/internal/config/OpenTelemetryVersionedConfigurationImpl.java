/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.config;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfoFactoryImpl;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;

/**
 * This class contains version specific configuration for OpenTelemetryInfoFactory
 */
@Component
public class OpenTelemetryVersionedConfigurationImpl implements OpenTelemetryInfoFactoryImpl.OpenTelemetryVersionedConfiguration {

    // Version specific API calls to AutoConfiguredOpenTelemetrySdk.builder()
    @Override
    public AutoConfiguredOpenTelemetrySdkBuilder getPartiallyConfiguredOpenTelemetrySDKBuilder() {
        return AutoConfiguredOpenTelemetrySdk.builder()
                        .disableShutdownHook();
    }

    // Version specific default properties
    @Override
    public Map<String, String> getTelemetryPropertyDefaults() {
        Map<String, String> telemetryProperties = new HashMap<String, String>();
        //Logs are disabled by default, metrics is not
        telemetryProperties.put(OpenTelemetryInfoFactoryImpl.CONFIG_LOGS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryInfoFactoryImpl.ENV_LOGS_EXPORTER_PROPERTY, "none");
        return telemetryProperties;
    }
}
