/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.clients.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local in-memory cache used to keep track of oidc sessions based on the sub, sid, and the oidc session id.
 * The cache contains a map which maps a sub to an oidc session store.
 * The oidc session store contains a map which maps a sid to a list of oidc session id's.
 * Oidc session id's which do not have an associated sid are grouped together in that sub's store.
 * A set containing all the invalidated oidc sessions is also maintained to check if a session is invalid in constant time.
 */
public class InMemoryOidcSessionCache implements OidcSessionCache {

    private static Set<String> invalidatedSessions;
    private static Map<String, OidcSessionsStore> subToOidcSessionsMap;

    public InMemoryOidcSessionCache() {
        invalidatedSessions = Collections.synchronizedSet(new HashSet<>());
        subToOidcSessionsMap = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public boolean insertSession(String sub, String sid, String oidcSessionId) {
        if (sub == null || sub.isEmpty()) {
            return false;
        }
        if (oidcSessionId == null || oidcSessionId.isEmpty()) {
            return false;
        }

        if (!subToOidcSessionsMap.containsKey(sub)) {
            OidcSessionsStore httpSessionsStore = new OidcSessionsStore();
            subToOidcSessionsMap.put(sub, httpSessionsStore);
        }

        OidcSessionsStore httpSessionsStore = subToOidcSessionsMap.get(sub);
        return httpSessionsStore.insertSession(sid, oidcSessionId);
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

        String sessionToInvalidate = httpSessionsStore.getSession(sid);
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

        if (!httpSessionsStore.removeSessionBySessionId(oidcSessionId)) {
            return false;
        }

        return invalidatedSessions.add(oidcSessionId);
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

        List<String> sessionsToInvalidate = httpSessionsStore.getSessions();
        if (sessionsToInvalidate.size() == 0) {
            return false;
        }

        httpSessionsStore.removeSessions();

        return invalidatedSessions.addAll(sessionsToInvalidate);
    }

    @Override
    public boolean removeInvalidatedSession(String oidcSessionId) {
        return invalidatedSessions.remove(oidcSessionId);
    }

    @Override
    public boolean isSessionInvalidated(String oidcSessionId) {
        return invalidatedSessions.contains(oidcSessionId);
    }

    private class OidcSessionsStore {
        private final Map<String, List<String>> sidToSessionsMap;

        public OidcSessionsStore() {
            sidToSessionsMap = new HashMap<>();
        }

        public boolean insertSession(String sid, String oidcSessionId) {
            if (!sidToSessionsMap.containsKey(sid)) {
                sidToSessionsMap.put(sid, new ArrayList<>());
            }
            List<String> sessions = sidToSessionsMap.get(sid);
            if (sid != null && !sid.isEmpty() && sessions.size() > 0) {
                return false;
            }
            return sessions.add(oidcSessionId);
        }

        public String getSession(String sid) {
            if (sid == null || sid.isEmpty()) {
                return null;
            }
            if (!sidToSessionsMap.containsKey(sid)) {
                return null;
            }
            return sidToSessionsMap.get(sid).get(0);
        }

        public List<String> getSessions() {
            List<String> sessions = new ArrayList<>();
            for (String key : sidToSessionsMap.keySet()) {
                sessions.addAll(sidToSessionsMap.get(key));
            }
            return sessions;
        }

        public boolean removeSession(String sid) {
            if (sid != null && !sid.isEmpty()) {
                List<String> removedSession = sidToSessionsMap.remove(sid);
                return removedSession != null;
            }
            return false;
        }

        public boolean removeSessionBySessionId(String oidcSessionId) {
            for (String key : sidToSessionsMap.keySet()) {
                List<String> oidcSessionIds = sidToSessionsMap.get(key);
                for (int i = 0; i < oidcSessionIds.size(); i++) {
                    if (oidcSessionIds.get(i).equals(oidcSessionId)) {
                        oidcSessionIds.remove(i);
                        if (oidcSessionIds.size() == 0) {
                            sidToSessionsMap.remove(key);
                        }
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean removeSessions() {
            if (sidToSessionsMap.keySet().size() == 0) {
                return false;
            }
            sidToSessionsMap.clear();
            return true;
        }

    }
}
