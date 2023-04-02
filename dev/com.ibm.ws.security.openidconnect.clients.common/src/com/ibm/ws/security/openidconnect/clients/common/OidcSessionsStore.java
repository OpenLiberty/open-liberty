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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OidcSessionsStore {

    private final Map<String, List<OidcSessionInfo>> sidToSessionsMap;

    public OidcSessionsStore() {
        sidToSessionsMap = new HashMap<>();
    }

    public boolean insertSession(String sid, OidcSessionInfo oidcSessionInfo) {
        if (!sidToSessionsMap.containsKey(sid)) {
            sidToSessionsMap.put(sid, new ArrayList<>());
        }
        List<OidcSessionInfo> sessions = sidToSessionsMap.get(sid);
        if (sid != null && !sid.isEmpty() && !sessions.isEmpty()) {
            return false;
        }
        return sessions.add(oidcSessionInfo);
    }

    public OidcSessionInfo getSession(String sid) {
        if (sid == null || sid.isEmpty()) {
            return null;
        }
        if (!sidToSessionsMap.containsKey(sid)) {
            return null;
        }
        return sidToSessionsMap.get(sid).get(0);
    }

    public List<OidcSessionInfo> getSessions() {
        List<OidcSessionInfo> sessions = new ArrayList<>();
        for (String key : sidToSessionsMap.keySet()) {
            sessions.addAll(sidToSessionsMap.get(key));
        }
        return sessions;
    }

    public boolean removeSession(String sid) {
        if (sid != null && !sid.isEmpty()) {
            List<OidcSessionInfo> removedSession = sidToSessionsMap.remove(sid);
            return removedSession != null;
        }
        return false;
    }

    public OidcSessionInfo removeSessionBySessionId(String oidcSessionId) {
        for (String key : sidToSessionsMap.keySet()) {
            List<OidcSessionInfo> oidcSessions = sidToSessionsMap.get(key);
            for (int i = 0; i < oidcSessions.size(); i++) {
                OidcSessionInfo oidcSessionInfo = oidcSessions.get(i);
                if (oidcSessionInfo.getSessionId().equals(oidcSessionId)) {
                    oidcSessions.remove(i);
                    if (oidcSessions.isEmpty()) {
                        sidToSessionsMap.remove(key);
                    }
                    return oidcSessionInfo;
                }
            }
        }
        return null;
    }

    public boolean removeSessions() {
        if (sidToSessionsMap.keySet().isEmpty()) {
            return false;
        }
        sidToSessionsMap.clear();
        return true;
    }

}
