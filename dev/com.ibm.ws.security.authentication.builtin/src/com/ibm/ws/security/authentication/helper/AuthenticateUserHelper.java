/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.helper;

import java.util.Hashtable;

import javax.security.auth.Subject;

import com.ibm.ws.security.authentication.AuthenticationConstants;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;
import com.ibm.wsspi.security.token.AttributeNameConstants;

/**
 * Helper class for authenticating a user
 */
public class AuthenticateUserHelper {

    /**
     * Authenticate the given user and return an authenticated Subject.
     * 
     * @param authenticationService service to authenticate a user, must not be null
     * @param userName the user to authenticate, must not be null
     * @param jaasEntryName the optional JAAS configuration entry name. The system.DEFAULT JAAS entry name will be used if null or empty String is passed
     * @return the authenticated subject
     * @throws AuthenticationException if there was a problem authenticating the user, or if the userName or authenticationService is null
     */
    public Subject authenticateUser(AuthenticationService authenticationService, String userName,
                                    String jaasEntryName) throws AuthenticationException
    {
        return authenticateUser(authenticationService, userName, jaasEntryName, null);
    }

    /**
     * Authenticate the given user and return an authenticated Subject.
     * 
     * @param authenticationService service to authenticate a user, must not be null
     * @param userName the user to authenticate, must not be null
     * @param jaasEntryName the optional JAAS configuration entry name. The system.DEFAULT JAAS entry name will be used if null or empty String is passed
     * @param customCacheKey The custom cache key to look up the subject
     * @return the authenticated subject
     * @throws AuthenticationException if there was a problem authenticating the user, or if the userName or authenticationService is null
     */
    public Subject authenticateUser(AuthenticationService authenticationService, String userName,
                                    String jaasEntryName, String customCacheKey) throws AuthenticationException
    {
        validateInput(authenticationService, userName);
        if (jaasEntryName == null || jaasEntryName.trim().isEmpty())
            jaasEntryName = JaasLoginConfigConstants.SYSTEM_DEFAULT;

        Subject partialSubject = createPartialSubject(userName, authenticationService, customCacheKey);
        return authenticationService.authenticate(jaasEntryName, partialSubject);
    }

    /*
     * Create the partial subject that can be used to authenticate the user with.
     */
    protected Subject createPartialSubject(String username, AuthenticationService authenticationService, String customCacheKey) {
        Subject partialSubject = null;
        partialSubject = new Subject();
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_USERID, username);
        // only set the property in the hashtable if the public property is not already set;
        // this property allows authentication when only the username is supplied
        if (!authenticationService.isAllowHashTableLoginWithIdOnly()) {
            hashtable.put(AuthenticationConstants.INTERNAL_ASSERTION_KEY, Boolean.TRUE);
        }

        if (customCacheKey != null) {
            hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, customCacheKey);
        }

        partialSubject.getPublicCredentials().add(hashtable);

        return partialSubject;
    }

    /**
     * Validate that the input parameters are not null.
     * 
     * @param authenticationService the service to authenticate a user
     * @param username the user to authenticate
     * @throws AuthenticationException when either input is null
     */
    private void validateInput(AuthenticationService authenticationService, String username) throws AuthenticationException {
        if (authenticationService == null) {
            throw new AuthenticationException("authenticationService cannot be null.");
        } else if (username == null) {
            throw new AuthenticationException("username cannot be null.");
        }
    }
}
