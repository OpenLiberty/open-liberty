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
 * Interface for cache used to keep track of active http sessions based on the sub, sid, and http session id.
 */
public interface HttpSessionCache {

    /**
     * Insert a new session into the cache.
     * The sid may be null or empty if it is not available.
     *
     * @param sub The sub claim.
     * @param sid The sid claim.
     * @param httpSessionId The id of the http session.
     */
    public void insertSession(String sub, String sid, String httpSessionId);

    /**
     * Remove a session from the cache.
     * If the sid is null or empty, all the sub's sessions will be removed.
     *
     * @param sub The sub claim.
     * @param sid The sid claim.
     */
    public void removeSession(String sub, String sid);

    /**
     * Check if the http session is active.
     *
     * @param httpSessionId The id of the http session.
     */
    public boolean isSessionActive(String httpSessionId);

}
