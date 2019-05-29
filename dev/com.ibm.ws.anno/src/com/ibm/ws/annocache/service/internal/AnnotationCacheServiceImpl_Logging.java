/*******************************************************************************
☺ * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.annocache.service.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.annocache.service.AnnotationCacheService_Logging;

public class AnnotationCacheServiceImpl_Logging implements AnnotationCacheService_Logging {
    // Loggers ...

    /** <p>The usual annotations logger.</p> */
    public static final Logger ANNO_LOGGER = Logger.getLogger(AnnotationCacheService_Logging.ANNO_LOGGER_NAME);

    /** <p>Functional logger: Log annotations state at the completion of scans.</p> */
    public static final Logger ANNO_STATE_LOGGER = Logger.getLogger(AnnotationCacheService_Logging.ANNO_LOGGER_STATE_NAME);

    /** <p>Functional logger: Log annotation queries (when caching is enabled). */
    public static final Logger ANNO_QUERY_LOGGER = Logger.getLogger(AnnotationCacheService_Logging.ANNO_LOGGER_QUERY_NAME);

    /** <p>Functional logger: Log JANDEX activity. */
    public static final Logger ANNO_JANDEX_LOGGER = Logger.getLogger(AnnotationCacheService_Logging.ANNO_LOGGER_JANDEX_NAME);

    /**
     * <p>Answer a base hash code for a target object.</p>
     *
     * @param object A target object for which to obtain a base hash code.
     * 
     * @return A hash code for the object.
     */
    public static String getBaseHash(Object object) {
        return object.getClass().getSimpleName() + "@" + Integer.toString(object.hashCode());
    }

    // Utilities for avoiding warnings ... for cases where
    // return values are not used, and must be consumed through a function
    // call to avoid a compiler unused value warning.

    /**
     * <p>Utility to explicitly consume a boolean value.</p>
     *
     * <p>This method is intended to be used to indicate a value
     * which is being disregarded. The method call on the value
     * causes a compiler warning to be ignored.</p>
     *
     * <p>All such cases should be examined to determined if the
     * value which is being ignored is correct to ignore.</p>
     *
     * @param booleanValue A boolean value which is to be consumed.
     */
    public static void consume(boolean booleanValue) {
        // NO-OP
    }

    /**
     * <p>Utility to explicitly consume an object reference.</p>
     *
     * <p>This method is intended to be used to indicate a value
     * which is being disregarded. The method call on the value
     * causes a compiler warning to be ignored.</p>
     *
     * <p>All such cases should be examined to determined if the
     * value which is being ignored is correct to ignore.</p>
     *
     * @param objectRef An object reference which is to be consumed.
     */
    public static void consumeRef(Object objectRef) {
        // NO-OP
    }

    //

    /**
     * Tell if a property is set.
     * 
     * @param propertyName The name of the property which is to be tested.
     *  
     * @return True or false telling if a valus is provided for the property.
     */
    public static boolean hasProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<Boolean>() {
                @Override
                public Boolean run() {
                    return Boolean.valueOf( System.getProperty(propertyName) != null );
                }
            } ).booleanValue();
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Answer the text value of a property.  Obtain the property value
     * using a privileged operation.
     * 
     * @param propertyName The name of the property which is to be retrieved.
     *  
     * @return The value of the property.
     */
    public static String getProperty(final String propertyName) {
        Object token = ThreadIdentityManager.runAsServer();
        try {
            return AccessController.doPrivileged( new PrivilegedAction<String>() {
                @Override
                public String run() {
                    return System.getProperty(propertyName);
                }
            } );
        } finally {
            ThreadIdentityManager.reset(token);
        }
    }

    /**
     * Obtain a string property.
     * 
     * If trace is enabled for the specified logger, log the property value.
     * 
     * @param logger The logger to use to display the property value.
     * @param sourceClass The class requesting the property value.
     * @param sourceMethod The method requesting the property value.
     * @param propertyName The name of the property.
     * @param propertyDefaultValue The default value of the property.
     * 
     * @return The value of the property.
     */
    public static String getProperty(
        Logger logger,
        String sourceClass, String sourceMethod,
        String propertyName, String propertyDefaultValue) {

        String propertyText = getProperty(propertyName);
        
        String propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = propertyText;
        }

        if ( logger.isLoggable(Level.FINER) ) {
            String debugText =
                "Property [ " + propertyName + " ]" +
                " [ " + propertyValue + " ]" +
                " (" + (propertyDefaulted ? "from default" : "from property") + ")";
            logger.logp(Level.FINER, sourceClass, sourceMethod, debugText);
        }

        return propertyValue;
    }

    /**
     * Obtain a boolean property.
     * 
     * If trace is enabled for the specified logger, log the property value.
     * 
     * @param logger The logger to use to display the property value.
     * @param sourceClass The class requesting the property value.
     * @param sourceMethod The method requesting the property value.
     * @param propertyName The name of the property.
     * @param propertyDefaultValue The default value of the property.
     * 
     * @return The value of the property.
     */
    public static boolean getProperty(
        Logger logger,
        String sourceClass, String sourceMethod,
        String propertyName, boolean propertyDefaultValue) {

        String propertyText = getProperty(propertyName);
        
        boolean propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Boolean.parseBoolean(propertyText);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            String debugText =
                "Property [ " + propertyName + " ]" +
                " [ " + propertyValue + " ]" +
                " (" + (propertyDefaulted ? "from default" : "from property") + ")";
            logger.logp(Level.FINER, sourceClass, sourceMethod, debugText);
        }

        return propertyValue;
    }
    
    /**
     * Obtain a integer property.
     * 
     * If trace is enabled for the specified logger, log the property value.
     * 
     * @param logger The logger to use to display the property value.
     * @param sourceClass The class requesting the property value.
     * @param sourceMethod The method requesting the property value.
     * @param propertyName The name of the property.
     * @param propertyDefaultValue The default value of the property.
     * 
     * @return The value of the property.
     */
    public static int getProperty(
        Logger logger,
        String sourceClass, String sourceMethod,
        String propertyName, int propertyDefaultValue) {

        String propertyText = getProperty(propertyName);
        
        int propertyValue;
        boolean propertyDefaulted;
        if ( propertyDefaulted = (propertyText == null) ) {
            propertyValue = propertyDefaultValue;
        } else {
            propertyValue = Integer.parseInt(propertyText);
        }

        if ( logger.isLoggable(Level.FINER) ) {
            String debugText =
                "Property [ " + propertyName + " ]" +
                " [ " + propertyValue + " ]" +
                " (" + (propertyDefaulted ? "from default" : "from property") + ")";
            logger.logp(Level.FINER, sourceClass, sourceMethod, debugText);
        }

        return propertyValue;
    }
}
