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
package com.ibm.ws.security.jwt.internal;

import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.CacheValue;

class JwtCacheValue extends CacheValue {

    private static final TraceComponent tc = Tr.register(JwtCacheValue.class);

    private final long clockSkew;

    public JwtCacheValue(Object value, long clockSkew) {
        super(value, clockSkew);
        this.clockSkew = clockSkew;
    }

    @FFDCIgnore(MalformedClaimException.class)
    @Override
    public boolean isExpired(long timeoutInMilliseconds) {
        if (super.isExpired(timeoutInMilliseconds)) {
            return true;
        }
        JwtContext jwtContext = (JwtContext) getValue();
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
        return (System.currentTimeMillis() > (jwtExp + clockSkew));
    }

}
