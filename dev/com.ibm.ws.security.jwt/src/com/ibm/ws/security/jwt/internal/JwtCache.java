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
import java.util.Map.Entry;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.SingleTableCache;
import com.ibm.ws.security.jwt.config.JwtConsumerConfig;

public class JwtCache extends SingleTableCache {

    private static final TraceComponent tc = Tr.register(JwtCache.class);

    static final int DEFAULT_ENTRY_LIMIT = 500;

    private final JwtConsumerConfig config;

    public JwtCache(long timeoutInMilliSeconds, JwtConsumerConfig config) {
        super(DEFAULT_ENTRY_LIMIT, timeoutInMilliSeconds);
        this.config = config;
    }

    /**
     * Implementation of the eviction strategy.
     */
    @Override
    protected synchronized void evictStaleEntries() {
        List<String> keysToRemove = new ArrayList<String>();
        for (Entry<String, Object> entry : lookupTable.entrySet()) {
            JwtContext jwtContext = (JwtContext) entry.getValue();
            if (isJwtExpired(jwtContext)) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (String keyToRemove : keysToRemove) {
            lookupTable.remove(keyToRemove);
        }
    }

    @FFDCIgnore(MalformedClaimException.class)
    public boolean isJwtExpired(JwtContext jwtContext) {
        JwtClaims jwtClaims = jwtContext.getJwtClaims();
        if (jwtClaims == null) {
            return true;
        }
        long jwtExp = 0;
        try {
            jwtExp = jwtClaims.getExpirationTime().getValueInMillis();
        } catch (MalformedClaimException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception getting expiration time for JWT: " + e);
            }
            return true;
        }
        return (System.currentTimeMillis() > (jwtExp + config.getClockSkew()));
    }

}
