/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import java.util.ArrayList;
import java.util.List;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.SingleTableCache;

public class JwtCache extends SingleTableCache {

    private static final TraceComponent tc = Tr.register(JwtCache.class);

    static final int DEFAULT_ENTRY_LIMIT = 500;

    public JwtCache(long timeoutInMilliSeconds) {
        super(DEFAULT_ENTRY_LIMIT, timeoutInMilliSeconds);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(@Sensitive String jwt, String configId) {
        JwtCacheKey key = getCacheKey(jwt, configId);
        JwtCacheValue cacheValue = (JwtCacheValue) get(key);
        if (cacheValue == null || isJwtExpired(cacheValue)) {
            return null;
        }
        return cacheValue.getValue();
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(@Sensitive String jwt, String configId, JwtContext value, long clockSkew) {
        JwtCacheKey key = getCacheKey(jwt, configId);
        JwtCacheValue cacheValue = new JwtCacheValue(value, clockSkew);
        super.put(key, cacheValue, clockSkew);
    }

    /**
     * Implementation of the eviction strategy.
     */
    @Override
    protected synchronized void evictStaleEntries() {
        super.evictStaleEntries();

        List<Object> keysToRemove = new ArrayList<>();
        for (Object key : lookupTable.keySet()) {
            JwtCacheValue cacheValue = (JwtCacheValue) get(key);
            if (cacheValue == null || isJwtExpired(cacheValue)) {
                keysToRemove.add(key);
            }
        }
        for (Object keyToRemove : keysToRemove) {
            remove(keyToRemove);
        }
    }

    @FFDCIgnore(MalformedClaimException.class)
    public boolean isJwtExpired(JwtCacheValue cacheValue) {
        JwtContext jwtContext = (JwtContext) cacheValue.getValue();
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        if (jwtClaims == null) {
            return true;
        }
        long jwtExp = 0;
        try {
            NumericDate expirationTime = jwtClaims.getExpirationTime();
            if (expirationTime == null) {
                return true;
            }
            jwtExp = expirationTime.getValueInMillis();
        } catch (MalformedClaimException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting expiration time for JWT: " + e);
            }
            return true;
        }
        return (System.currentTimeMillis() > (jwtExp + cacheValue.getClockSkew()));
    }

    private JwtCacheKey getCacheKey(@Sensitive String jwt, String configId) {
        return new JwtCacheKey(jwt, configId);
    }

}
