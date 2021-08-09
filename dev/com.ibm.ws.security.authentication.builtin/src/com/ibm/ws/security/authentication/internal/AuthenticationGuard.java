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
package com.ibm.ws.security.authentication.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.authentication.AuthenticationData;

/**
 * The authentication guard is used to create a synchronization boundary when authenticating
 * with the same authentication data AND the authentication cache is available. There is no
 * locking when the authentication data is not the same.
 * <p>
 * The usage pattern for the user of this class is to request access at the beginning of the
 * authentication, query the authentication cache, optionally proceed with the JAAS login,
 * insert the subject in the authentication cache, and finally relinquish access.
 * </p>
 * The size of the internal map will be bounded. Leave unbounded for debugging purposes.
 */
public class AuthenticationGuard {

    private static final TraceComponent tc = Tr.register(AuthenticationGuard.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final Map<Object, ReentrantLock> authenticationDataLocks = new HashMap<Object, ReentrantLock>();

    /**
     * Obtains a reentrant lock for the given authentication data.
     * 
     * @param authenticationData
     * @return the reentrant lock assigned to the given authentication data.
     */
    public synchronized ReentrantLock requestAccess(AuthenticationData authenticationData) {
        ReentrantLock currentLock = null;
        currentLock = authenticationDataLocks.get(authenticationData);
        if (currentLock == null) {
            currentLock = new ReentrantLock();
            authenticationDataLocks.put(authenticationData, currentLock);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The size of the authenticationDataLocks is ", authenticationDataLocks.size());
        }
        return currentLock;
    }

    /**
     * Unlocks the lock and cleans up internal state.
     * 
     * @param authenticationData the authentication data.
     * @param currentLock the reentrant lock assigned to the given authentication data.
     */
    public synchronized void relinquishAccess(AuthenticationData authenticationData, ReentrantLock currentLock) {
        if (currentLock != null) {
            ReentrantLock savedLock = authenticationDataLocks.get(authenticationData);
            if (currentLock == savedLock) {
                authenticationDataLocks.remove(authenticationData);
            }
            currentLock.unlock();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "The size of the authenticationDataLocks is ", authenticationDataLocks.size());
        }
    }

}
