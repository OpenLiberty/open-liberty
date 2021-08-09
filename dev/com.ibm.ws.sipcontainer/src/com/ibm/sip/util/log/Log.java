/*******************************************************************************
 * Copyright (c) 2003,2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.sip.util.log;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.LogManager;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * @author Amir Perlman, Jun 9, 2005
 *
 * Logger providing tWAS like API implemented on top of the Tr. 
 */
public class Log {
	/**
	 * This is the name of the resource bundle for all loggers produced
	 * @see java.util.ResourceBundle for information about resource bundles  
	 */		
	public static final String BUNDLE_NAME = "com.ibm.ws.sipcontainer.resources.Messages";

	/**
	 * Indicates that user of this log utility does not wish to use resource bundle file
	 */
	private static String NO_BUNDLE = "NO_BUNDLE";
	
	/**
	 * Reads configuration for loggers
	 */
    private static final String PROPERTIES_FILE_NAME = "logging.properties";
	static {
		try {
		    InputStream props = ClassLoader.getSystemResourceAsStream( PROPERTIES_FILE_NAME);
		    
		    if( props == null){
		        LogManager.getLogManager().readConfiguration();
		    }
		    else{
		        LogManager.getLogManager().readConfiguration( props);
		    }
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static LogMgr getNoBundle(Class aClass) {
    	return get(aClass, NO_BUNDLE);
    }
	
	/**
     * Creates a new LogMgr for the class 
     * @param aClass The class where the loger was created
     * @return LogMgr object
     */
    public static LogMgr get(Class aClass) {
    	return get(aClass, null);
    }
    /**
     * Creates a new LogMgr for the class 
     * @param aClass The class where the loger was created
     * @param resourceBundle
     * @return LogMgr object
     */
    public static LogMgr get(Class aClass, String resourceBundle) {
        //base class name to use in log messages
    	String baseClassName = "";
    	if( aClass == null){
    	    throw new RuntimeException("aClass cannot be null in LogMgr.get(Class aClass)");
    	}
    	
        baseClassName = aClass.getName();
        int i = baseClassName.lastIndexOf(".");
        if (i != -1) {
            baseClassName = baseClassName.substring(i + 1);
        }
        
        String bundleName =null;
        if(resourceBundle==null){
        	bundleName = BUNDLE_NAME;
        }else{
        	bundleName = resourceBundle;
        }
        
        final Class aClassF = aClass;
    	final String bundleNameF = bundleName;    	
    	final  TraceComponent tc = Tr.register(aClassF, null, bundleNameF);
    	
    	return new LogMgr(tc);
    }
}
