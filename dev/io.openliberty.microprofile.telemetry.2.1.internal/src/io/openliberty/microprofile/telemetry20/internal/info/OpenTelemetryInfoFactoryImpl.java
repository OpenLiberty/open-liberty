/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry20.internal.info;

import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.common.info.AbstractOpenTelemetryInfoFactory;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.resources.HostResource;
import io.opentelemetry.instrumentation.resources.OsResource;
import io.opentelemetry.instrumentation.resources.ProcessResource;
import io.opentelemetry.instrumentation.resources.ProcessRuntimeResource;
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
//We want this to start before CDI so the meta data slot is ready before anyone triggers the CDI producer.
@Component(service = { OpenTelemetryInfoFactory.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public class OpenTelemetryInfoFactoryImpl extends AbstractOpenTelemetryInfoFactory {

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoFactoryImpl.class);

    private static final String DISABLED_RESOURCE_PROVIDERS = "otel.java.disabled.resource.providers";
    private static final String RESOURCES_PACKAGE = "io.opentelemetry.instrumentation.resources.";
    private static final String OS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "OsResourceProvider";
    private static final String HOST_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "HostResourceProvider";
    private static final String PROCESS_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessResourceProvider";
    private static final String PROCESS_RUNTIME_RESOURCE_PROVIDER = RESOURCES_PACKAGE + "ProcessRuntimeResourceProvider";

    // Version specific API calls to AutoConfiguredOpenTelemetrySdk.builder()
    @Override
    public OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser, ClassLoader classLoader) {

        OpenTelemetrySdk openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
                        .addPropertiesCustomizer(x -> openTelemetryProperties) //Overrides OpenTelemetry's property order
                        .addResourceCustomizer(resourceCustomiser)
                        .setServiceClassLoader(classLoader)
                        .disableShutdownHook()
                        .build()
                        .getOpenTelemetrySdk();

        return openTelemetry;
    }

    @Override
    protected ResourceBuilder customizeResource(Resource resource, ConfigProperties c, boolean isEnabled) {
        ResourceBuilder builder = super.customizeResource(resource, c, isEnabled);
        builder.put(OpenTelemetryConstants.KEY_SERVICE_INSTANCE_ID, UUID.randomUUID().toString());
        //Resource providers can be disabled with otel.java.disabled.resource.providers
        Set<String> disabledProviders = new HashSet<>(c.getList(DISABLED_RESOURCE_PROVIDERS));

        if(!disabledProviders.contains(OS_RESOURCE_PROVIDER)){
            builder.putAll(OsResource.get());
        }
        if(!disabledProviders.contains(HOST_RESOURCE_PROVIDER)){
            builder.putAll(HostResource.get());
        }
        if(!disabledProviders.contains(PROCESS_RESOURCE_PROVIDER)){
            builder.putAll(ProcessResource.get());
        }
        if(!disabledProviders.contains(PROCESS_RUNTIME_RESOURCE_PROVIDER)){
            builder.putAll(ProcessRuntimeResource.get());
        }
        return builder;
    }

    /** {@inheritDoc} */
    @Override
    protected void addDefaultVersionedProperties(Map<String, String> telemetryProperties) {
        //no op;
    }

    /** {@inheritDoc} */
    @Override
    protected void mergeInJVMMetrics(OpenTelemetry openTelemetry, boolean runtimeEnabled) {
        //JVM metrics are, naturally, created by the JVM and so are shared across apps.
        //Thus we only output them in runtime mode where they will not be misleading.
        if (openTelemetry != null && runtimeEnabled && runningOnJ9OrHotspot()) {
            // Register observers for runtime metrics
            Classes.registerObservers(openTelemetry);
            Cpu.registerObservers(openTelemetry);
            MemoryPools.registerObservers(openTelemetry);
            Threads.registerObservers(openTelemetry);
            GarbageCollector.registerObservers(openTelemetry);
        }
    }
}
