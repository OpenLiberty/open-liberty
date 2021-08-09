/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import org.osgi.service.component.annotations.Component;

import com.ibm.wsspi.cache.CacheFeatureSupport;
import com.ibm.wsspi.cache.CacheProvider;
import com.ibm.wsspi.cache.CoreCache;

@Component(service = CacheProvider.class, property = { "name=" + CacheConfig.CACHE_PROVIDER_DYNACACHE, "service.vendor=IBM" })
public class DefaultCacheProvider implements CacheProvider {
    @Override
    public void stop() {

    }

    @Override
    public void start() {

    }

    @Override
    public String getName() {
        return CacheConfig.CACHE_PROVIDER_DYNACACHE;
    }

    @Override
    public CacheFeatureSupport getCacheFeatureSupport() {
        return null;
    }

    @Override
    public CoreCache createCache(com.ibm.wsspi.cache.CacheConfig cacheConfig) {
        return null;
    }
}
