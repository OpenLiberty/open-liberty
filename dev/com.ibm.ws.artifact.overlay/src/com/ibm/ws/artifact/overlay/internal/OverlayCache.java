/*******************************************************************************
 * Copyright (c) 2011, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.artifact.overlay.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Implementation of storage for an overlay cache.
 */
public class OverlayCache {
	private static final String CLASS_NAME = OverlayCache.class.getSimpleName();

	private static final String ABSENT = "Absent";
	private static final String PRESENT = "Present";
	private static final String PRESENT_LAST = "Present-Last";

    private Map<String, Map<Class<?>, Object>> pathTypeStorage =
    	new HashMap<String, Map<Class<?>, Object>>();

    public synchronized void addToCache(String pathKey, Class<?> typeKey, Object data) {
    	String methodName = "addToCache";

    	String pathCase = ABSENT;
    	Object oldData = null;
    	String typeCase = ABSENT;

        Map<Class<?>, Object> typeStorage = pathTypeStorage.get(pathKey);
        if ( typeStorage == null ) {
            typeStorage = new HashMap<Class<?>, Object>();
            pathTypeStorage.put(pathKey, typeStorage);

            oldData = typeStorage.put(typeKey, data);

        } else {
        	pathCase = PRESENT;

        	oldData = typeStorage.put(typeKey, data);

        	if ( oldData != null ) {
        		typeCase = PRESENT;
        	}
        }
        
        // Use the cache logger to avoid interactions with injected trace.
        // Placing the logger in OverlayCache means it will be used
        // both for custom logging and for injected trace.

        if ( OverlayCacheLogger.logger.isLoggable(Level.FINER) ) {
        	OverlayCacheLogger.logger.logp(Level.FINER, CLASS_NAME, methodName,
        		"Path ({0}) [ {1} ] Type ({2}) [ {3} ] Data(Old) [ {4} ] Data(New) [ {5} ]",
        		new Object[] { pathCase, pathKey, typeCase, typeKey, oldData, data });
        }
    }

    public synchronized void removeFromCache(String pathKey, Class<?> typeKey) {
    	String methodName = "removeFromCache";

    	String pathCase = ABSENT;
    	Object data = null;
    	String typeCase = ABSENT;

        Map<Class<?>, Object> typeStorage = pathTypeStorage.get(pathKey);
        if ( typeStorage != null ) {
            pathCase = PRESENT;

            data = typeStorage.remove(typeKey);

            if ( data != null ) {
                if ( typeStorage.isEmpty() ) {
                    pathTypeStorage.remove(pathKey);
                    typeCase = PRESENT_LAST;
                } else {
                	typeCase = PRESENT;
                }
            }            
        }

        // Use the cache logger to avoid interactions with injected trace.
        // Placing the logger in OverlayCache means it will be used
        // both for custom logging and for injected trace.
        
        if ( OverlayCacheLogger.logger.isLoggable(Level.FINER) ) {
        	OverlayCacheLogger.logger.logp(Level.FINER, CLASS_NAME, methodName,
        		"Path ({0}) [ {1} ] Type ({2}) [ {3} ] Data [ {4} ]",
        		new Object[] { pathCase, pathKey, typeCase, typeKey, data });
        }
    }

    public synchronized Object getFromCache(String pathKey, Class<?> typeKey) {
    	String methodName = "getFromCache";

    	String pathCase = ABSENT;
    	Object data = null;
    	String typeCase = ABSENT;

        Map<Class<?>, Object> typeStorage = pathTypeStorage.get(pathKey);
        if ( typeStorage != null ) {
        	pathCase = PRESENT;

            data = typeStorage.get(typeKey);

            if ( data != null ) {
            	typeCase = PRESENT;
            }
        }

        // Use the cache logger to avoid interactions with injected trace.
        // Placing the logger in OverlayCache means it will be used
        // both for custom logging and for injected trace.
        
        if ( OverlayCacheLogger.logger.isLoggable(Level.FINER) ) {
        	OverlayCacheLogger.logger.logp(Level.FINER, CLASS_NAME, methodName,
        		"Path ({0}) [ {1} ] Type ({2}) [ {3} ] Data [ {4} ]",
        		new Object[] { pathCase, pathKey, typeCase, typeKey, data });
        }

        return data;
    }

}
