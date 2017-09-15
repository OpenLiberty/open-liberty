/*******************************************************************************
 * Copyright (c) 1997, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.cache.CacheProvider;
import com.ibm.wsspi.cache.CacheProviderLoader;

/**
 * Loads the CacheProviders configured as eclipse plugins using eclipse extension registry pattern or placed in the WAS
 * lib dir. using the WAS ExtClassloader.
 */
public class CacheProviderLoaderImpl implements CacheProviderLoader {

    private static TraceComponent tc = Tr.register(CacheProviderLoaderImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private List<CacheServiceImpl> cacheServices = new LinkedList<CacheServiceImpl>();

    // singleton design pattern
    static CacheProviderLoaderImpl cacheLoader = null;

    private CacheProviderLoaderImpl() {
    }

    public static synchronized CacheProviderLoaderImpl getInstance() {
        if (null == cacheLoader) {
            cacheLoader = new CacheProviderLoaderImpl();
        }
        return cacheLoader;
    }

    @Override
    public CacheProvider getCacheProvider(String name) {
        synchronized (cacheServices) {
            for (CacheServiceImpl csi : cacheServices) {
                if (csi.getCacheConfig().getCacheProviderName().equals(name)) {
                    return csi.getCacheProvider();
                }
            }
        }

        return null;
    }

    @Override
    public Map<String, CacheProvider> getCacheProviders() {
        HashMap<String, CacheProvider> providers = new HashMap<String, CacheProvider>(cacheServices.size());
        synchronized (cacheServices) {
            for (CacheServiceImpl csi : cacheServices) {
                providers.put(csi.getCacheConfig().getCacheProviderName(), csi.getCacheProvider());
            }
        }

        return providers;
    }

    public void addCacheProvider(CacheServiceImpl cacheServiceImpl) {
        synchronized (cacheServices) {
            cacheServices.add(cacheServiceImpl);
        }
    }

    public void removeCacheProvider(CacheServiceImpl cacheServiceImpl) {
        synchronized (cacheServices) {
            cacheServices.remove(cacheServiceImpl);
        }
    }
}
