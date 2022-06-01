/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jsp;

import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PagesVersionHandler {

    static protected Logger logger;

    private static final String CLASS_NAME = "com.ibm.ws.jsp.PagesVersionHandler";
    static {
        logger = Logger.getLogger("com.ibm.ws.jsp");
    }

    public static String loadedSpecLevel = loadPagesVersion();

    private static String DEFAULT_VERSION = "2.2";
    
    private static synchronized String loadPagesVersion(){
        String methodName = "loadServletVersion";

        try (InputStream input = PagesVersionHandler.class.getClassLoader().getResourceAsStream("com/ibm/ws/jsp/speclevel/jspSpecLevel.properties")) {

            // null check fixes errors in wc unit tests
            if(input != null){
                Properties prop = new Properties();
                prop.load(input);
                return prop.getProperty("version");
            } else {
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                    logger.logp(Level.FINE, CLASS_NAME, methodName, "InputStream was null for jspSpecLevel.properties" );
                }
            }

        } catch (Exception ex) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && logger.isLoggable(Level.FINE)) {
                logger.logp(Level.FINE, CLASS_NAME, methodName, "Exception occured: " + ex.getCause() );
            }
        }

        logger.logp(Level.WARNING, CLASS_NAME, "getLoadedPagesSpecLevel", "jsp.feature.not.loaded.correctly");

        return DEFAULT_VERSION;
    }

    public static boolean isPages30Loaded(){
        if( PagesVersionHandler.loadedSpecLevel.equals("3.0")) {
            return true;
        }
        return false;
    }
}
