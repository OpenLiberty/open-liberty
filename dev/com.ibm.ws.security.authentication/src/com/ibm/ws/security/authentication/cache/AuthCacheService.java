/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package com.ibm.ws.security.authentication.cache;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Sensitive;

/**
 * The authentication cache service.
 */
public interface AuthCacheService {

    /**
     * Inserts the subject into the cache.
     *
     * @param subject
     */
    public void insert(Subject subject);

    /**
     * Inserts the subject into the cache using a X509Certicate as the key.
     *
     * @param subject
     * @param client  certificate
     */
    public void insert(Subject subject, java.security.cert.X509Certificate[] certChain);

    /**
     * Inserts the subject into the cache. The userid and password may be used by the BasicAuthCacheKeyProvider
     * to create a key.
     *
     * @param subject
     * @param userid
     * @param password
     */
    public void insert(Subject subject, String userid, String password);

    /**
     * Gets the subject from the cache using the specified cache key.
     * Only valid subjects are returned. An invalid subject found is immediately removed from the cache.
     *
     * @param cacheKey
     * @return the valid subject or <code>null</code>.
     */
    public Subject getSubject(@Sensitive Object cacheKey);

    /**
     * Removes the subject specified by the cache key from the cache.
     *
     * @param cacheKey
     */
    public void remove(@Sensitive Object cacheKey);

    /**
     * Removes all entries from the cache.
     */
    public void removeAllEntries();

    /**
     * Removes all entries from the cache.
     *
     * @param force Whether to force the clearing of the cache.
     */
    public void removeAllEntries(boolean force);

    /**
     * Whether to automatically clear cache entries. This is intended for distributed caches
     * where over-zealous clearing could lead to performance degradation.
     */
    public boolean getAutoClearCache();

    /**
     * Return whether the server is started.
     *
     * @return True if the server is started.
     */
    public boolean isServerStarted();
}
