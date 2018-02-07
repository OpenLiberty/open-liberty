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
 * Key for a session property that is stored in the cache.
 */
@Trivial
public class SessionPropertyKey implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Property id.
     */
    public final String propId;

    /**
     * Session id.
     */
    public final String sessionId;

    /**
     * Construct a new cache key for a session property.
     * 
     * @param sessionId session id.
     * @param propId property id.
     */
    public SessionPropertyKey(String sessionId, String propId) {
        this.sessionId = sessionId;
        this.propId = propId;
    }

    @Override
    public boolean equals(Object o) {
        SessionPropertyKey s = o instanceof SessionPropertyKey ? (SessionPropertyKey) o : null;
        return s != null
                        && (sessionId == null ? s.sessionId == null : sessionId.equals(s.sessionId))
                        && (propId == null ? s.propId == null : propId.equals(s.propId));
    }

    @Override
    public int hashCode() {
        return sessionId.hashCode() + propId.hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("SessionPropertyKey:").append(sessionId).append('.').append(propId).toString();
    }
}
