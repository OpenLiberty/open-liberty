/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.webapp.config;

import java.util.HashMap;

import com.ibm.wsspi.adaptable.module.NonPersistentCache;

/**
 *
 */
public class TestingMemoryCache implements NonPersistentCache {

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.adaptable.module.NonPersistentCache#addToCache(java.lang.Class, java.lang.Object)
     */

    HashMap<Class<?>, Object> dataStore = new HashMap<Class<?>, Object>();

    @Override
    public void addToCache(Class<?> owner, Object data) {
        dataStore.put(owner, data);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.adaptable.module.NonPersistentCache#removeFromCache(java.lang.Class)
     */
    @Override
    public void removeFromCache(Class<?> owner) {
        dataStore.remove(owner);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wsspi.adaptable.module.NonPersistentCache#getFromCache(java.lang.Class)
     */
    @Override
    public Object getFromCache(Class<?> owner) {
        return dataStore.get(owner);
    }

    public void deleteAll() {
        dataStore.clear();
    }

}
