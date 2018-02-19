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

package com.ibm.ws.session.store.cache;

import java.util.ArrayList;
import java.util.BitSet;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A layer on top of the ArrayList data structure that is stored in JCache.
 * This provides for type safety and hides the details of accessing the ArrayList.
 */
class SessionData {
    private static final TraceComponent tc = Tr.register(SessionData.class);

    /**
     * Indices and size for ArrayList values that are stored in the cache.
     * 
     * Data types are:
     * long   - CREATION_TIME
     * long   - LAST_ACCESS
     * int    - MAX_INACTIVE_TIME
     * short  - LISTENER_COUNT
     * String - USER
     * BitSet - BITS
     * 
     * BitSet is used to wrap the byte[] because it is both Serializable and supports a .equals comparison that matches
     * its contents (and is provided by the JDK, so it can deserialize on the server side).
     * Direct use of byte[] would not be valid because its .equals method is an instance comparison.
     * TODO it will hopefully be possible to switch to byte[] once other fields are removed
     * such that we no longer require Cache.replace operations. 
     */
    private static int CREATION_TIME = 0, LAST_ACCESS = 1, MAX_INACTIVE_TIME = 2, LISTENER_COUNT = 3, USER = 4, BITS = 5, SIZE = 6; 

    /**
     * The ArrayList backing this instance.
     */
    private final ArrayList<Object> list;

    // Mutable SessionInfo (allows setters) vs immutable sessionInfo?
    /**
     * The underlying ArrayList reference is held onto by this class after construction.
     * 
     * @param list ArrayList obtained from the cache.
     */
    @SuppressWarnings("unchecked")
    SessionData(@Sensitive ArrayList<?> list) { // avoid tracing all of the bytes
        this.list = (ArrayList<Object>) list;
    }

    /**
     * Construct new SessionInfo object backed by a new ArrayList.
     * 
     * @param creationTime creation time is also the last accessed time
     * @param maxInactiveInterval
     * @param listenerCount
     * @param userName
     */
    SessionData(long creationTime, int maxInactiveInterval, short listenerCount, String userName) {
        // When adding array elements, order must match the numeric value of the constants
        list = new ArrayList<Object>(SIZE);
        list.add(creationTime); // CREATION_TIME
        list.add(creationTime); // LAST_ACCESS
        list.add(maxInactiveInterval);
        list.add(listenerCount);
        list.add(userName);
        list.add(null); // BITS
    }

    /**
     * Create a copy of this instance which is backed by a copy of its ArrayList.
     * 
     * @return the clone.
     */
    protected SessionData clone() {
        @SuppressWarnings("unchecked")
        ArrayList<Object> newList = (ArrayList<Object>) list.clone();
        return new SessionData(newList);
    }

    /**
     * Returns the ArrayList instance backing this object.
     * 
     * @return the ArrayList instance backing this object.
     */
    @Trivial
    ArrayList<Object> getArrayList() {
        return list;
    }

    @Trivial
    byte[] getBytes() {
        Object o = list.get(BITS);
        byte[] bytes = o instanceof BitSet ? ((BitSet) o).toByteArray() : null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "getBytes", bytes == null ? null : bytes.length);
        return bytes;
    }

    long getCreationTime() {
        return (Long) list.get(CREATION_TIME);
    }

    long getLastAccess() {
        return (Long) list.get(LAST_ACCESS);
    }

    short getListenerCount() {
        return (Short) list.get(LISTENER_COUNT);
    }

    int getMaxInactiveTime() {
        return (Integer) list.get(MAX_INACTIVE_TIME);
    }

    String getUser() {
        return (String) list.get(USER);
    }

    @Trivial
    void setBytes(byte[] bytes) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "setBytes", bytes == null ? null : bytes.length);
        list.set(BITS, bytes == null ? null : BitSet.valueOf(bytes));
    }

    void setLastAccess(long time) {
        list.set(LAST_ACCESS, time);
    }

    void setListenerCount(short count) {
        list.set(LISTENER_COUNT, count);
    }

    void setMaxInactiveTime(int max) {
        list.set(MAX_INACTIVE_TIME, max);
    }

    void setUser(String user) {
        list.set(USER, user);
    }

    @Override
    public String toString() {
        Object o = list.get(BITS);
        Integer length = o instanceof BitSet ? (((BitSet) o).length() + 7) / 8 : null; // per BitSet.toByteArray
        return new StringBuilder("SessionData[").append(length)
                        .append("] for ").append(list.get(USER))
                        .append(" created ").append(list.get(CREATION_TIME))
                        .append(" accessed " ).append(list.get(LAST_ACCESS))
                        .append(" listeners ").append(list.get(LISTENER_COUNT))
                        .append(" maxInactive ").append(list.get(MAX_INACTIVE_TIME))
                        .toString();
    }
}