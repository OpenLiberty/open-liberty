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
package com.ibm.ws.security.common.structures;

import java.util.Objects;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class CacheValue {

    private static final TraceComponent tc = Tr.register(CacheValue.class);

    private Object value;
    private long createdAt = 0L;

    public CacheValue(Object value) {
        this(value, 0);
    }

    public CacheValue(Object value, long clockSkew) {
        this.value = value;
        this.createdAt = System.currentTimeMillis();
        if (clockSkew > 0) {
            // Take the clock skew into account up front
            this.createdAt += clockSkew;
        }
    }

    public Object getValue() {
        return value;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long timeoutInMilliseconds) {
        long now = System.currentTimeMillis();
        if ((now - createdAt) > timeoutInMilliseconds) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Entry is considered expired; the current time " + now + " - the created at time " + createdAt + " was larger than the specified timeout " + timeoutInMilliseconds);
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CacheValue other = (CacheValue) obj;
        return Objects.equals(value, other.value);
    }

}
