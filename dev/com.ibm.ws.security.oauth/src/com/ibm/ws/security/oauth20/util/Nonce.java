/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oauth20.util;

import java.io.Serializable;

import com.ibm.oauth.core.internal.OAuthUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class Nonce implements Serializable {

    private static TraceComponent tc = Tr.register(Nonce.class, null, null);

    private static final long serialVersionUID = -8717898447122773693L;

    private static long dftLifetime = 5 * 60 * 1000; // default expires in 5 minutes

    private String nonceValue;
    private long generated;
    private long lifetime;

    /**
     * Create an instance with the given values
     * 
     * @param nonceValue random String value of the nonce
     * @param generated  nonce generation time in nanoseconds
     * @param lifetime   lifetime of this nonce in milliseconds
     */
    private Nonce(String nonceValue, long generated, long lifetime) {
        this.nonceValue = nonceValue;
        this.generated = generated;
        this.lifetime = lifetime * 1000000; // convert to nanoseconds
    }

    /**
     * This nonce is valid if all of the following are true:
     *    nonceValue is equal to the supplied nonceValue
     *    It is not expired
     *    The current time >= the time the nonce was generated
     * 
     * @param nonceValue
     * @return true if the nonce is valid
     */
    public boolean isValid(String nonceValue) {
        long currentTime = System.nanoTime();
        if (!this.nonceValue.equals(nonceValue)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "secrets not equal", new Object[] { this.nonceValue, nonceValue });
            return false;
        }
        if (!((currentTime - generated) < lifetime)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "nonce expired", new Object[] { currentTime, generated, lifetime });
            return false;
        }
        if (!((currentTime - generated) >= 0)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "current time < generation time", new Object[] { currentTime, generated, lifetime });
            return false;
        }
        return true;
    }

    /**
     * Returns true if the nonce is expired
     * 
     * @return 
     */
    public boolean isExpired() {
        return (System.nanoTime() - generated) >= lifetime;
    }

    /**
     * Generates an instance with a random nonce value
     * 
     * @param lifetime the lifetime of this nonce in milliseconds
     * @return the new instance
     */
    public static Nonce getInstance(long lifetime) {
        return new Nonce(OAuthUtil.getRandom(16), System.nanoTime(),
                lifetime);
    }

    /**
     * Generates an instance with a random nonce value with default expiration
     * 
     * @return the new instance
     */
    public static Nonce getInstance() {
        return getInstance(dftLifetime);
    }

    public String getValue() {
        return this.nonceValue;
    }

    public String toString() {
        return this.nonceValue;
    }
}
