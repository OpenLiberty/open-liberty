/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.clients.common;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

import io.openliberty.security.common.osgi.SecurityOSGiUtils;

/**
 * Local in-memory cache used to keep track of oidc sessions based on the sub, sid, and the oidc session id.
 * The cache contains a map which maps a sub to an oidc session store.
 * The oidc session store contains a map which maps a sid to a list of oidc session id's.
 * Oidc session id's which do not have an associated sid are grouped together in that sub's store.
 * A set containing all the invalidated oidc sessions is also maintained to check if a session is invalid in constant time.
 */
public class InMemoryOidcSessionCache implements OidcSessionCache {

    private static final TraceComponent tc = Tr.register(InMemoryOidcSessionCache.class);

    private final PrivilegedAction<ScheduledExecutorService> getScheduledExecutorServiceAction = new GetScheduledExecutorServiceAction();

    private final Set<OidcSessionInfo> invalidatedSessions;
    private final Map<String, OidcSessionsStore> subToOidcSessionsMap;

    private long timeoutInMilliSeconds = 10 * 60 * 1000;

    private ScheduledExecutorService evictionSchedule;

    public InMemoryOidcSessionCache() {
        this(0);
    }

    public InMemoryOidcSessionCache(long timeoutInMilliSeconds) {
        invalidatedSessions = Collections.synchronizedSet(new HashSet<>());
        subToOidcSessionsMap = Collections.synchronizedMap(new HashMap<>());

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }
        scheduleEvictionTask();
    }

    private void scheduleEvictionTask() {
        if (System.getSecurityManager() == null) {
            evictionSchedule = getScheduledExecutorService();
        } else {
            evictionSchedule = AccessController.doPrivileged(getScheduledExecutorServiceAction);
        }
        if (evictionSchedule != null) {
            evictionSchedule.scheduleWithFixedDelay(new EvictionTask(), timeoutInMilliSeconds, timeoutInMilliSeconds, TimeUnit.MILLISECONDS);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Failed to obtain a ScheduledExecutorService");
            }
        }
    }

    @Override
    public boolean insertSession(OidcSessionInfo oidcSessionInfo) {
        String sub = oidcSessionInfo.getSub();
        if (sub == null || sub.isEmpty()) {
            return false;
        }

        if (!subToOidcSessionsMap.containsKey(sub)) {
            OidcSessionsStore httpSessionsStore = new OidcSessionsStore();
            subToOidcSessionsMap.put(sub, httpSessionsStore);
        }

        String sid = oidcSessionInfo.getSid();
        OidcSessionsStore httpSessionsStore = subToOidcSessionsMap.get(sub);
        return httpSessionsStore.insertSession(sid, oidcSessionInfo);
    }

    @Override
    public Map<String, OidcSessionsStore> getSubMap() {
        return subToOidcSessionsMap;
    }

    @Override
    public boolean invalidateSession(String sub, String sid) {
        if (sub == null || sub.isEmpty()) {
            return false;
        }

        OidcSessionsStore httpSessionsStore = subToOidcSessionsMap.get(sub);
        if (httpSessionsStore == null) {
            return false;
        }

        OidcSessionInfo sessionToInvalidate = httpSessionsStore.getSession(sid);
        if (sessionToInvalidate == null) {
            return false;
        }

        httpSessionsStore.removeSession(sid);

        return invalidatedSessions.add(sessionToInvalidate);
    }

    @Override
    public boolean invalidateSessionBySessionId(String sub, String oidcSessionId) {
        if (sub == null || sub.isEmpty()) {
            return false;
        }

        OidcSessionsStore httpSessionsStore = subToOidcSessionsMap.get(sub);
        if (httpSessionsStore == null) {
            return false;
        }

        OidcSessionInfo sessionAssociatedWithSessionId = httpSessionsStore.removeSessionBySessionId(oidcSessionId);
        if (sessionAssociatedWithSessionId == null) {
            return false;
        }

        return invalidatedSessions.add(sessionAssociatedWithSessionId);
    }

    @Override
    public boolean invalidateSessions(String sub) {
        if (sub == null || sub.isEmpty()) {
            return false;
        }

        OidcSessionsStore httpSessionsStore = subToOidcSessionsMap.get(sub);
        if (httpSessionsStore == null) {
            return false;
        }

        List<OidcSessionInfo> sessionsToInvalidate = httpSessionsStore.getSessions();
        if (sessionsToInvalidate.size() == 0) {
            return false;
        }

        httpSessionsStore.removeSessions();

        return invalidatedSessions.addAll(sessionsToInvalidate);
    }

    @Override
    public boolean removeInvalidatedSession(OidcSessionInfo sessionInfo) {
        return invalidatedSessions.remove(sessionInfo);
    }

    @Override
    public boolean isSessionInvalidated(OidcSessionInfo sessionInfo) {
        return invalidatedSessions.contains(sessionInfo);
    }

    private class GetScheduledExecutorServiceAction implements PrivilegedAction<ScheduledExecutorService> {

        @Override
        public ScheduledExecutorService run() {
            return getScheduledExecutorService();
        }

    }

    private ScheduledExecutorService getScheduledExecutorService() {
        return SecurityOSGiUtils.getService(getClass(), ScheduledExecutorService.class);
    }

    private class EvictionTask implements Runnable {

        @Override
        public void run() {
            evictStaleEntries();
        }

    }

    protected synchronized void evictStaleEntries() {
        removeExpiredSessionsFromInvalidatedSessions();
        removeExpiredSessionsFromSubToOidcSessionsMap();
    }

    private void removeExpiredSessionsFromInvalidatedSessions() {
        Iterator<OidcSessionInfo> sessions = invalidatedSessions.iterator();
        while (sessions.hasNext()) {
            OidcSessionInfo session = sessions.next();
            long currentTimeInMillis = System.currentTimeMillis();
            long expInMillis = new Long(session.getExp());
            if (currentTimeInMillis > expInMillis) {
                invalidatedSessions.remove(session);
            }
        }
    }

    private void removeExpiredSessionsFromSubToOidcSessionsMap() {
        Iterator<String> subs = subToOidcSessionsMap.keySet().iterator();
        while (subs.hasNext()) {
            String sub = subs.next();
            OidcSessionsStore store = subToOidcSessionsMap.get(sub);
            List<OidcSessionInfo> sessions = store.getSessions();
            for (OidcSessionInfo session : sessions) {
                long currentTimeInMillis = System.currentTimeMillis();
                long expInMillis = new Long(session.getExp());
                if (currentTimeInMillis > expInMillis) {
                    store.removeSessionBySessionId(session.getSessionId());
                }
            }
        }
    }

}
