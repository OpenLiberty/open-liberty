/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.oauth20.internal.config;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

public class OAuthEndpointSettings {

    private static final TraceComponent tc = Tr.register(OAuthEndpointSettings.class);

    public static final String KEY_NAME = "name";
    public static final String KEY_SUPPORTED_HTTP_METHODS = "supportedHttpMethods";

    private final Map<EndpointType, SpecificOAuthEndpointSettings> allOAuthEndpointSettings = new HashMap<EndpointType, SpecificOAuthEndpointSettings>();

    public Map<EndpointType, SpecificOAuthEndpointSettings> getAllOAuthEndpointSettings() {
        return allOAuthEndpointSettings;
    }

    public SpecificOAuthEndpointSettings getSpecificOAuthEndpointSettings(EndpointType endpointType) {
        return allOAuthEndpointSettings.get(endpointType);
    }

    @FFDCIgnore(RuntimeException.class)
    public void addOAuthEndpointSettings(Configuration endpointSettingsConfig) {
        if (endpointSettingsConfig == null) {
            return;
        }
        Dictionary<String, Object> configProps = endpointSettingsConfig.getProperties();
        if (configProps == null) {
            return;
        }
        SpecificOAuthEndpointSettings endpointSettings = null;
        try {
            EndpointType endpoint = getEndpointTypeFromConfigName((String) configProps.get(KEY_NAME));
            if (endpoint == null) {
                return;
            }
            endpointSettings = new SpecificOAuthEndpointSettings(endpoint);
            endpointSettings.setSupportedHttpMethods((String[]) configProps.get(KEY_SUPPORTED_HTTP_METHODS));
        } catch (RuntimeException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught an exception reading endpoint settings from the config: {0}. Config properties were {1}", e, configProps);
            }
        }
        updateAllEndpointSettings(endpointSettings);
    }

    @FFDCIgnore(IllegalArgumentException.class)
    EndpointType getEndpointTypeFromConfigName(String endpointName) {
        EndpointType endpointType = null;
        try {
            endpointType = EndpointType.valueOf(endpointName);
        } catch (IllegalArgumentException e) {
            // Didn't find easy match for endpoint name, so need to find the appropriate match
            endpointType = getNonStandardEndpointTypeFromConfigName(endpointName);
        }
        if (endpointType == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to find matching endpoint type for intput [" + endpointName + "]");
            }
        }
        return endpointType;
    }

    /**
     * The values defined in metatype.xml for the endpoint names don't necessarily match 1:1 with the values defined by the
     * EndpointType enum. This method finds those non-standard endpoint names.
     */
    EndpointType getNonStandardEndpointTypeFromConfigName(String endpointName) {
        if ("coverageMap".equals(endpointName)) {
            return EndpointType.coverage_map;
        } else if ("appPasswords".equals(endpointName)) {
            return EndpointType.app_password;
        } else if ("appTokens".equals(endpointName)) {
            return EndpointType.app_token;
        }
        return null;
    }

    void updateAllEndpointSettings(SpecificOAuthEndpointSettings newEndpointSettings) {
        if (newEndpointSettings == null) {
            return;
        }
        EndpointType newEndpointType = newEndpointSettings.getEndpointType();
        if (allOAuthEndpointSettings.containsKey(newEndpointType)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Endpoint settings already recorded for endpoint type {0}. Recorded settings are {1}. The following settings will be ignored: {2}", newEndpointType, allOAuthEndpointSettings.get(newEndpointType), newEndpointSettings);
            }
        } else {
            allOAuthEndpointSettings.put(newEndpointType, newEndpointSettings);
        }
    }

}
