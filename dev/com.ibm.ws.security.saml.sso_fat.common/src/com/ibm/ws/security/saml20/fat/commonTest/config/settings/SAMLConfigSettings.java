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
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.config.settings.ConfigSettings;
import com.ibm.ws.security.fat.common.config.settings.FeatureSettings;
import com.ibm.ws.security.fat.common.config.settings.SSLConfigSettings;

public class SAMLConfigSettings extends ConfigSettings {

    private static final Class<?> thisClass = SAMLConfigSettings.class;

    protected Map<String, SAMLProviderSettings> samlProviderSettings = new HashMap<String, SAMLProviderSettings>();
    protected Map<String, AuthFilterSettings> authFilterSettings = new HashMap<String, AuthFilterSettings>();

    public static final String VAR_SAML_PROVIDERS = "${saml.providers}";
    public static final String VAR_AUTH_FILTERS = "${auth.filters}";

    public SAMLConfigSettings() {
    }

    public SAMLConfigSettings(FeatureSettings featureSettings, List<String> registryFiles, SSLConfigSettings sslConfigSettings,
            List<String> applicationFiles, String miscFile, Map<String, SAMLProviderSettings> samlProviderSettings,
            Map<String, AuthFilterSettings> authFilterSettings) {

        super(featureSettings, registryFiles, sslConfigSettings, applicationFiles, miscFile);

        if (samlProviderSettings != null) {
            for (String id : samlProviderSettings.keySet()) {
                SAMLProviderSettings settings = samlProviderSettings.get(id);
                this.samlProviderSettings.put(id, settings.copyConfigSettings());
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
    public SAMLConfigSettings createShallowCopy() {
        return new SAMLConfigSettings(featureSettings, registryFiles, sslConfigSettings, applicationFiles, miscFile,
                samlProviderSettings, authFilterSettings);
    }

    @Override
    public SAMLConfigSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of SAML settings config variables");
        }

        // This class extends ConfigSettings instead of BaseConfigSettings, so we need to first obtain the normal
        // config settings tracked by the ConfigSettings class
        Map<String, String> settings = super.getConfigSettingsVariablesMap();

        // SAML providers
        StringBuilder samlProviderOutput = new StringBuilder();
        for (String id : samlProviderSettings.keySet()) {
            SAMLProviderSettings providerSettings = samlProviderSettings.get(id);
            samlProviderOutput.append(providerSettings.buildConfigOutput());
        }
        settings.put(VAR_SAML_PROVIDERS, samlProviderOutput.toString());

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
     * Creates a default SAMLProviderSettings and AuthFilterSettings object and puts each into the respective maps for SAML
     * provider settings and auth filter settings. The id of the AuthFilterSettings object is set to the default value of
     * the authFilterRef property of the SAMLProviderSettings object.
     */
    public void setDefaultSamlProviderAndAuthFilters() {
        String method = "setDefaultSamlProviderAndAuthFilters";

        SAMLProviderSettings spSettings = new SAMLProviderSettings();
        String spId = spSettings.getId();
        String authFilterId = spSettings.getAuthFilterRef();

        if (debug) {
            Log.info(thisClass, method, "Setting default SAML provider settings with id: " + spId + " and auth filter settings with id: " + authFilterId);
        }

        AuthFilterSettings filterSettings = new AuthFilterSettings();
        filterSettings.setId(authFilterId);
        filterSettings.setDefaultRequestUrlSettings();

        this.samlProviderSettings.put(spId, spSettings);
        this.authFilterSettings.put(authFilterId, filterSettings);
    }

    public Map<String, SAMLProviderSettings> getAllSamlProviderSettings() {
        return this.samlProviderSettings;
    }

    public void removeAllSamlProviderSettings() {
        this.samlProviderSettings.clear();
    }

    public Map<String, AuthFilterSettings> getAllAuthFilterSettings() {
        return this.authFilterSettings;
    }

    public void removeAllAuthFilterSettings() {
        this.authFilterSettings.clear();
    }

    /********************************* SAML provider settings *********************************/

    public void setSamlProviderSettings(String id, SAMLProviderSettings samlProviderSettings) {
        this.samlProviderSettings.put(id, samlProviderSettings.copyConfigSettings());
    }

    /**
     * Gets the only SAML provider settings tracked by this class. If no, or more than 1, sets of SAML provider settings
     * are being tracked, this will throw an exception.
     * 
     * @return
     * @throws Exception
     */
    public SAMLProviderSettings getDefaultSamlProviderSettings() throws Exception {
        return getDefaultSettings(this.samlProviderSettings);
    }

    public SAMLProviderSettings getSamlProviderSettings(String id) {
        return this.samlProviderSettings.get(id);
    }

    public void removeSamlProviderSettings(String id) {
        this.samlProviderSettings.remove(id);
    }

    /********************************* Auth filter settings *********************************/

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
        return getDefaultSettings(this.authFilterSettings);
    }

    public AuthFilterSettings getAuthFilterSettings(String id) {
        return this.authFilterSettings.get(id);
    }

    public void removeAuthFilterSettings(String id) {
        this.authFilterSettings.remove(id);
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        super.printConfigSettings();

        Log.info(thisClass, thisMethod, "SAML config settings: ");
        printSamlProviderSettings();
        printAuthFilterSettings();
    }

    public void printSamlProviderSettings() {
        if (samlProviderSettings == null) {
            Log.info(thisClass, "printSamlProviderSettings", "samlProviderSettings: null");
        } else {
            for (String id : samlProviderSettings.keySet()) {
                SAMLProviderSettings settings = samlProviderSettings.get(id);
                if (settings != null) {
                    settings.printConfigSettings();
                }
            }
        }
    }

    public void printAuthFilterSettings() {
        if (authFilterSettings == null) {
            Log.info(thisClass, "printAuthFilterSettings", "authFilterSettings: null");
        } else {
            for (String id : authFilterSettings.keySet()) {
                AuthFilterSettings settings = authFilterSettings.get(id);
                if (settings != null) {
                    settings.printConfigSettings();
                }
            }
        }
    }

}
