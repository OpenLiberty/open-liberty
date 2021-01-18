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
package com.ibm.ws.security.saml20.fat.commonTest.config.settings;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.config.settings.BaseConfigSettings;

public class AuthFilterSettings extends BaseConfigSettings {

    private Class<?> thisClass = AuthFilterSettings.class;

    public static final String CONFIG_ELEMENT_NAME = "authFilter";

    public static final String VAR_AUTH_FILTER_ID = "${auth.filter.id}";

    public static final String ATTR_AUTH_FILTER_ID = "id";

    private String id = "myAuthFilter1";
    private Map<String, RequestUrlSettings> requestUrlSettings = new HashMap<String, RequestUrlSettings>();

    public AuthFilterSettings() {
        configElementName = CONFIG_ELEMENT_NAME;
    }

    public AuthFilterSettings(String id, Map<String, RequestUrlSettings> requestUrlSettings) {
        this();

        this.id = id;

        if (requestUrlSettings != null) {
            for (String reqUrlId : requestUrlSettings.keySet()) {
                RequestUrlSettings settings = requestUrlSettings.get(reqUrlId);
                RequestUrlSettings copiedSettings = settings.copyConfigSettings();
                this.requestUrlSettings.put(reqUrlId, copiedSettings);

                // The requestUrl settings are child elements of the authFilter element
                //putChildConfigSettings(reqUrlId, copiedSettings);
            }
        }
    }

    @Override
    public AuthFilterSettings createShallowCopy() {
        return new AuthFilterSettings(id, requestUrlSettings);
    }

    @Override
    public AuthFilterSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigAttributesMap() {
        String method = "getConfigAttributesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of auth filter config attributes");
        }
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(ATTR_AUTH_FILTER_ID, getId());

        return attributes;
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of auth filter settings config variables");
        }
        Map<String, String> settings = new HashMap<String, String>();

        settings.put(VAR_AUTH_FILTER_ID, getId());

        return settings;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    /**
     * Creates a default RequestUrlSettings object and puts it into the map for request URL settings.
     */
    public void setDefaultRequestUrlSettings() {
        String method = "setDefaultRequestUrlSettings";

        RequestUrlSettings reqUrlSettings = new RequestUrlSettings();
        String reqUrlId = reqUrlSettings.getId();

        if (debug) {
            Log.info(thisClass, method, "Setting default request URL settings with id: " + reqUrlId);
        }

        this.requestUrlSettings.put(reqUrlId, reqUrlSettings);
        putChildConfigSettings(reqUrlId, reqUrlSettings);
    }

    public Map<String, RequestUrlSettings> getAllRequestUrlSettings() {
        return this.requestUrlSettings;
    }

    public void removeAllRequestUrlSettings() {
        this.requestUrlSettings.clear();
    }

    /**
     * Gets the only request URL settings tracked by this class. If no, or more than 1, sets of settings are being
     * tracked, this will throw an exception.
     * 
     * @return
     * @throws Exception
     */
    public RequestUrlSettings getDefaultRequestUrlSettings() throws Exception {
        return getDefaultSettings(this.requestUrlSettings);
    }

    public void setRequestUrlSettings(String id, RequestUrlSettings requestUrlSettings) {
        this.requestUrlSettings.put(id, requestUrlSettings.copyConfigSettings());
    }

    public RequestUrlSettings getRequestUrlSettings(String id) {
        return this.requestUrlSettings.get(id);
    }

    public void removeRequestUrlSettings(String id) {
        RequestUrlSettings settings = requestUrlSettings.get(id);
        this.requestUrlSettings.remove(id);
        removeChildConfigSettings(id, settings);
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "Auth filter config settings: ");
        Log.info(thisClass, thisMethod, indent + ATTR_AUTH_FILTER_ID + ": " + id);

        for (String id : requestUrlSettings.keySet()) {
            RequestUrlSettings settings = requestUrlSettings.get(id);
            settings.printConfigSettings();
        }
    }

}
