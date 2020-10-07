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
package com.ibm.oauth.core.api.oauth20.token;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;

/**
 * OAuth 2.0 token cache interface. Component consumers must provide a class
 * which implements this for persisting token information.
 * 
 */
public interface OAuth20TokenCache {

    /**
     * 
     * This method is called by a factory when an instance of this object is
     * created. The configuration object will allow the token cache to
     * initialize itself.
     * 
     * @param config
     *            - Configuration entity for the component instance
     */
    public void init(OAuthComponentConfiguration config);

    /**
     * Adds an OAuth20Token object to the cache. It is reasonable to assume that
     * there should not be an existing object in the cache with this lookupKey.
     * This will allow for implementations to optimize for insert.
     * 
     * @param lookupKey
     *            - The key that will be later used to lookup the entry
     * @param entry
     *            - The entry to store in a cache.
     * @param lifetime
     *            - The length of time we should store the token in seconds
     */
    public void add(String lookupKey, OAuth20Token entry, int lifetime);

    /**
     * Removes an entry previously stored in our cache identified by the
     * lookupKey. If the entry does not exist the method should return without
     * error.
     * 
     * @param lookupKey
     *            - index into cache for the entry to remove
     */
    public void remove(String lookupKey);

    /**
     * Returns an entry for the given lookup key that was previously stored in
     * our cache. The entry is NOT removed from the cache. If the entry does not
     * exist this will return null.
     * 
     * @param lookupKey
     *            - index into cache for the entry
     * @return The stored entry, or null if there is no current valid entry for
     *         the given lookupKey.
     */
    public OAuth20Token get(String lookupKey);
}
