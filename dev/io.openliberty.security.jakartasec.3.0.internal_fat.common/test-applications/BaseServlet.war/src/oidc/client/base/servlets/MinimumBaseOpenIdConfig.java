/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package oidc.client.base.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.openliberty.security.jakartasec.fat.utils.Constants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Named;
import jakarta.security.enterprise.authentication.mechanism.http.openid.DisplayType;
import jakarta.security.enterprise.authentication.mechanism.http.openid.PromptType;

/**
 * Load the config values from the default config property file.
 * If no value is found for an expected config attribute, load it, if a value is NOT found set this tests suites default value.
 * Test applications can override methods to specify their own value.
 * A value of "UnsetValue" will cause this tooling to return null (this allows the test specified config files to omit specific
 * config values)
 */

@Named
@Dependent
public class MinimumBaseOpenIdConfig {

    protected Properties config;

    @PostConstruct
    public void init() {

        System.out.println("in MinimumBaseOpenIdConfig init");

        config = new Properties();

        InputStream configFile = MinimumBaseOpenIdConfig.class.getResourceAsStream(Constants.OPEN_ID_CONFIG_PROPERTIES);
        if (configFile != null) {
            try {
                System.out.println("Loading config from: " + Constants.OPEN_ID_CONFIG_PROPERTIES);
                config.load(configFile);
            } catch (IOException e) {
                throw new IllegalStateException("Could not load OpenIdConfig");
            }
        }
    }

    // TODO turn off the debug messages to reduce clutter in the logs
    public String getStringValue(String key) {
        String value = config.getProperty(key);
        if (value.equals(Constants.EMPTY_VALUE)) {
            System.out.println("Setting empty string as the value for config attribute: " + key);
            return "";
        } else {
            if (value.equals(Constants.NULL_VALUE)) {
                System.out.println("Unsetting the value for config attribute: " + key);
                return null;
            } else {
                System.out.println("Setting the value for config attribute: " + key + " to: " + value);
                return value;
            }
        }
    }

    public String[] getStringArrayValue(String key, String delimiter) {
        String value = config.getProperty(key);
        String[] returnValue = {};
        if (value != null && !value.isEmpty() && !value.contains(Constants.EMPTY_VALUE)) {
            returnValue = value.split(delimiter);
        }
        return returnValue;
    }

    public boolean getBooleanValue(String key) {
        String value = config.getProperty(key);
        try {
            if (value == null) {
                throw new Exception("Don't know what to do with a null boolean value");
            }
            if (!(value.equals(String.valueOf(false)) || value.equals(String.valueOf(true)))) {
                throw new Exception("Don't know what to do with a " + value + " boolean value");
            } else {
                System.out.println("Setting the boolean value for config attribute: " + key + " to: " + value);
                return Boolean.parseBoolean(value);
            }
        } catch (Exception e) {
            System.out.println("getBooleanValue couldn't handle the value specified in the config properties - setting a default to false.");
            System.out.println(e.getMessage());
            return false;
        }
    }

    public PromptType[] getPromptTypeValue(String key) {
        String value = config.getProperty(key);
        System.out.println("Raw prompt value: " + value);
        PromptType[] returnValue = {};
        try {
            if (value != null && !value.isEmpty() && !value.contains(Constants.EMPTY_VALUE)) {
                String[] splitString = value.split(",");
                int size = splitString.length;
                returnValue = new PromptType[size];
                int i = 0;
                for (String v : splitString) {
                    System.out.println("Raw individual value: " + v);
                    returnValue[i] = PromptType.valueOf(v);
                    i++;
                }

            }
            System.out.println("Returning prompt value: " + returnValue.toString());
            return returnValue;

        } catch (

        Exception e) {
            System.out.println("getPromptTypeValue couldn't handle the value specified in the config properties - setting a default to {}.");
            System.out.println(e.getMessage());
            return returnValue;
        }

    }

    public DisplayType getDisplayTypeValue(String key) {
        String value = config.getProperty(key);
        try {
            if (value == null || value.isEmpty()) {
                throw new Exception("Don't know what to do with a null or empty DisplayType value");
            }
            return DisplayType.valueOf(value);
        } catch (Exception e) {
            System.out.println("getDisplayTypeValue couldn't handle the value specified in the config properties - setting a default to page.");
            System.out.println(e.getMessage());
            return DisplayType.PAGE;
        }
    }

    public int getIntValue(String key, int defaultValue) {
        String value = config.getProperty(key);
        if (value.equals(Constants.EMPTY_VALUE)) {
            System.out.println("Can't return an empty value for config attribute: " + key + ", but will return the default value.");
            return defaultValue;
        } else {
            if (value.equals(Constants.NULL_VALUE)) {
                System.out.println("Can't unset the value for config attribute: " + key + ", but will return the default value.");
                return defaultValue;
            } else {
                System.out.println("Setting the value for config attribute: " + key + " to: " + value);
                return Integer.valueOf(value);
            }
        }
    }

    public String getProviderBase() {

        String value = "Must be set in openIdConfig.properties before it can be used";;
        if (config.containsKey(Constants.PROVIDER_BASE)) {
            value = getStringValue(Constants.PROVIDER_BASE);
        }

        // not a standard value, so this value will only matter if the config is trying to use the value
        return value;
    }

}
