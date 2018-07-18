/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.utils;

import java.security.AccessController;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.SecureAction;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * This class can be used to discover information about the runtime environment
 * in which the caller is currently running.
 */

public final class RuntimeInfo {

    public final static String SIB_PROPERTY_SEPARATOR = ".";
    public final static String SIB_PROPERTY_PREFIX = "sib" + SIB_PROPERTY_SEPARATOR;

    private static final TraceComponent tc = SibTr.register(RuntimeInfo.class, UtConstants.MSG_GROUP, UtConstants.MSG_BUNDLE);
    final static SecureAction priv = AccessController.doPrivileged(SecureAction.get());

    private static boolean _isClustered;
    private static boolean _isServer;
    private static boolean _isClientContainer;
    private static boolean _isFatClient;
    private static boolean _isThinClient;

    static {
        boolean thinClientPropertySet = false;
        try {
            _isClustered = false;
            _isServer = true;
        } catch (Exception e) { // Ignore any exception because we could be in a client
            // No FFDC code needed
            SibTr.exception(tc, e);
        } catch (NoClassDefFoundError e) { // We may get this in thin clients!
            // No FFDC code needed
            if (!thinClientPropertySet) { // 400794: Don't write this exception out if we're in the thin client
                SibTr.exception(tc, e);
            }
        }

        if (!_isClustered && !_isServer) {
            if ("client".equals(priv.getProperty("com.ibm.ws.container"))) {
                _isClientContainer = true;
            } else if (thinClientPropertySet) {
                _isThinClient = true;
            } else {
                _isFatClient = true;
            }
        }

    }

    /**
     * Is the current process a clustered server
     * 
     * @return boolean
     */

    public static boolean isClusteredServer() {
        return _isClustered;
    }

    /**
     * Is the current process a non-clustered server
     * 
     * @return boolean
     */

    public static boolean isServer() {
        return _isServer;
    }

    /**
     * Is the current process a client container
     * 
     * @return boolean
     */

    public static boolean isClientContainer() {
        return _isClientContainer;
    }

    /**
     * This method returns true if the current process is a client that is running outside
     * of the client container but with the full WAS libraries (either the client libraries
     * or the server libraries).
     * 
     * @return boolean
     */

    public static boolean isFatClient() {
        return _isFatClient;
    }

    /**
     * This method returns true if the current process is a client that is running with the
     * 'Portly' client libraries (i.e. the cutdown JMS client libraries only).
     * 
     * @return boolean
     */

    public static boolean isThinClient() {
        return _isThinClient;
    }

    /*
     * The following section is code that allows callers to retrieve information
     * from the sib.properties file
     */

    /**
     * Retrieves the named property from the sib.properties file or if not found a
     * System property.
     * 
     * @param property The non-null name of the property.
     * 
     * @return The string value of found property otherwise null.
     */

    public static String getProperty(String property) {
        // We have to get the property from Configuration (server.xml) in Liberty, we need to implement this
        return null;
    }

    /**
     * Retrieves the named property by searching sib.config.properties then if not found
     * sib.properties or if not found a System property. If no property value is found
     * then the default value is returned. 328282
     * 
     * @param property The non-null name of the property.
     * @param defval The possibly null default value to return if property does not exist.
     * 
     * @return The string value of found property otherwise defval.
     */

    public static String getProperty(String property, String defval) {
        // We have to get the property from Configuration (server.xml) in Liberty, we need to implement this  
        return defval;
    }

    /**
     * Retrieve the named property using getProperty(String property, String defval) and if the
     * returned value is different to the default value call Runtime.changedPropertyValue.
     * 
     * @param property The non-null name of the property.
     * @param defval The possibly null default value to return if property does not exist.
     * 
     * @return The string value of found property otherwise defval.
     */

    public static String getPropertyWithMsg(String property, String defval) {
        return defval;
    }

    /**
     * Returns true if we are running on 64bit java
     * 
     * @return boolean
     */
    public static boolean is64bit() {
        String bitsize = priv.getProperty("sun.arch.data.model", "32");
        boolean is64bit = bitsize.equals("64");
        return is64bit;
    }

    //Venu mock mock
    //For now return false. However further investigation has to be done to set the correct value
    public static boolean isCRAJvm() {
        return true;
        //return  true;
    }
}
