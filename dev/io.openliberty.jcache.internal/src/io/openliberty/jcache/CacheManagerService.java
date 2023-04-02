/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.jcache;

import javax.cache.CacheManager;

import io.openliberty.jcache.internal.CachingProviderService;

/**
 * Interface that represents service to interact with a {@link CacheManager}
 * instance.
 */
public interface CacheManagerService {

    /**
     * Get the {@link CacheManager} this service represents.
     *
     * @return The {@link CacheManager} for this service.
     */
    public CacheManager getCacheManager();

    /**
     * Get the {@link CachingProviderService} that is the parent of this
     * {@link CacheManagerService}.
     *
     * @return The parent {@link CachingProviderService}.
     */
    public CachingProviderService getCachingProviderService();
}
