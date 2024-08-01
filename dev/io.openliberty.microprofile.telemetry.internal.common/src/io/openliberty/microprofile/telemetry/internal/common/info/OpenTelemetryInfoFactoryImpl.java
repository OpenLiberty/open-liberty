/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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

// We want this to start before CDI so the meta data slot is ready before anyone triggers the CDI producer.
@Component(service = { OpenTelemetryInfoFactory.class }, property = { "service.vendor=IBM", "service.ranking:Integer=1500" })
public abstract class OpenTelemetryInfoFactoryImpl implements OpenTelemetryInfoFactory {

    /*
     * Class summary.
     *
     * This class consists of three parts
     * 1: Methods for producing instances of OpenTelemetryInfo and its wrapped OpenTelemetry which are used by
     * suppliers (see below)
     *
     * 2. An ApplicationStateListener which controls the state of OpenTelemetryInfo in a lazy fashion.
     *
     * On startup the ASL populates ApplicationMetaData with a lazy supplier of OpenTelemetryInfo.
     *
     * On shutdown the ASL replaces the supplier in ApplicationMetaData with a supplier that will return a DisposedOpenTelemetryInfo
     * (DisposedOpenTelemetryInfo logs warnings and no-ops when it is used).
     * The ASL also OpenTelemetryInfo.dispose() which will clean up an open telemetry object.
     *
     * To ensure threads do not get the wrong OpenTelemetryInfo from the application metadata, it is stored as an AtomicReference
     */

    private static final TraceComponent tc = Tr.register(OpenTelemetryInfoFactoryImpl.class);

    //This contains API calls that change between the upstream open telemetry version.
    //We get a partially configued SDK Builder from OSGi becase we are in a static context
    //and do not know which version of mpTelemetry will be in use.
    private final OpenTelemetryVersionedConfiguration openTelemetryVersionedConfiguration;

    @Activate
    public OpenTelemetryInfoFactoryImpl(@Reference OpenTelemetryVersionedConfiguration openTelemetryVersionedConfiguration) {

        this.openTelemetryVersionedConfiguration = openTelemetryVersionedConfiguration;
    }

    @Override
    public OpenTelemetryInfo createOpenTelemetryInfo() {
        try {

            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();

            if (AgentDetection.isAgentActive()) {
                // If we're using the agent, it will have set GlobalOpenTelemetry and we must use its instance
                // all config is handled by the agent in this case
                return new EnabledOpenTelemetryInfo(true, GlobalOpenTelemetry.get(), appName);
            }

            final Map<String, String> telemetryProperties = getTelemetryProperties();

            //Builds tracer provider if user has enabled tracing aspects with config properties
            if (!checkDisabled(telemetryProperties)) {
                OpenTelemetry openTelemetry = AccessController.doPrivileged((PrivilegedAction<OpenTelemetry>) () -> {
                    return openTelemetryVersionedConfiguration.buildOpenTelemetry(telemetryProperties,
                                                                                  this::customizeResource, Thread.currentThread().getContextClassLoader());
                });

                if (openTelemetry != null) {
                    return new EnabledOpenTelemetryInfo(true, openTelemetry, appName);
                }
            }

            //By default, MicroProfile Telemetry tracing is off.
            //The absence of an installed SDK is a “no-op” API
            //Operations on a Tracer, or on Spans have no side effects and do nothing
            Tr.info(tc, "CWMOT5100.tracing.is.disabled", appName);

            return new DisabledOpenTelemetryInfo();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }

    }

