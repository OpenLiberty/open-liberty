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
package io.openliberty.security.openidconnect.server.config;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.cm.Configuration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.web.OAuth20Request.EndpointType;

@SuppressWarnings("restriction")
public class OidcEndpointSettings {

    private static final TraceComponent tc = Tr.register(OidcEndpointSettings.class);

    public static final String KEY_NAME = "name";
    public static final String KEY_SUPPORTED_HTTP_METHODS = "supportedHttpMethods";

    private final Map<EndpointType, SpecificOidcEndpointSettings> allOidcEndpointSettings = new HashMap<EndpointType, SpecificOidcEndpointSettings>();

    public Map<EndpointType, SpecificOidcEndpointSettings> getAllOidcEndpointSettings() {
        return allOidcEndpointSettings;
    }

    public SpecificOidcEndpointSettings getSpecificOidcEndpointSettings(EndpointType endpointType) {
        return allOidcEndpointSettings.get(endpointType);
    }

    @FFDCIgnore(RuntimeException.class)
    public void addOidcEndpointSettings(Configuration endpointSettingsConfig) {
        if (endpointSettingsConfig == null) {
            return;
        }
        Dictionary<String, Object> configProps = endpointSettingsConfig.getProperties();
        if (configProps == null) {
            return;
        }
        SpecificOidcEndpointSettings endpointSettings = null;
        try {
            EndpointType endpoint = getEndpointTypeFromConfigName((String) configProps.get(KEY_NAME));
            if (endpoint == null) {
                return;
            }
            endpointSettings = new SpecificOidcEndpointSettings(endpoint);
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
        if ("endSession".equals(endpointName)) {
            return EndpointType.end_session;
        } else if ("checkSessionIframe".equals(endpointName)) {
            return EndpointType.check_session_iframe;
        }
        return null;
    }

    void updateAllEndpointSettings(SpecificOidcEndpointSettings newEndpointSettings) {
        if (newEndpointSettings == null) {
            return;
        }
        EndpointType newEndpointType = newEndpointSettings.getEndpointType();
        if (allOidcEndpointSettings.containsKey(newEndpointType)) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Endpoint settings already recorded for endpoint type {0}. Recorded settings are {1}. The following settings will be ignored: {2}", newEndpointType, allOidcEndpointSettings.get(newEndpointType), newEndpointSettings);
            }
        } else {
            allOidcEndpointSettings.put(newEndpointType, newEndpointSettings);
        }
    }

}
