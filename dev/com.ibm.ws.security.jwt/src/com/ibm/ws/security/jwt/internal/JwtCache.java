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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.common.structures.CacheValue;
import com.ibm.ws.security.common.structures.SingleTableCache;

public class JwtCache extends SingleTableCache {

    static final int DEFAULT_ENTRY_LIMIT = 500;

    public JwtCache(long timeoutInMilliSeconds) {
        super(DEFAULT_ENTRY_LIMIT, timeoutInMilliSeconds);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public Object get(@Sensitive String jwt, String configId) {
        JwtCacheKey key = getCacheKey(jwt, configId);
        return super.get(key);
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public void put(@Sensitive String jwt, String configId, JwtContext value, long clockSkew) {
        JwtCacheKey key = getCacheKey(jwt, configId);
        super.put(key, value, clockSkew);
    }

    private JwtCacheKey getCacheKey(@Sensitive String jwt, String configId) {
        return new JwtCacheKey(jwt, configId);
    }

    @Override
    protected CacheValue createCacheValue(Object jwtContext, long clockSkew) {
        return new JwtCacheValue(jwtContext, clockSkew);
    }

}
