/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package io.openliberty.microprofile.telemetry.internal.helper;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import org.eclipse.microprofile.config.Config;
import java.util.HashMap;
import java.util.Map;
import jakarta.inject.Inject;
/**
 * Methods to detect whether the OpenTelemetry automatic instrumentation agent is active
 */
public class EnabledHelper {

    private static final String ENV_DISABLE_PROPERTY = "OTEL_SDK_DISABLED";
    private static final String CONFIG_DISABLE_PROPERTY = "otel.sdk.disabled";

    @Inject
    Config config;

    public boolean checkDisabled() {
        HashMap<String, String> oTelConfigs = getTelemetryProperties();
        //In order to enable any of the tracing aspects, the configuration otel.sdk.disabled=false must be specified in any of the configuration sources available via MicroProfile Config.
        if (oTelConfigs.get(ENV_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(ENV_DISABLE_PROPERTY));
        } else if (oTelConfigs.get(CONFIG_DISABLE_PROPERTY) != null) {
            return Boolean.valueOf(oTelConfigs.get(CONFIG_DISABLE_PROPERTY));
        }
        return true;
    }

    private HashMap<String, String> getTelemetryProperties() {
        HashMap<String, String> telemetryProperties = new HashMap<>();
        for (String propertyName : config.getPropertyNames()) {
            if (propertyName.startsWith("otel.")) {
                config.getOptionalValue(propertyName, String.class).ifPresent(
                                                                              value -> telemetryProperties.put(propertyName, value));
            }
        }
        return telemetryProperties;
    }

}
