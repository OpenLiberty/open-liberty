/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.common.info;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.microprofile.telemetry.internal.common.constants.OpenTelemetryConstants;

public class OpenTelemetryPropertiesReader {

    private static final TraceComponent tc = Tr.register(OpenTelemetryPropertiesReader.class);

    //TODO document exactly how this is different from the other
    public static HashMap<String, String> getTelemetryProperties() {
        try {
            Config config = ConfigProvider.getConfig();

            HashMap<String, String> telemetryProperties = new HashMap<>();

            HashMap<String, String> propertyLocation = new HashMap<>();

            for (ConfigSource configSource : config.getConfigSources()) {

                configSource.getName();

                for (Entry<String, String> entry : configSource.getProperties().entrySet()) {
                    if (entry.getKey().startsWith("otel") || entry.getKey().startsWith("OTEL")) {
                        String normalizedName = entry.getKey().toLowerCase().replace('_', '.');
                        config.getOptionalValue(normalizedName, String.class)
                              .ifPresent(value -> {
                                  telemetryProperties.putIfAbsent(normalizedName, value);
                                  if (propertyLocation.containsKey(normalizedName)) {
                                      //conflictingPropertyWarning.
                                  } else {
                                      propertyLocation.put(normalizedName, configSource.getName());
                                  }
                              });
                    }
                }
            }

            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    //TODO document exactly how this is different from the other
    public static HashMap<String, String> getRuntimeInstanceTelemetryProperties() {
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

            return telemetryProperties;
        } catch (Exception e) {
            Tr.error(tc, Tr.formatMessage(tc, "CWMOT5002.telemetry.error", e));
            return new HashMap<String, String>();
        }
    }

    /**
     * Reads the oTelConfigs to see if open telemetry is disabled. Call this with the result of one of the two methods above
     *
     * @param oTelConfigs a map of open telemetry configuration properties acquired by {@link getTelemetryProperties} or {@link getRuntimeInstanceTelemetryProperties}
     * @return true if Open Telemetry is disabled otherwise false
     */
    public static boolean checkDisabled(Map<String, String> oTelConfigs) {
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(OpenTelemetryConstants.CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

}
