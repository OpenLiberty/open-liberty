/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

/**
 * Interface methods to create, add, and get logged out tokens from the LoggedOutTokenMap DistributedMap
 */
public interface LoggedOutTokenCache {

    /**
     * Check if the cache contains the specified token or cookie.
     *
     * @param key The key to check for.
     * @return True if the cache contains the key; otherwise, false.
     */
    public boolean contains(Object key);

    /**
     * Put the specified key/value pair into the cache.
     *
     * @param key The key to store.
     * @param value The value to store.
     */
    public void put(Object key, Object value);

    /**
     * Determine if configuration was provided that indicates we should always track tokens / cookies.
     *
     * @return True if we should track tokens / cookies; otherwise, false.
     */
    public boolean shouldTrackTokens();
}
