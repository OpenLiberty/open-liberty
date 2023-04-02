/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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

    private static Set<OidcSessionInfo> invalidatedSessions;
    private static Map<String, OidcSessionsStore> subToOidcSessionsMap;

    public InMemoryOidcSessionCache() {
        invalidatedSessions = Collections.synchronizedSet(new HashSet<>());
        subToOidcSessionsMap = Collections.synchronizedMap(new HashMap<>());
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

}
