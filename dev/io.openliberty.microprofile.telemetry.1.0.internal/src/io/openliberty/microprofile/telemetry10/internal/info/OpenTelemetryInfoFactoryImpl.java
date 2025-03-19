/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry10.internal.info;

import java.util.Map;
import java.util.function.BiFunction;

import org.osgi.service.component.annotations.Component;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.AbstractOpenTelemetryInfoFactory;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;

/**
 * This class contains version specific configuration for OpenTelemetryInfoFactory
 */

// We want this to start before CDI so the meta data slot is ready before anyone triggers the CDI producer.
@Component(service = { OpenTelemetryInfoFactory.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public class OpenTelemetryInfoFactoryImpl extends AbstractOpenTelemetryInfoFactory {

    @Override
    public OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser, ClassLoader classLoader) {

        return AutoConfiguredOpenTelemetrySdk.builder()
                        .addPropertiesCustomizer(x -> openTelemetryProperties) //Overrides OpenTelemetry's property order
                        .addResourceCustomizer(resourceCustomiser)
                        .setServiceClassLoader(classLoader)
                        .setResultAsGlobal(false)
                        .build()
                        .getOpenTelemetrySdk();

    }

    /** {@inheritDoc} */
    @Override
    protected void addDefaultVersionedProperties(Map<String, String> telemetryProperties) {
        //Without these open telemetry breaks as we do not support metrics/logs on this version.
        //so override even if they're explicitly set.
        telemetryProperties.put(OpenTelemetryConstants.CONFIG_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.CONFIG_LOGS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.ENV_METRICS_EXPORTER_PROPERTY, "none");
        telemetryProperties.put(OpenTelemetryConstants.ENV_LOGS_EXPORTER_PROPERTY, "none");
    }

    @Override
    protected void mergeInJVMMetrics(OpenTelemetry openTelemetry, boolean runtimeEnabled) {
        //No op before Telemetry 2.0
    }

    @Override
    protected void warnIfAppEnabledAndRuntimeExplicitlyDisabled(Map<String, String> telemetryAppProperties, String appName) {
        //No op before Telemetry 2.0
    }
}
