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

public class CacheEntry {

    private Object value;
    private long createdAt = 0L;

    public CacheEntry(Object value) {
        this.value = value;
        this.createdAt = System.currentTimeMillis();
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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CacheEntry other = (CacheEntry) obj;
        if (value == null) {
            if (other.getValue() != null) {
                return false;
            }
        } else if (!value.equals(other.getValue())) {
            return false;
        }
        return true;
    }

}
