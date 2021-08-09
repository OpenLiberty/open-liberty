/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.saml.fat.jaxrs.config.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.config.settings.ConfigSettings;
import com.ibm.ws.security.fat.common.config.settings.FeatureSettings;
import com.ibm.ws.security.fat.common.config.settings.SSLConfigSettings;
import com.ibm.ws.security.saml20.fat.commonTest.config.settings.AuthFilterSettings;

public class RSSamlConfigSettings extends ConfigSettings {

    private static final Class<?> thisClass = RSSamlConfigSettings.class;

    protected Map<String, RSSamlProviderSettings> rsSamlProviderSettings = new HashMap<String, RSSamlProviderSettings>();
    protected Map<String, AuthFilterSettings> authFilterSettings = new HashMap<String, AuthFilterSettings>();

    public static final String VAR_RS_SAML_PROVIDERS = "${rs.saml.providers}";
    public static final String VAR_AUTH_FILTERS = "${auth.filters}";

    public RSSamlConfigSettings() {
    }

    public RSSamlConfigSettings(FeatureSettings featureSettings, List<String> registryFiles, SSLConfigSettings sslConfigSettings,
            List<String> applicationFiles, String miscFile, Map<String, RSSamlProviderSettings> rsSamlProviderSettings,
            Map<String, AuthFilterSettings> authFilterSettings) {

        super(featureSettings, registryFiles, sslConfigSettings, applicationFiles, miscFile);

        if (rsSamlProviderSettings != null) {
            for (String id : rsSamlProviderSettings.keySet()) {
                RSSamlProviderSettings settings = rsSamlProviderSettings.get(id);
                this.rsSamlProviderSettings.put(id, settings.copyConfigSettings());
            }
        }
        if (authFilterSettings != null) {
            for (String id : authFilterSettings.keySet()) {
                AuthFilterSettings settings = authFilterSettings.get(id);
                this.authFilterSettings.put(id, settings.copyConfigSettings());
            }
        }
    }

    @Override
    public RSSamlConfigSettings createShallowCopy() {
        return new RSSamlConfigSettings(featureSettings, registryFiles, sslConfigSettings, applicationFiles, miscFile,
                rsSamlProviderSettings, authFilterSettings);
    }

    @Override
    public RSSamlConfigSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of RS SAML settings config variables");
        }
        // This class extends ConfigSettings instead of BaseConfigSettings, so we need to first obtain the normal
        // config settings tracked by the ConfigSettings class
        Map<String, String> settings = super.getConfigSettingsVariablesMap();

        // RS SAML providers
        StringBuilder rsSamlProviderOutput = new StringBuilder();
        for (String id : rsSamlProviderSettings.keySet()) {
            RSSamlProviderSettings providerSettings = rsSamlProviderSettings.get(id);
            rsSamlProviderOutput.append(providerSettings.buildConfigOutput());
        }
        settings.put(VAR_RS_SAML_PROVIDERS, rsSamlProviderOutput.toString());

        // Auth filters
        StringBuilder authFilterOutput = new StringBuilder();
        for (String id : authFilterSettings.keySet()) {
            AuthFilterSettings filterSettings = authFilterSettings.get(id);
            authFilterOutput.append(filterSettings.buildConfigOutput());
        }
        settings.put(VAR_AUTH_FILTERS, authFilterOutput.toString());

        return settings;
    }

    /**
     * Creates a default RSSamlProviderSettings and puts it into the map for RS SAML settings.
     */
    public void setDefaultRSSamlProviderSettings() {
        String method = "setDefaultRSSamlProviderSettings";

        RSSamlProviderSettings spSettings = new RSSamlProviderSettings();
        String spId = spSettings.getId();

        if (debug) {
            Log.info(thisClass, method, "Setting default RS SAML provider settings with id: " + spId);
        }

        rsSamlProviderSettings.put(spId, spSettings);
    }

    public Map<String, RSSamlProviderSettings> getAllRSSamlProviderSettings() {
        return rsSamlProviderSettings;
    }

    public void removeAllRSSamlProviderSettings() {
        rsSamlProviderSettings.clear();
    }

    public void setRSSamlProviderSettings(String id, RSSamlProviderSettings rsSamlProviderSettings) {
        this.rsSamlProviderSettings.put(id, rsSamlProviderSettings.copyConfigSettings());
    }

    /**
     * Gets the only RS SAML provider settings tracked by this class. If no, or more than 1, sets of SAML provider settings
     * are being tracked, this will throw an exception.
     *
     * @return
     * @throws Exception
     */
    public RSSamlProviderSettings getDefaultRSSamlProviderSettings() throws Exception {
        return getDefaultSettings(rsSamlProviderSettings);
    }

    public RSSamlProviderSettings getSamlProviderSettings(String id) {
        return rsSamlProviderSettings.get(id);
    }

    public void removeSamlProviderSettings(String id) {
        rsSamlProviderSettings.remove(id);
    }

    /**
     * Creates a default AuthFilterSettingsobject and puts it into the map for auth filter settings.
     */
    public void setDefaultAuthFilterSettings() {
        String method = "setDefaultAuthFilterSettings";

        AuthFilterSettings filterSettings = new AuthFilterSettings();
        String filterId = filterSettings.getId();

        if (debug) {
            Log.info(thisClass, method, "Setting default auth filter settings with id: " + filterId);
        }

        filterSettings.setDefaultRequestUrlSettings();

        authFilterSettings.put(filterId, filterSettings);
    }

    public Map<String, AuthFilterSettings> getAllAuthFilterSettings() {
        return authFilterSettings;
    }

    public void removeAllAuthFilterSettings() {
        authFilterSettings.clear();
    }

    public void setAuthFilterSettings(String id, AuthFilterSettings authFilterSettings) {
        this.authFilterSettings.put(id, authFilterSettings.copyConfigSettings());
    }

    /**
     * Gets the only auth filter settings tracked by this class. If no, or more than 1, sets of auth filter settings
     * are being tracked, this will throw an exception.
     *
     * @return
     * @throws Exception
     */
    public AuthFilterSettings getDefaultAuthFilterSettings() throws Exception {
        return getDefaultSettings(authFilterSettings);
    }

    public AuthFilterSettings getAuthFilterSettings(String id) {
        return authFilterSettings.get(id);
    }

    public void removeAuthFilterSettings(String id) {
        authFilterSettings.remove(id);
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        Log.info(thisClass, thisMethod, "RS SAML config settings:");
        printRSSamlProviderSettings();
        printAuthFilterSettings();
    }

    public void printRSSamlProviderSettings() {
        if (rsSamlProviderSettings == null) {
            Log.info(thisClass, "printRSSamlProviderSettings", "rsSamlProviderSettings: null");
        } else {
            for (String id : rsSamlProviderSettings.keySet()) {
                RSSamlProviderSettings settings = rsSamlProviderSettings.get(id);
                settings.printConfigSettings();
            }
        }
    }

    public void printAuthFilterSettings() {
        if (authFilterSettings == null) {
            Log.info(thisClass, "printAuthFilterSettings", "authFilterSettings: null");
        } else {
            for (String id : authFilterSettings.keySet()) {
                AuthFilterSettings settings = authFilterSettings.get(id);
                settings.printConfigSettings();
            }
        }
    }

}
