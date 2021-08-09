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

public class RequestUrlSettings extends BaseConfigSettings {

    private Class<?> thisClass = RequestUrlSettings.class;

    public static final String CONFIG_ELEMENT_NAME = "requestUrl";

    public static final String ATTR_REQUEST_URL_ID = "id";
    public static final String ATTR_REQUEST_URL_PATTERN = "urlPattern";
    public static final String ATTR_REQUEST_URL__MATCH_TYPE = "matchType";

    private String id = "myRequestUrl1";
    private String urlPattern = "/samlclient/fat/sp1/";
    private String matchType = "contains";

    public RequestUrlSettings() {
        configElementName = CONFIG_ELEMENT_NAME;
    }

    public RequestUrlSettings(String id, String urlPattern, String matchType) {
        configElementName = CONFIG_ELEMENT_NAME;
        this.id = id;
        this.urlPattern = urlPattern;
        this.matchType = matchType;
    }

    @Override
    public RequestUrlSettings createShallowCopy() {
        return new RequestUrlSettings(id, urlPattern, matchType);
    }

    @Override
    public RequestUrlSettings copyConfigSettings() {
        return copyConfigSettings(this);
    }

    @Override
    public Map<String, String> getConfigAttributesMap() {
        String method = "getConfigAttributesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of auth filter config attributes");
        }
        Map<String, String> attributes = new HashMap<String, String>();

        attributes.put(ATTR_REQUEST_URL_ID, getId());
        attributes.put(ATTR_REQUEST_URL_PATTERN, getUrlPattern());
        attributes.put(ATTR_REQUEST_URL__MATCH_TYPE, getMatchType());

        return attributes;
    }

    @Override
    public Map<String, String> getConfigSettingsVariablesMap() {
        String method = "getConfigSettingsVariablesMap";
        if (debug) {
            Log.info(thisClass, method, "Getting map of auth filter settings config variables");
        }
        Map<String, String> settings = new HashMap<String, String>();

        //        settings.put(VAR_AUTH_FILTER_ID, getId());
        //        settings.put(VAR_AUTH_URL_PATTERN, getUrlPattern());
        //        settings.put(VAR_AUTH_MATCH_TYPE, getMatchType());

        return settings;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setUrlPattern(String urlPattern) {
        this.urlPattern = urlPattern;
    }

    public String getUrlPattern() {
        return this.urlPattern;
    }

    public void setMatchType(String matchType) {
        this.matchType = matchType;
    }

    public String getMatchType() {
        return this.matchType;
    }

    @Override
    public void printConfigSettings() {
        String thisMethod = "printConfigSettings";

        String indent = "  ";
        Log.info(thisClass, thisMethod, "Request URL config settings: ");
        Log.info(thisClass, thisMethod, indent + ATTR_REQUEST_URL_ID + ": " + id);
        Log.info(thisClass, thisMethod, indent + ATTR_REQUEST_URL_PATTERN + ": " + urlPattern);
        Log.info(thisClass, thisMethod, indent + ATTR_REQUEST_URL__MATCH_TYPE + ": " + matchType);

    }
}
