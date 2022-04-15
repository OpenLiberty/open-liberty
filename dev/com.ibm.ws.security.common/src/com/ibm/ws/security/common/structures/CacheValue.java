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
package com.ibm.ws.security.common.structures;

import java.util.Objects;

public class CacheValue {

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
