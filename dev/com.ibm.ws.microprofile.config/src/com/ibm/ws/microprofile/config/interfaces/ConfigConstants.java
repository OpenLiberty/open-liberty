/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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

}
