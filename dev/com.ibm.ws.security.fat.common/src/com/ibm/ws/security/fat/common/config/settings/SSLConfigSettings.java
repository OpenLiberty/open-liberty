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
package com.ibm.ws.security.fat.common.config.settings;

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonIOTools;

/**
 * Class that tracks sslDefault, ssl, and keystore elements for server configuration files.
 */
public class SSLConfigSettings extends BaseConfigSettings {

    private static final Class<?> thisClass = SSLConfigSettings.class;

    protected Map<String, SSLSettings> sslSettings = new HashMap<String, SSLSettings>();
    protected Map<String, KeystoreSettings> keystoreSettings = new HashMap<String, KeystoreSettings>();

    public static final String VAR_SSL_SETTINGS = "${ssl.settings}";

    public static final String SSL_DEFAULT_ELEMENT_NAME = "sslDefault";

    private String sslDefaultRef = null;

    public SSLConfigSettings() {}

    public SSLConfigSettings(String sslDefaultRef, Map<String, SSLSettings> sslSettings, Map<String, KeystoreSettings> keystoreSettings) {
        this.sslDefaultRef = sslDefaultRef;

        if (sslSettings != null) {
            for (String id : sslSettings.keySet()) {
                SSLSettings settings = sslSettings.get(id);
                this.sslSettings.put(id, settings.copyConfigSettings());
            }
        }
        if (keystoreSettings != null) {
            for (String id : keystoreSettings.keySet()) {
                KeystoreSettings settings = keystoreSettings.get(id);
                this.keystoreSettings.put(id, settings.copyConfigSettings());
            }
        }
    }

    @Override
    public SSLConfigSettings createShallowCopy() {
        return new SSLConfigSettings(sslDefaultRef, sslSettings, keystoreSettings);
    }

    @Override
    public SSLConfigSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of SSL config settings");
        }
        StringBuilder sslOutput = new StringBuilder();

        // Set sslDefault element if the default reference is not null
        if (sslDefaultRef != null) {
            sslOutput.append("<" + SSL_DEFAULT_ELEMENT_NAME + " sslRef=\"" + sslDefaultRef + "\" />" + CommonIOTools.NEW_LINE);
        }

        // SSL elements
        for (String id : sslSettings.keySet()) {
            SSLSettings ssl = sslSettings.get(id);
            sslOutput.append(ssl.buildConfigOutput());
        }

        // Keystores
        for (String id : keystoreSettings.keySet()) {
            KeystoreSettings keystore = keystoreSettings.get(id);
            sslOutput.append(keystore.buildConfigOutput());
        }

        Map<String, String> settings = new HashMap<String, String>();
        settings.put(VAR_SSL_SETTINGS, sslOutput.toString());

        return settings;
    }

    public Map<String, SSLSettings> getAllSSLSettings() {
        return this.sslSettings;
    }

    public void removeAllSSLSettings() {
        this.sslSettings.clear();
    }

    public Map<String, KeystoreSettings> getAllKeystoreSettings() {
        return this.keystoreSettings;
    }

    public void removeAllKeystoreSettings() {
        this.keystoreSettings.clear();
    }

    /********************************* SSL settings *********************************/

    public void setSSLSettings(String id, SSLSettings sslSettings) {
        this.sslSettings.put(id, sslSettings.copyConfigSettings());
    }

    /**
     * Gets the only SSL settings tracked by this class. If no, or more than 1, sets of settings are being tracked, this
     * will throw an exception.
     * 
     * @return
     * @throws Exception
     */
    public SSLSettings getDefaultSSLSettings() throws Exception {
        return getDefaultSettings(this.sslSettings);
    }

    public SSLSettings getSSLSettings(String id) {
        return this.sslSettings.get(id);
    }

    public void removeSSLSettings(String id) {
        this.sslSettings.remove(id);
    }

    /********************************* Keystore settings *********************************/

    public void setKeystoreSettings(String id, KeystoreSettings keystoreSettings) {
        this.keystoreSettings.put(id, keystoreSettings.copyConfigSettings());
    }

    /**
     * Gets the only keystore settings tracked by this class. If no, or more than 1, sets settings are being tracked, this
     * will throw an exception.
     * 
     * @return
     * @throws Exception
     */
    public KeystoreSettings getDefaultKeystoreSettings() throws Exception {
        return getDefaultSettings(this.keystoreSettings);
    }

    public KeystoreSettings getKeystoreSettings(String id) {
        return this.keystoreSettings.get(id);
    }

    public void removeKeystoreSettings(String id) {
        this.keystoreSettings.remove(id);
    }

    /********************************* Private member getters and setters *********************************/

    public void setSslDefaultRef(String sslDefaultRef) {
        this.sslDefaultRef = sslDefaultRef;
    }

    public String getSslDefaultRef() {
        return this.sslDefaultRef;
    }

    @Override
    public String buildConfigOutput() {
        String method = "buildConfigOutput";
        if (debug) {
            Log.info(thisClass, method, "Building config output for SSL elements");
        }

        StringBuilder output = new StringBuilder();

        if (isIncludeFile()) {
            output.append(buildIncludeElement(includeFileLocation));
        } else {
            // There are several distinct elements tracked by this class, whose proper output is created by the getConfigSettingsVariablesMap() method
            Map<String, String> configSettings = getConfigSettingsVariablesMap();
            String settingsString = configSettings.get(VAR_SSL_SETTINGS);
            output.append(settingsString);
        }
        return output.toString();
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "SSL config settings:");
        Log.info(thisClass, thisMethod, indent + SSL_DEFAULT_ELEMENT_NAME + ": " + sslDefaultRef);
        printSSLSettings();
        printKeystoreSettings();
    }

    public void printSSLSettings() {
        if (sslSettings == null) {
            Log.info(thisClass, "printSSLSettings", "sslSettings: null");
        } else {
            for (String id : sslSettings.keySet()) {
                SSLSettings settings = sslSettings.get(id);
                if (settings != null) {
                    settings.printConfigSettings();
                }
            }
        }
    }

    public void printKeystoreSettings() {
        if (keystoreSettings == null) {
            Log.info(thisClass, "printKeystoreSettings", "keystoreSettings: null");
        } else {
            for (String id : keystoreSettings.keySet()) {
                KeystoreSettings settings = keystoreSettings.get(id);
                if (settings != null) {
                    settings.printConfigSettings();
                }
            }
        }
    }

}
