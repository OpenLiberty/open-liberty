/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.saml.sso20.internal.utils;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.saml.TraceConstants;

import io.openliberty.jcache.CacheService;

/**
 * Shared replay store backed by a user-configured JCache (samlWebSso20 replayCacheRef).
 * Atomicity comes from {@link Cache#putIfAbsent(Object, Object)}.
 *
 * NOTE: entry lifetime is governed by the JCache provider's expiry policy, which Liberty
 * cannot set (the cache element only has cacheManagerRef/name). The provider TTL must be
 * >= assertion lifetime + clockSkew, otherwise entries may vanish before the assertion
 * stops being acceptable.
 */
public class JCacheSamlReplayStore implements SamlReplayStore {

    private static final TraceComponent tc = Tr.register(JCacheSamlReplayStore.class,
                                                         TraceConstants.TRACE_GROUP,
                                                         TraceConstants.MESSAGE_BUNDLE);
    private final CacheService cacheService;

    public JCacheSamlReplayStore(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Only putIfAbsent is used on purpose: Cache.invoke is not used because CacheProxy.invoke
     * does not forward the key correctly.
     */
    @Override
    @FFDCIgnore(Exception.class)
    public boolean registerIfAbsent(String key, long expiryMs) throws SamlReplayStoreException {
        try {
            Cache<Object, Object> cache = cacheService.getCache();
            if (cache == null) {
                throw new SamlReplayStoreException("The JCache for the SAML replay store is not available.");
            }
            // putIfAbsent returns true when the entry was stored (first use).
            boolean firstUse = cache.putIfAbsent(key, Long.valueOf(expiryMs));
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Shared replay store putIfAbsent -> firstUse=" + firstUse);
            }
            return !firstUse;
        } catch (SamlReplayStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new SamlReplayStoreException(e);
        }
    }
}
