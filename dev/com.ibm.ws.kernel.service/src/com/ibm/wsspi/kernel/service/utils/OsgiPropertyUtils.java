/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wsspi.kernel.service.utils;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Attempt to retrieve properties using the bundle context of the bundle
 * that loaded this utility class. {@link BundleContext.getProperty} does
 * not check any values that are bundle-specific: it first checks
 * the list of properties used to start the framework, and then checks
 * system properties to retrieve the property value.
 * 
 * @see FrameworkUtil#getBundle(Class)
 * @see BundleContext#getProperty(String)
 */
public class OsgiPropertyUtils {

    private static final BundleContext bContext;

    static {
        BundleContext bc = null;
        try {
            // Get the system bundle's bundle context to avoid timing issues with 
            // the start/readiness of this bundle -- we're using it to check for properties
            // which all go to the system bundle anyway
            Bundle b = FrameworkUtil.getBundle(OsgiPropertyUtils.class);
            bc = b.getBundleContext();
            bc = bc.getBundle(Constants.SYSTEM_BUNDLE_LOCATION).getBundleContext();
        } catch (Throwable t) {
        }
        bContext = bc;
    }

    /**
     * Attempts to retrieve the property via the bundle context,
     * which checks framework properties before checking system properties.
     * If the bundle context was not set (or is invalid), this will
     * attempt to find the property in system properties.
     * 
     * The property checks in this method should not be wrapped in doPriv
     * blocks: the caller's permissions (with respect to properties
     * they can retrieve) must be used.
     * 
     * @param propertyName
     * @return String value, or null if not found in either framework or system properties
     */
    @FFDCIgnore(IllegalStateException.class)
    private static String get(String propertyName) {
        String tmpObj = null;
        if (bContext != null) {
            try {
                tmpObj = bContext.getProperty(propertyName);
            } catch (IllegalStateException ise) {
            }
        }

        if (tmpObj == null) {
            tmpObj = System.getProperty(propertyName);
        }
        return tmpObj;
    }

    /**
     * Retrieve the value of the specified property from framework/system properties.
     * 
     * @param propertyName Name of property
     * @param defaultValue Default value to return if property is not set
     * @return Property or default value as a String
     */
    public static String getProperty(String propertyName, String defaultValue) {
        String value = get(propertyName);
        return value == null ? defaultValue : value;
    }

    /**
     * Retrieve the value of the specified property from framework/system properties.
     * Value is converted and returned as an int.
     * 
     * @param propertyName Name of property
     * @param defaultValue Default value to return if property is not set
     * @return Property or default value as an int
     */
    public static int getInteger(String propertyName, int defaultValue) {
        String tmpObj = get(propertyName);

        if (tmpObj != null) {
            try {
                return Integer.parseInt(tmpObj);
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }

    /**
     * Retrieve the value of the specified property from framework/system properties.
     * Value is converted and returned as an long.
     * 
     * @param propertyName Name of property
     * @param defaultValue Default value to return if property is not set
     * @return Property or default value as an long
     */
    public static long getLong(String propertyName, long defaultValue) {
        String tmpObj = get(propertyName);

        if (tmpObj != null) {
            try {
                return Long.parseLong(tmpObj);
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }

    /**
     * Retrieve the value of the specified property from framework/system properties.
     * Value is converted and returned as an boolean.
     * 
     * @param propertyName Name of property
     * @param defaultValue Default value to return if property is not set
     * @return Property or default value as an boolean
     */
    public static boolean getBoolean(String propertyName) {
        String tmpObj = get(propertyName);
        return Boolean.parseBoolean(tmpObj);
    }
}