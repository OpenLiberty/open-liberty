/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.messaging.security;

import javax.security.auth.Subject;

import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationService;
import com.ibm.ws.messaging.security.authorization.MessagingAuthorizationService;

/**
 * Interface for Messaging Security Service
 * 
 * @author Sharath Chandra B
 * 
 */
public interface MessagingSecurityService {

    /**
     * Gets the MessagingAuthenticationService
     * If this method is called when Messaging Security is disabled, returns Null.
     * When Messaging Security is enabled, it returns the MessagingAuthentication Service
     * 
     * @return
     *         MessagingAuthenticationService
     */
    public MessagingAuthenticationService getMessagingAuthenticationService();

    /**
     * Gets the MessagingAuthorizationService
     * If this method is called when Messaging Security is disabled, returns Null
     * When Messaging Security is enabled, it returns the MessagingAuthorization Service
     * 
     * @return
     *         MessagingAuthorizationService
     */
    public MessagingAuthorizationService getMessagingAuthorizationService();

    /**
     * This method returns the unique name of the user that was being
     * authenticated. This is a best can do process and a user name may not be
     * available, in which case null should be returned. This method should not
     * return an empty string.
     * 
     * @param subject
     *            the WAS authenticated subject
     * 
     * @return The name of the user being authenticated.
     * @throws MessagingSecurityException
     */
    public String getUniqueUserName(Subject subject) throws MessagingSecurityException;

    /**
     * Check if the Subject is Authenticated
     * 
     * @param subject
     * @return
     *         true if Subject is not authenticated
     */
    public boolean isUnauthenticated(Subject subject) throws Exception;

}
