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

/**
 * Interface for cache used to keep track of oidc sessions based on the sub, sid, and the oidc session id.
 */
public interface OidcSessionCache {

    /**
     * Insert a new session into the cache.
     * The sid may be null or empty if it is not available.
     *
     * @param sub The sub claim.
     * @param sid The sid claim.
     * @param oidcSessionId The id of the oidc session.
     * @return Whether or not the session was inserted.
     */
    public boolean insertSession(String sub, String sid, String oidcSessionId);

    /**
     * Invalidate a session in the cache.
     * The invalidated session still has to be removed by invoking removeSession.
     *
     * @param sub The sub claim.
     * @param sid The sid claim.
     * @return Whether or not the session was invalidated.
     */
    public boolean invalidateSession(String sub, String sid);

    /**
     * Invalidate all the sessions belonging to a sub.
     * The invalidated sessions still have to be removed by invoking removeSession on each session.
     *
     * @param sub The sub claim
     * @return Whether or not the sessions were invalidated.
     */
    public boolean invalidateSessions(String sub);

    /**
     * Remove a session from the cache.
     *
     * @param oidcSessionId The id of the oidc session.
     * @return Whether or not the session was removed.
     */
    public boolean removeInvalidatedSession(String oidcSessionId);

    /**
     * Check if the oidc session has been invalidated.
     *
     * @param oidcSessionId The id of the oidc session.
     * @return Whether or not the oidc session has been invalidated.
     */
    public boolean isSessionInvalidated(String oidcSessionId);

}
