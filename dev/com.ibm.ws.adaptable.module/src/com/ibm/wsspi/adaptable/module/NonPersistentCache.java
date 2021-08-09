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
package com.ibm.wsspi.adaptable.module;

/**
 *
 */
public interface NonPersistentCache {

    /**
     * Stores some data associated with the given container/entry within
     * non persistent in memory cache associated with its overlay instance.
     * 
     * @param owner Class of caller setting data, allows multiple adapters to cache against a given container/entry.
     * @param data Data to store for caller.
     */
    public void addToCache(Class<?> owner, Object data);

    /**
     * Removes some data associated with the given container/entry within
     * non persistent in memory cache associated with its overlay instance.
     * 
     * @param owner Class of caller getting data, allows multiple adapters to cache against a given container/entry.
     */
    public void removeFromCache(Class<?> owner);

    /**
     * Obtains some data associated with the given container/entry within
     * non persistent in memory cache associated with its overlay instance.
     * 
     * @param owner Class of caller getting data, allows multiple adapters to cache against a given container/entry.
     * @returns Cached data if any was held, or null if none was known.
     */
    public Object getFromCache(Class<?> owner);

}