    @Override
    public OpenTelemetryInfo createDisposedOpenTelemetryInfo() {
        try {
            String appName = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData().getJ2EEName().getApplication();
            return new DisposedOpenTelemetryInfo(appName); //Getting appName like this was easier than writing a functional interface

        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
    }

    @Override
    public OpenTelemetryInfo createDisabledOpenTelemetryInfo() {
        try {
            return new DisabledOpenTelemetryInfo();
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new ErrorOpenTelemetryInfo();
        }
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

    private HashMap<String, String> getTelemetryProperties() {
        try {
            Config config = ConfigProvider.getConfig();

            HashMap<String, String> telemetryProperties = new HashMap<>();

            for (ConfigSource configSource : config.getConfigSources()) {
                for (Entry<String, String> entry : configSource.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("otel") || entry.getKey().startsWith("OTEL")) {
                        String normalizedName = entry.getKey().toLowerCase().replace('_', '.');
                        config.getOptionalValue(normalizedName, String.class)
                              .ifPresent(value -> telemetryProperties.putIfAbsent(normalizedName, value));
                    }
                }
            }

            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    private HashMap<String, String> getServerTelemetryProperties() {
        try {
            HashMap<String, String> telemetryProperties = new HashMap<>();

            AccessController.doPrivileged(new PrivilegedAction<String>() {
                @Override
                public String run() {
                    Map<String, String> envProperties = System.getenv();
                    Map<Object, Object> systemProperties = System.getProperties();

                    HashMap<String, String> tempProperties = new HashMap<>();

                    //Check system environment for all configured OTEL properties
                    for (String propertyName : envProperties.keySet()) {
                        if (propertyName.startsWith("otel") || propertyName.startsWith("OTEL")) {

                            String normalizedName = propertyName.toLowerCase().replace('_', '.');
                            String propertyValue = envProperties.get(propertyName);

                            if (propertyValue != null)
                                tempProperties.put(normalizedName, propertyValue);
                        }
                    }

                    //Check system properties for all configured OTEL properties and replace any
                    //previously configured values found in the system properties.
                    for (Object propertyName : systemProperties.keySet()) {
                        String normalizedName = ((String) propertyName).toLowerCase().replace('_', '.');
                        if (normalizedName.startsWith("otel")) {
                            String propertyValue = (String) systemProperties.get(propertyName);

                            if (tempProperties.containsKey(normalizedName))
                                tempProperties.remove(normalizedName);

                            if (propertyValue != null)
                                tempProperties.put(normalizedName, propertyValue);
                        }

                    }

                    //Only add telemetry properties if OTEL is enabled.
                    if (tempProperties.containsKey(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY)
                        && "false".equalsIgnoreCase(tempProperties.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY))) {
                        telemetryProperties.putAll(tempProperties);
                    }
                    return null;
                }
            });

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Runtime OTEL instance is being configured with the properties: {0}", telemetryProperties);
            }
            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    //Adds the service name to the resource attributes
    private Resource customizeResource(Resource resource, ConfigProperties c) {
        ResourceBuilder builder = resource.toBuilder();

        //Don't use ResourceAttributes.SERVICE_NAME due to semcov moving the class around
        builder.put(OpenTelemetryConstants.KEY_SERVICE_NAME, getServiceName(c));
        return builder.build();
    }

    //Uses application name if the user has not given configured service.name resource attribute
    private String getServiceName(ConfigProperties c) {
        String serviceName = c.getString(OpenTelemetryConstants.SERVICE_NAME_PROPERTY);
        if (serviceName == null) {

            if (!isRuntimeEnabled()) {
                ComponentMetaData cmd = ComponentMetaDataAccessorImpl.getComponentMetaDataAccessor().getComponentMetaData();
                if (cmd != null) {
                    serviceName = cmd.getModuleMetaData().getApplicationMetaData().getName();
                }
            }

            if (serviceName == null) {
                serviceName = "unkown_service";
            }
        }
        return serviceName;
    }

    @Override
    public abstract boolean isRuntimeEnabled();

    //Interfaces and private classes only relevent to this factory.

    public interface OpenTelemetryVersionedConfiguration {
        OpenTelemetry buildOpenTelemetry(Map<String, String> openTelemetryProperties, BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomiser,
                                         ClassLoader classLoader);

        OpenTelemetry createServerOpenTelemetryInfo(HashMap<String, String> hashMap);
    }
}
