/*******************************************************************************
 * Copyright (c) 2023, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.runtime.metadata.ComponentMetaData;
import com.ibm.ws.threadContext.ComponentMetaDataAccessorImpl;

import io.openliberty.microprofile.telemetry.internal.common.AgentDetection;
import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;
import io.openliberty.microprofile.telemetry.internal.interfaces.OpenTelemetryInfoFactory;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.resources.ResourceBuilder;

public abstract class AbstractOpenTelemetryInfoFactory implements OpenTelemetryInfoFactory {

    private static final TraceComponent tc = Tr.register(AbstractOpenTelemetryInfoFactory.class);

    private static final String OS_BEAN_J9 = "com.ibm.lang.management.OperatingSystemMXBean";
    private static final String OS_BEAN_HOTSPOT = "com.sun.management.OperatingSystemMXBean";

    @Override
    public OpenTelemetryInfoInternal createOpenTelemetryInfo(boolean runtimeEnabled) {
        try {

            String instanceName = runtimeEnabled ? OpenTelemetryConstants.OTEL_RUNTIME_INSTANCE_NAME : ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor()
                                                                                                                                    .getComponentMetaData().getJ2EEName()
                                                                                                                                    .getApplication();

            if (AgentDetection.isAgentActive()) {
                // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
                // all config is handled by the agent in this case
                return new EnabledOpenTelemetryInfo(true, GlobalOpenTelemetry.get(), instanceName);
            }

            final Map<String, String> telemetryProperties;

            if (runtimeEnabled) {
                telemetryProperties = OpenTelemetryPropertiesReader.getRuntimeInstanceTelemetryProperties();
            } else {
                telemetryProperties = OpenTelemetryPropertiesReader.getTelemetryProperties();

                //Checks if app mode thinks we're enabled and runtime thinks we're not
                //The inverse condition is checked in the lifecycle manager. Because we won't create any
                //OpenTelemetryInfo for apps in runtime mode.
                warnIfAppEnabledAndRuntimeExplicitlyDisabled(telemetryProperties, instanceName);
            }

            //Builds tracer provider if user has enabled tracing aspects with config properties
            if (OpenTelemetryPropertiesReader.isEnabled(telemetryProperties)) {

                addDefaultVersionedProperties(telemetryProperties);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "OTEL instance {0} is being configured with the properties: {1}", instanceName, telemetryProperties);
                }

                OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                    ClassLoader classLoader = runtimeEnabled ? OpenTelemetry.noop().getClass().getClassLoader() : Thread.currentThread().getContextClassLoader();

                    return buildOpenTelemetry(telemetryProperties, getResourceCustomizer(runtimeEnabled), classLoader);
                });

                if (openTelemetry != null) {
                    mergeInJVMMetrics(openTelemetry, runtimeEnabled);
                    return new EnabledOpenTelemetryInfo(true, openTelemetry, instanceName);
                }
            }

            //By default, MicroProfile Telemetry tracing is off.
            //The absence of an installed SDK is a “no-op” API
            //Operations on a Tracer, or on Spans have no side effects and do nothing
            Tr.info(tc, "CWMOT5100.tracing.is.disabled", instanceName);

            return new DisabledOpenTelemetryInfo();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }

    }

    //No op in Telemetry 1.1 and 1.0
    protected void warnIfAppEnabledAndRuntimeExplicitlyDisabled(Map<String, String> telemetryAppProperties, String appName) {
        //Log a warning if we're in app mode and OTel is enabled but the runtime mode would think we OTel should be disabled.

        HashMap<String, String> runtimePropreties = OpenTelemetryPropertiesReader.getRuntimeInstanceTelemetryProperties();

        if (OpenTelemetryPropertiesReader.isEnabled(telemetryAppProperties) && OpenTelemetryPropertiesReader.checkExplicitlyDisabled(runtimePropreties)) {
            Tr.warning(tc, "CWMOT5007.tel.enabled.conflict", appName);
        }

    }

    protected abstract void addDefaultVersionedProperties(Map<String, String> telemetryProperties);

    protected abstract void mergeInJVMMetrics(OpenTelemetry openTelemetry, boolean runtimeEnabled);

    protected abstract OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties,
                                                        BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser,
                                                        ClassLoader classLoader);

    @Override
    public OpenTelemetryInfoInternal createDisposedOpenTelemetryInfo() {
        try {
            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();
            return new DisposedOpenTelemetryInfo(appName); //Getting appName like this was easier than writing a functional interface

        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    @Override
    public OpenTelemetryInfoInternal createDisabledOpenTelemetryInfo() {
        try {
            return new DisabledOpenTelemetryInfo();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    //Wrapper so we can override customizeResource;
    private BiFunction<? super Resource, ConfigProperties, ? extends Resource> getResourceCustomizer(final boolean runtimeEnabled) {
        return (Resource resource, ConfigProperties configProperties) -> {
            return this.customizeResource(resource, configProperties, runtimeEnabled).build();
        };
    }

    //Adds the service name to the resource attributes
    protected ResourceBuilder customizeResource(Resource resource, ConfigProperties c, boolean runtimeEnabled) {
        ResourceBuilder builder = resource.toBuilder();

        //Use a plain String not ResourceAttributes.SERVICE_NAME due to semconv moving the class around
        builder.put(OpenTelemetryConstants.KEY_SERVICE_NAME, getServiceName(c, runtimeEnabled));
        return builder;
    }

    //In app mode uses application name if the user has not given configured service.name resource attribute
    //In runtime mode it uses the constant "unknown_service"
    private String getServiceName(ConfigProperties c, boolean runtimeEnabled) {
        String serviceName = c.getString(OpenTelemetryConstants.SERVICE_NAME_PROPERTY);
        if (serviceName == null) {

            if (!runtimeEnabled) {
                ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                if (cmd != null) {
                    serviceName = cmd.getModuleMetaData().getApplicationMetaData().getName();
                }
            }

            if (serviceName == null) {
                serviceName = OpenTelemetryConstants.UNKNOWN_SERVICE;
            }
        }
        return serviceName;
    }

    @FFDCIgnore(ClassNotFoundException.class)
    protected boolean runningOnJ9OrHotspot() {

        Class<?> j9BeanClass = null;
        Class<?> hotspotBeanClass = null;

        try {
            j9BeanClass = Class.forName(OS_BEAN_J9);
        } catch (ClassNotFoundException ignored) {
        }

        try {
            hotspotBeanClass = Class.forName(OS_BEAN_HOTSPOT);
        } catch (ClassNotFoundException ignored) {
        }

        return j9BeanClass != null || hotspotBeanClass != null;
    }
}
