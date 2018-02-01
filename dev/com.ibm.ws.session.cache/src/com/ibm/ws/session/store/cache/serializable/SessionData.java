/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.store.cache.serializable;

import java.io.Serializable;
import java.util.Arrays;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A compound value that can be stored in the cache because it is serializable and supports hashCode/equals.
 */
@Trivial
public class SessionData implements Cloneable, Serializable {
    private static final long serialVersionUID = 1L;

    private byte[] bytes;
    private long creationTime;
    private long lastAccess;
    private short listenerCount;
    private int maxInactiveTime;
    private String userName;

    @Override
    public SessionData clone() {
        try {
            return (SessionData) super.clone();
        } catch (CloneNotSupportedException x) {
            throw new Error(x); // unreachable
        }
    }

    @Override
    public boolean equals(Object o) {
        SessionData s = o instanceof SessionData ? (SessionData) o : null;
        return s != null
                        && creationTime == s.creationTime
                        && lastAccess == s.lastAccess
                        && listenerCount == s.listenerCount
                        && maxInactiveTime == s.maxInactiveTime
                        && (userName == null ? s.userName == null : userName.equals(s.userName))
                        && Arrays.equals(bytes, s.bytes);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public long getLastAccess() {
        return lastAccess;
    }

    public short getListenerCount() {
        return listenerCount;
    }

    public int getMaxInactiveTime() {
        return maxInactiveTime;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public int hashCode() {
        return (int) (bytes.length + creationTime + lastAccess + listenerCount + maxInactiveTime);
    }

    public void setBytes(byte[] value) {
        bytes = value;
    }

    public void setCreationTime(long value) {
        creationTime = value;
    }

    public void setLastAccess(long value) {
        lastAccess = value;
    }

    public void setListenerCount(short value) {
        listenerCount = value;
    }

    public void setMaxInactiveTime(int value) {
        maxInactiveTime = value;
    }

    public void setUserName(String value) {
        userName = value;
    }

    @Override
    public String toString() {
        return new StringBuilder("SessionInfo[").append(bytes.length)
                        .append("] for ").append(userName)
                        .append(" created ").append(creationTime)
                        .append(" accessed " ).append(lastAccess)
                        .append(" listeners ").append(listenerCount)
                        .append(" maxInactive ").append(maxInactiveTime)
                        .toString();
    }
}
