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
import java.util.Set;
import java.util.TreeSet;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * A layer on top of the ArrayList data structure that is stored in JCache.
 * This provides for type safety and hides the details of accessing the ArrayList.
 */
class SessionInfo {
    /**
     * Indices and size for ArrayList values that are stored in the cache.
     * 
     * Data types are:
     * long            - CREATION_TIME
     * long            - LAST_ACCESS
     * int             - MAX_INACTIVE_TIME
     * short           - LISTENER_COUNT
     * String          - USER
     * TreeSet<String> - PROP_IDS
     */
    private static int CREATION_TIME = 0, LAST_ACCESS = 1, MAX_INACTIVE_TIME = 2, LISTENER_COUNT = 3, USER = 4, PROP_IDS = 5, SIZE = 6; 

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
    SessionInfo(ArrayList<?> list) {
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
    SessionInfo(long creationTime, int maxInactiveInterval, short listenerCount, String userName) {
        // When adding array elements, order must match the numeric value of the constants
        list = new ArrayList<Object>(SIZE);
        list.add(creationTime); // CREATION_TIME
        list.add(creationTime); // LAST_ACCESS
        list.add(maxInactiveInterval);
        list.add(listenerCount);
        list.add(userName);
        list.add(null); // PROP_IDS
    }

    void addSessionPropertyIds(Set<String> propIdsToAdd) {
        Object o = list.get(PROP_IDS);
        if (o instanceof TreeSet) {
            @SuppressWarnings("unchecked")
            TreeSet<String> propIds = (TreeSet<String>) o;
            propIds.addAll(propIdsToAdd);
        } else {
            list.set(PROP_IDS, new TreeSet<String>(propIdsToAdd));
        }
    }

    /**
     * Create a copy of this instance which is backed by a copy of its ArrayList.
     * 
     * @return the clone.
     */
    protected SessionInfo clone() {
        @SuppressWarnings("unchecked")
        ArrayList<Object> newList = (ArrayList<Object>) list.clone();
        return new SessionInfo(newList);
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

    @SuppressWarnings("unchecked")
    Set<String> getSessionPropertyIds() {
        Object o = list.get(PROP_IDS);
        return o instanceof TreeSet ? (TreeSet<String>) o : null;
    }

    String getUser() {
        return (String) list.get(USER);
    }

    void removeSessionPropertyIds(Set<String> propIdsToRemove) {
        Object o = list.get(PROP_IDS);
        if (o instanceof TreeSet) {
            @SuppressWarnings("unchecked")
            TreeSet<String> propIds = (TreeSet<String>) o;
            propIds.removeAll(propIdsToRemove);
        }        
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
        return new StringBuilder("SessionInfo for ").append(list.get(USER))
                        .append(" created ").append(list.get(CREATION_TIME))
                        .append(" accessed " ).append(list.get(LAST_ACCESS))
                        .append(" listeners ").append(list.get(LISTENER_COUNT))
                        .append(" maxInactive ").append(list.get(MAX_INACTIVE_TIME))
                        .append(' ').append(list.get(PROP_IDS))
                        .toString();
    }
}