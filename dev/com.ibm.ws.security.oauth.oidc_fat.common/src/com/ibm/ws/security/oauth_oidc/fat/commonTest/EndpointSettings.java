/*******************************************************************************
 * Copyright (c) 2013, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth_oidc.fat.commonTest;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.simplicity.log.Log;

public class EndpointSettings {

    static private final Class<?> thisClass = EndpointSettings.class;
    private String testProviderType = null;

    public EndpointSettings() {

    }

    public class endpointSettings {

        String key;
        String value;

        public endpointSettings(String inKey, String inValue) {

            key = inKey;
            value = inValue;
        }

        public String getKey() {

            return key;
        };

        public String getValue() {

            return value;
        };

    }

    public List<endpointSettings> addEndpointSettingsIfNotNull(List<endpointSettings> parmList, String key, String value) throws Exception {

        if (value != null) {
            return addEndpointSettings(parmList, key, value);
        }
        return parmList;

    }

    public List<endpointSettings> addEndpointSettings(List<endpointSettings> parmList, String key, String value) throws Exception {

        try {
            if (parmList == null) {
                parmList = new ArrayList<endpointSettings>();
            }

            parmList.add(new endpointSettings(key, value));
            return parmList;
        } catch (Exception e) {
            Log
                    .info(thisClass, "addParm",
                            "Error occured while trying to set an parm during test setup");
            throw e;
        }
    }

    public void setOAUTHOPTestType() {

        testProviderType = Constants.OAUTH_OP;
    }

    public void setOIDCOPTestType() {

        testProviderType = Constants.OIDC_OP;
    }

    public String getProviderType() {

        return testProviderType;
    }

    // assemble the endpoint - use the default test wide provider type (tests are either running for OAuth or OpenIDConnect...
    // ie: "https://localhost:8020"  "OAuthConfigSample" "authorize"
    public String assembleEndpoint(String where, String type, String provider, String endpoint) throws Exception {

        return assembleEndpointAllParms(where, null, type, provider, endpoint);
    }

    // assemble the endpoint - use the passed in provider type (tests are either running for OAuth or OpenIDConnect...
    public String assembleEndpointAllParms(String where, String providerType, String type, String provider, String endpoint) throws Exception {

        String ptype = null;
        // did the caller over-ride the overall test type of OAuth or OpenIDConnect?
        if (providerType == null) {
            ptype = testProviderType;
        } else {
            ptype = providerType;
        }

        if (ptype.equals(Constants.OAUTH_OP)) {
            return where + "/" + Constants.OAUTH_ROOT + "/" + type + "/" + provider + getEndpointIfNotEmpty(endpoint);
        } else {
            if (ptype.equals(Constants.OIDC_OP)) {
                return where + "/" + Constants.OIDC_ROOT + "/" + type + "/" + provider + getEndpointIfNotEmpty(endpoint);
            } else {
                Log.info(thisClass, "assembleEndpointAllParms", "Unexpected provider type" + ptype);
            }

            return null;
        }
    }

    public String assembleProtectedResource(String where, String root, String resource) throws Exception {

        return where + "/" + root + "/" + resource;
    }

    /**
     * If endpoint is not null and not empty, return string '/<endpoint>' otherwise, return empty string
     *
     * @param endpoint
     * @return
     */
    private String getEndpointIfNotEmpty(String endpoint) {
        return (endpoint != null && !endpoint.isEmpty()) ? ("/" + endpoint) : "";
    }

}