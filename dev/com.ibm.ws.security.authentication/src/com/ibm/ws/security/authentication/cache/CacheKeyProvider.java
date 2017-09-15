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
package com.ibm.ws.security.authentication.cache;

/**
 * Implement this interface in order to provide cache keys to the authentication cache.
 */
public interface CacheKeyProvider {

    /**
     * Provides the cache key to be used by the authentication cache.
     * Optionally, return a Set of objects for returning more than one key. In such case,
     * the authentication cache will use each element in the set as an individual key.
     * 
     * @param cacheContext The cache context provided by the authentication cache.
     * @return the cache key(s).
     */
    public Object provideKey(CacheContext cacheContext);

}
