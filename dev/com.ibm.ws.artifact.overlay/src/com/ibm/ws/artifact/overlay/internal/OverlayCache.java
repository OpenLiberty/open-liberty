/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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

/**
 * Common Class to hold cached data for Overlay Impls.
 */
@SuppressWarnings("rawtypes")
public class OverlayCache {
    final Map<String, Map<Class, Object>> cache = new HashMap<String, Map<Class, Object>>();

    public synchronized void addToCache(String path, Class owner, Object data) {
        Map<Class, Object> classStore = cache.get(path);
        if (classStore == null) {
            classStore = new HashMap<Class, Object>();
            cache.put(path, classStore);
        }
        classStore.put(owner, data);
    }

    public synchronized void removeFromCache(String path, Class owner) {
        Map<Class, Object> classStore = cache.get(path);
        if (classStore != null) {
            classStore.remove(owner);
            if (classStore.isEmpty()) {
                cache.remove(path);
            }
        }
    }

    public synchronized Object getFromCache(String path, Class owner) {
        Map<Class, Object> classStore = cache.get(path);
        if (classStore != null) {
            return classStore.get(owner);
        } else {
            return null;
        }
    }
    
    public synchronized boolean isCacheEmpty(){
        return cache.isEmpty();
    }

    public synchronized Map<String, Map<Class, Object>> getSnapshot(){
        Map<String, Map<Class, Object>> retValue = new HashMap<String, Map<Class, Object>>();

        for( Map.Entry<String, Map<Class, Object>> stringTo: cache.entrySet() ){
            retValue.put(stringTo.getKey(), new HashMap<Class, Object>(stringTo.getValue()));
        }

        return retValue;
    }

}
