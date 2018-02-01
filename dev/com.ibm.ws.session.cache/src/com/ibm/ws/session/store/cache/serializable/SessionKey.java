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

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Key for entries that are stored in the cache.
 */
@Trivial
public class SessionKey implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Application name.
     */
    public final String app;

    /**
     * Session id.
     */
    public final String id;

    /**
     * Construct a new cache key for a session.
     * 
     * @param sessionId session id.
     * @param appName application name.
     */
    public SessionKey(String sessionId, String appName) {
        id = sessionId;
        app = appName;
    }

    @Override
    public boolean equals(Object o) {
        SessionKey s = o instanceof SessionKey ? (SessionKey) o : null;
        return s != null
                        && (id == null ? s.id == null : id.equals(s.id))
                        && (app == null ? s.app == null : app.equals(s.app));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("SessionKey:").append(id).append('@').append(app).toString();
    }
}
