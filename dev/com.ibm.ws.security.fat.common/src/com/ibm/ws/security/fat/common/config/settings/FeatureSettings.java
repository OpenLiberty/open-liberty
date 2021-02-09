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
import com.ibm.ws.security.fat.common.CommonIOTools;

public class FeatureSettings extends BaseConfigSettings {

    private static final Class<?> thisClass = FeatureSettings.class;

    protected List<String> featureNames = new ArrayList<String>();

    public static final String CONFIG_ELEMENT_NAME = "feature";
    public static final String VAR_FEATURES = "${features}";

    public FeatureSettings() {
        configElementName = CONFIG_ELEMENT_NAME;
    }

    public FeatureSettings(String... features) {
        configElementName = CONFIG_ELEMENT_NAME;
        for (String feature : features) {
            this.featureNames.add(feature);
        }
    }

    @Override
    public FeatureSettings createShallowCopy() {
        return new FeatureSettings(featureNames.toArray(new String[featureNames.size()]));
    }

    @Override
    public FeatureSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of feature config settings");
        }
        Map<String, String> configSettings = new HashMap<String, String>();

        configSettings.put(VAR_FEATURES, buildConfigOutput());

        return configSettings;
    }

    public void setFeatureNames(String... features) {
        List<String> featureList = new ArrayList<String>();
        for (String feature : features) {
            featureList.add(feature);
        }
        setFeatureNames(featureList);
    }

    private void setFeatureNames(List<String> features) {
        this.featureNames = new ArrayList<String>(features);
    }

    public List<String> getFeatureNames() {
        return this.featureNames;
    }

    @Override
    public String buildConfigOutput() {
        String method = "buildConfigOutput";
        if (debug) {
            Log.info(thisClass, method, "Building config output for features");
        }

        StringBuilder output = new StringBuilder();

        if (isIncludeFile()) {
            output.append(buildIncludeElement(includeFileLocation));
        } else {
            output.append("<featureManager>" + CommonIOTools.NEW_LINE);

            String indent = "    ";
            for (String feature : featureNames) {
                output.append(indent + "<" + configElementName + ">" + feature + "</" + configElementName + ">" + CommonIOTools.NEW_LINE);
            }

            output.append("</featureManager>" + CommonIOTools.NEW_LINE);
        }
        return output.toString();
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "Feature config settings:");
        for (String feature : featureNames) {
            Log.info(thisClass, thisMethod, indent + "feature: " + feature);
        }
    }

}
