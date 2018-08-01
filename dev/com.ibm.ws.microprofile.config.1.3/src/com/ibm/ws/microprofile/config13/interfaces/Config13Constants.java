/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config13.interfaces;

/**
 * Constants for Config 1.3
 */
public class Config13Constants {

    /***** Taken from com.ibm.ws.config.xml.internal.XMLConfigConstants *****/
    /** Property identifying prefix of configuration key */
    public static final String CFG_CONFIG_PREFIX = "config.";

    /** Property identifying the service prefix */
    public static final String CFG_SERVICE_PREFIX = "service.";
    /************************************************************************/

    public static final String[] SYSTEM_PREFIXES = new String[] { CFG_CONFIG_PREFIX, CFG_SERVICE_PREFIX };
    public static final int APP_PROPERTY_ORDINAL = 600;
    public static final int SERVER_XML_VARIABLE_ORDINAL = 500;

}
