/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.backchannellogout.internal;

import org.jose4j.jwt.JwtClaims;

import com.ibm.ws.security.common.structures.CacheValue;
import com.ibm.ws.security.common.structures.SingleTableCache;

public class BackchannelLogoutJtiCache extends SingleTableCache {

    static final int DEFAULT_ENTRY_LIMIT = 500;

    public BackchannelLogoutJtiCache(long timeoutInMilliSeconds) {
        super(DEFAULT_ENTRY_LIMIT, timeoutInMilliSeconds);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public Object get(String jti, String configId) {
        JtiCacheKey key = getCacheKey(jti, configId);
        return super.get(key);
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public void put(String jti, String configId, JwtClaims value, long clockSkew) {
        JtiCacheKey key = getCacheKey(jti, configId);
        super.put(key, value, clockSkew);
    }

    private JtiCacheKey getCacheKey(String jti, String configId) {
        return new JtiCacheKey(jti, configId);
    }

    @Override
    protected CacheValue createCacheValue(Object claims, long clockSkew) {
        return new JtiCacheValue((JwtClaims) claims, clockSkew);
    }

}
