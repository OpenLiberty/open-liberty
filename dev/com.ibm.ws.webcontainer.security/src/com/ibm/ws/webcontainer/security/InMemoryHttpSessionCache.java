/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Local in-memory cache used to keep track of active http sessions based on the sub, sid, and http session id.
 * The cache contains a map which maps a sub to an http session store.
 * The http session store contains a map which maps a sid to a list of http session id's.
 * Http session id's which do not have an associated sid are grouped together in that sub's store.
 * A set containing all the active http sessions is also maintained to check if a session is active in constant time.
 */
public class InMemoryHttpSessionCache implements HttpSessionCache {

    private static Set<String> activeHttpSessions;
    private static Map<String, HttpSessionsStore> subToHttpSessionsMap;

    public InMemoryHttpSessionCache() {
        activeHttpSessions = Collections.synchronizedSet(new HashSet<>());
        subToHttpSessionsMap = Collections.synchronizedMap(new HashMap<>());
    }

    @Override
    public void insertSession(String sub, String sid, String httpSessionId) {
        if (sub == null || sub.isEmpty()) {
            return;
        }
        if (httpSessionId == null || httpSessionId.isEmpty()) {
            return;
        }

        if (!subToHttpSessionsMap.containsKey(sub)) {
            HttpSessionsStore httpSessionsStore = new HttpSessionsStore();
            subToHttpSessionsMap.put(sub, httpSessionsStore);
        }

        HttpSessionsStore httpSessionsStore = subToHttpSessionsMap.get(sub);
        if (httpSessionsStore.insertSession(sid, httpSessionId)) {
            activeHttpSessions.add(httpSessionId);
        }
    }

    @Override
    public void removeSession(String sub, String sid) {
        if (sub == null || sub.isEmpty()) {
            return;
        }

        HttpSessionsStore httpSessionsStore = subToHttpSessionsMap.get(sub);

        List<String> sessionsToRemove = httpSessionsStore.getSessions(sid);
        for (String sessionToRemove : sessionsToRemove) {
            activeHttpSessions.remove(sessionToRemove);
        }

        httpSessionsStore.removeSession(sid);
    }

    @Override
    public boolean isSessionActive(String httpSessionId) {
        return activeHttpSessions.contains(httpSessionId);
    }

    private class HttpSessionsStore {
        private final Map<String, List<String>> sidToSessionsMap;

        public HttpSessionsStore() {
            sidToSessionsMap = new HashMap<>();
        }

        public boolean insertSession(String sid, String httpSessionId) {
            if (!sidToSessionsMap.containsKey(sid)) {
                sidToSessionsMap.put(sid, new ArrayList<>());
            }
            List<String> sessions = sidToSessionsMap.get(sid);
            if (sid != null && !sid.isEmpty() && sessions.size() > 0) {
                return false;
            }
            return sessions.add(httpSessionId);
        }

        public List<String> getSessions(String sid) {
            List<String> sessions = new ArrayList<>();
            if (sid != null && !sid.isEmpty()) {
                if (sidToSessionsMap.containsKey(sid)) {
                    sessions.addAll(sidToSessionsMap.get(sid));
                }
            } else {
                for (String key : sidToSessionsMap.keySet()) {
                    sessions.addAll(sidToSessionsMap.get(key));
                }
            }
            return sessions;
        }

        public void removeSession(String sid) {
            if (sid != null && !sid.isEmpty()) {
                sidToSessionsMap.remove(sid);
            } else {
                sidToSessionsMap.clear();
            }
        }

    }
}
