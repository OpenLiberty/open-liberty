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
package com.ibm.ws.security.authentication;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;

import com.ibm.ws.security.authentication.cache.AuthCacheService;

/**
 * The Authentication Service interface. The AuthenticationService implementation performs
 * the authentication given a JAAS configuration entry name, authentication materials,
 * and an optional partial subject. The sole purpose of this service is to provide authentication
 * and the authenticated subject is NOT placed on the thread.
 */
public interface AuthenticationService {

    /**
     * Returns the authenticated subject.
     *
     * @param jaasEntryName the optional JAAS configuration entry name. The system.DEFAULT JAAS entry name will be used if null or empty String is passed.
     * @param the callback handler.
     * @param subject the optional partial subject to use during authentication.
     *
     * @return the authenticated subject
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    public Subject authenticate(String jaasEntryName, CallbackHandler callbackHandler, Subject subject) throws AuthenticationException;

    /**
     * Returns the authenticated subject.
     *
     * @param jaasEntryName the optional JAAS configuration entry name. The system.DEFAULT JAAS entry name will be used if null or empty String is passed.
     * @param authenticationData the authentication materials.
     * @param subject the optional partial subject to use during authentication.
     *
     * @return the authenticated subject
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    public Subject authenticate(String jaasEntryName, AuthenticationData authenticationData, Subject subject) throws AuthenticationException;

    /**
     * Returns the authenticated subject from the authentication based on the contents of the partial subject.
     *
     * @param jaasEntryName the optional JAAS configuration entry name. The system.DEFAULT JAAS entry name will be used if null or empty String is passed.
     * @param inputSubject the optional partial subject that may contain the hashtable to use during authentication.
     *
     * @return the authenticated subject
     * @throws com.ibm.ws.security.authentication.AuthenticationException
     */
    public Subject authenticate(String jaasEntryName, Subject inputSubject) throws AuthenticationException;

    /**
     * Gets the delegation subject based on the currently configured delegation provider
     * or the MethodDelegationProvider if one is not configured.
     *
     * @param roleName the name of the role, used to look up the corresponding user.
     * @param appName the name of the application, used to look up the corresponding user.
     * @return subject a subject representing the user that is mapped to the given run-as role.
     * @throws IllegalArgumentException
     */
    public Subject delegate(String roleName, String appName);

    /**
     * Returns the allow hashtable login with ID only.
     *
     * @return
     */
    public Boolean isAllowHashTableLoginWithIdOnly();

    /**
     * Get the authentication cache service
     *
     * @return
     */
    public AuthCacheService getAuthCacheService();

    /**
     * Get the identity of the unauthenticated user
     *
     * @return
     *
     */
    public String getInvalidDelegationUser();
}
