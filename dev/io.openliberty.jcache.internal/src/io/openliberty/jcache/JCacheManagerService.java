/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache;

import javax.cache.CacheManager;

import io.openliberty.jcache.internal.JCachingProviderService;

/**
 * Interface that represents service to interact with a {@link CacheManager}
 * instance.
 */
public interface JCacheManagerService {

    /**
     * Get the {@link CacheManager} this service represents.
     *
     * @return The {@link CacheManager} for this service.
     */
    public CacheManager getCacheManager();

    /**
     * Get the {@link JCachingProviderService} that is the parent of this
     * {@link JCacheManagerService}.
     *
     * @return The parent {@link JCachingProviderService}.
     */
    public JCachingProviderService getJCachingProviderService();
}
