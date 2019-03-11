/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.interfaces;

/**
 * The class for all your favourite Config related constants.
 */
public class ConfigConstants {

    //The META-INF folder inside a jar or war
    public static final String META_INF = "META-INF/";
    //The default config properties file name
    public static final String CONFIG_PROPERTIES = META_INF + "microprofile-config.properties";
    //The special property name which can control the ordinal of a ConfigSource
    public static final String ORDINAL_PROPERTY = "config_ordinal";
    //The ordinal of the default system properties source
    public static final int ORDINAL_SYSTEM_PROPERTIES = 400;
    //The ordinal of the default environment variables source
    public static final int ORDINAL_ENVIRONMENT_VARIABLES = 300;
    //The ordinal of the default properties file source
    public static final int ORDINAL_PROPERTIES_FILE = 100;

    //Default Priority for USER Converters
    public static final int DEFAULT_CONVERTER_PRIORITY = 100;

    //Priority for BUILT IN Converters
    public static final int BUILTIN_CONVERTER_PRIORITY = 1;

    //The special system property which can be used to override the default refresh rate for dynamic sources
    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";
    //The default refresh rate for dynamic sources
    public static final long DEFAULT_DYNAMIC_REFRESH_INTERVAL = 500;
    //The minimum refresh rate for dynamic sources ... if the refresh rate is set lower than this then it will be overridden by this value
    public static final long MINIMUM_DYNAMIC_REFRESH_INTERVAL = 500;
    //In MP Config 1.3 this constant provides the regex to specify allowable characters in an Environment Variable Config Source
    public static final String CONFIG13_ALLOWABLE_CHARS_IN_ENV_VAR_SOURCE = "[^A-Za-z0-9_]";

    //We try to keep track of which application is using which Config. There are cases where the Config is used by a global component
    //or we just can't work out which app it is. Then we fall back to this global name.
    public static final String GLOBAL_CONFIG_APPLICATION_NAME = "!GLOBAL_CONFIG_APPLICATION_NAME!";
}
