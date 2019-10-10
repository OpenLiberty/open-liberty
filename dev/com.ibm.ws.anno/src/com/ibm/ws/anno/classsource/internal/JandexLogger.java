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
package com.ibm.ws.anno.classsource.internal;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;

public class JandexLogger {
    private static final Logger logger = Logger.getLogger("com.ibm.ws.jandex");
    
    @Trivial
    protected static boolean doLog() {
    	return ( logger.isLoggable(Level.FINER) );
    }

    @Trivial
    protected static void log(String sourceClass, String sourceMethod, String message) {
    	logger.logp(Level.FINER, sourceClass, sourceMethod, message);
    }

    @Trivial
    protected static void log(String sourceClass, String sourceMethod, String message, Object...parms) {
    	logger.logp(Level.FINER, sourceClass, sourceMethod, message, parms);
    }

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

    public static boolean getProperty(
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

        if ( doLog() ) {
            String debugText =
                "Property [ " + propertyName + " ]" +
                " [ " + propertyValue + " ]" +
                " (" + (propertyDefaulted ? "from default" : "from property") + ")";
            log(sourceClass, sourceMethod, debugText);
        }

        return propertyValue;
    }    
}
