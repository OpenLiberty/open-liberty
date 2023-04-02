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
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.common.structures.CacheValue;

class JtiCacheValue extends CacheValue {

    private static final TraceComponent tc = Tr.register(JtiCacheValue.class);

    private final long clockSkew;

    public JtiCacheValue(JwtClaims claims, long clockSkew) {
        super(claims, clockSkew);
        this.clockSkew = clockSkew;
    }

    @FFDCIgnore(MalformedClaimException.class)
    @Override
    public boolean isExpired(long timeoutInMilliseconds) {
        if (super.isExpired(timeoutInMilliseconds)) {
            return true;
        }
        JwtClaims claims = (JwtClaims) getValue();
        if (claims == null) {
            return true;
        }
        long jwtExp = 0;
        try {
            NumericDate expirationTime = claims.getExpirationTime();
            if (expirationTime == null) {
                // Logout tokens aren't required to have an exp claim, so don't consider the entry expired if one is missing
                return false;
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
