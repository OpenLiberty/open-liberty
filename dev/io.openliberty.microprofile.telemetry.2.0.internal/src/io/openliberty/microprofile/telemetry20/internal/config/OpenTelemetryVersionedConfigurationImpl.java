/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.config;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.OpenTelemetryInfoFactoryImpl;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

/**
 * This class contains version specific configuration for OpenTelemetryInfoFactory
 */
@Component
public class OpenTelemetryVersionedConfigurationImpl implements OpenTelemetryInfoFactoryImpl.OpenTelemetryVersionedConfiguration {

    private static final TraceComponent tc = Tr.register(OpenTelemetryVersionedConfigurationImpl.class);

    // Version specific API calls to AutoConfiguredOpenTelemetrySdk.builder()
    @Override
    public OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser, ClassLoader classLoader) {

        openTelemetryProperties.putAll(getTelemetryPropertyDefaults());

        OpenTelemetrySdk openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
                        .addPropertiesCustomizer(x -> openTelemetryProperties) //Overrides OpenTelemetry's property order
                        .addResourceCustomizer(resourceCustomiser) //Defaults service name to application name
                        .setServiceClassLoader(classLoader)
                        .disableShutdownHook()
                        .build()
                        .getOpenTelemetrySdk();

        // Register observers for runtime metrics
        Classes.registerObservers(openTelemetry);
        Cpu.registerObservers(openTelemetry);
        MemoryPools.registerObservers(openTelemetry);
        Threads.registerObservers(openTelemetry);
        GarbageCollector.registerObservers(openTelemetry);

        return openTelemetry;

    }

    // Version specific default properties
    private Map<String, String> getTelemetryPropertyDefaults() {
        Map<String, String> telemetryProperties = new HashMap<String, String>();
        return telemetryProperties;
    }

    @Override
    public OpenTelemetry createServerOpenTelemetryInfo(HashMap<String, String> telemetryProperties) {
        try {
            String otelInstanceName = OpenTelemetryConstants.OTEL_RUNTIME_INSTANCE_NAME;

            if (AgentDetection.isAgentActive()) {
                // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
                // all config is handled by the agent in this case
                return GlobalOpenTelemetry.get();
            }

            ClassLoader classLoader = OpenTelemetry.noop().getClass().getClassLoader();

            //Builds tracer provider if user has enabled tracing aspects with config properties
            if (!checkDisabled(telemetryProperties)) {
                OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                    return buildOpenTelemetry(telemetryProperties, OpenTelemetryVersionedConfigurationImpl::customizeResource, classLoader);
                });

                if (openTelemetry != null) {
                    return openTelemetry;
                }
            }

            //By default, MicroProfile Telemetry tracing is off.
            //The absence of an installed SDK is a “no-op” API
            //Operations on a Tracer, or on Spans have no side effects and do nothing

            Tr.info(tc, "CWMOT5100.tracing.is.disabled", otelInstanceName);

            return null;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return null;
        }

    }

    private static Resource customizeResource(Resource resource, ConfigProperties c) {
        ResourceBuilder builder = resource.toBuilder();
        builder.put(AttributeKey.stringKey("service.name"), OpenTelemetryConstants.OTEL_RUNTIME_INSTANCE_NAME);

        return builder.build();
    }

    private static boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

}
