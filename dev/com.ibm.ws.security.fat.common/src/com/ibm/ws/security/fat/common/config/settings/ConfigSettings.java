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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.simplicity.log.Log;

public class ConfigSettings extends BaseConfigSettings {

    private static final Class<?> thisClass = ConfigSettings.class;

    protected FeatureSettings featureSettings = new FeatureSettings();
    protected SSLConfigSettings sslConfigSettings = new SSLConfigSettings();

    protected List<String> registryFiles = new ArrayList<String>();
    protected List<String> applicationFiles = new ArrayList<String>();
    protected String miscFile = "";

    public static final String VAR_INCLUDE_REGISTRIES = "${include.registries}";
    public static final String VAR_INCLUDE_FILE_MISC = "${include.misc}";
    public static final String VAR_INCLUDE_TEST_APPS = "${include.test.applications}";

    public ConfigSettings() {
        featureSettings = new FeatureSettings();
        sslConfigSettings = new SSLConfigSettings();

        registryFiles = new ArrayList<String>();
        applicationFiles = new ArrayList<String>();
        miscFile = "";
    }

    public ConfigSettings(FeatureSettings featureSettings, List<String> registryFiles, SSLConfigSettings sslConfigSettings,
                          List<String> applicationFiles, String miscFile) {
        this.featureSettings = featureSettings.copyConfigSettings();
        this.sslConfigSettings = sslConfigSettings.copyConfigSettings();

        this.registryFiles = registryFiles;
        this.applicationFiles = applicationFiles;
        this.miscFile = miscFile;
    }

    @Override
    public ConfigSettings createShallowCopy() {
        return new ConfigSettings(featureSettings, registryFiles, sslConfigSettings, applicationFiles, miscFile);
    }

    @Override
    public ConfigSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of config settings");
        }
        Map<String, String> configSettings = new HashMap<String, String>();

        configSettings.put(FeatureSettings.VAR_FEATURES, featureSettings.buildConfigOutput());
        configSettings.put(SSLConfigSettings.VAR_SSL_SETTINGS, sslConfigSettings.buildConfigOutput());

        configSettings.put(VAR_INCLUDE_REGISTRIES, buildIncludeElements(getRegistryFiles()));
        configSettings.put(VAR_INCLUDE_TEST_APPS, buildIncludeElements(getApplicationFiles()));
        configSettings.put(VAR_INCLUDE_FILE_MISC, buildIncludeElement(getMiscFile()));

        return configSettings;
    }

    public void setFeatures(String... featureNames) {
        this.featureSettings = new FeatureSettings(featureNames);
    }

    public void setFeatureSettings(FeatureSettings settings) {
        this.featureSettings = settings.copyConfigSettings();
    }

    public FeatureSettings getFeatureSettings() {
        return this.featureSettings;
    }

    /**
     * Sets the feature settings to use the specified feature file. If this file is set, when building the configuration
     * output for these settings, the output will be an include element with its location set to {@code fileLocation}.
     * 
     * @param fileLocation
     */
    public void setFeatureFile(String fileLocation) {
        this.featureSettings.setIncludeFileLocation(fileLocation);
    }

    public void setSSLConfigSettings(SSLConfigSettings settings) {
        this.sslConfigSettings = settings.copyConfigSettings();
    }

    public SSLConfigSettings getSSLConfigSettings() {
        return this.sslConfigSettings;
    }

    /**
     * Sets the feature settings to use the specified feature file. If this file is set, when building the configuration
     * output for these settings, the output will be an include element with its location set to {@code featureFile}.
     * 
     * @param featureFile
     */
    public void setSSLConfigSettingsFile(String fileLocation) {
        this.sslConfigSettings.setIncludeFileLocation(fileLocation);
    }

    public void setRegistryFiles(String... registryFiles) {
        List<String> files = new ArrayList<String>();
        for (String file : registryFiles) {
            files.add(file);
        }
        setRegistryFiles(files);
    }

    private void setRegistryFiles(List<String> registryFiles) {
        this.registryFiles = registryFiles;
    }

    public List<String> getRegistryFiles() {
        return this.registryFiles;
    }

    public void setApplicationFiles(String... applicationFiles) {
        List<String> files = new ArrayList<String>();
        for (String file : applicationFiles) {
            files.add(file);
        }
        setApplicationFiles(files);
    }

    private void setApplicationFiles(List<String> applicationFiles) {
        this.applicationFiles = applicationFiles;
    }

    public List<String> getApplicationFiles() {
        return this.applicationFiles;
    }

    public void setMiscFile(String miscFile) {
        this.miscFile = miscFile;
    }

    public String getMiscFile() {
        return this.miscFile;
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "Config settings:");

        featureSettings.printConfigSettings();
        sslConfigSettings.printConfigSettings();

        for (String file : registryFiles) {
            Log.info(thisClass, thisMethod, indent + "registryFile: " + file);
        }
        for (String file : applicationFiles) {
            Log.info(thisClass, thisMethod, indent + "applicationFile: " + file);
        }
        Log.info(thisClass, thisMethod, indent + "miscFile: " + miscFile);
    }

}
