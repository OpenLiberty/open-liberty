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

    public static final String META_INF = "META-INF/";
    public static final String CONFIG_PROPERTIES = META_INF + "microprofile-config.properties";

    public static final String ORDINAL_PROPERTY = "config_ordinal";

    public static final int ORDINAL_SYSTEM_PROPERTIES = 400;
    public static final int ORDINAL_ENVIRONMENT_VARIABLES = 300;
    public static final int ORDINAL_PROPERTIES_FILE = 100;

    //Default Priority for USER Converters
    public static final int DEFAULT_CONVERTER_PRIORITY = 100;

    //Priority for BUILT IN Converters
    public static final int BUILTIN_CONVERTER_PRIORITY = 1;

    public static final String DYNAMIC_REFRESH_INTERVAL_PROP_NAME = "microprofile.config.refresh.rate";
    public static final long DEFAULT_DYNAMIC_REFRESH_INTERVAL = 500;
    public static final long MINIMUM_DYNAMIC_REFRESH_INTERVAL = 500;

}
