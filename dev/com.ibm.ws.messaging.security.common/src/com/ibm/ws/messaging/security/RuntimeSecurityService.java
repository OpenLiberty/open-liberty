/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
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

import com.ibm.ws.messaging.security.authentication.MessagingAuthenticationException;

/**
 *
 */

public interface RuntimeSecurityService {

    /**
     * Check if Messaging Security is enabled or not.
     * This is determined by the existence of the MessagingSecurityService.
     * If MessagingSecurity Service is not null, MessagingSecurity is enabled
     * 
     * @return
     *         true : If MessasingSecurity is enabled
     *         false: If MessagingSecurity is disabled
     */
    boolean isMessagingSecure();

    /**
     * Create a Unauthenticated Subject
     * When Messaging Security is disabled, we will create a blank subject
     * This should be called only when Messaging Security is disabled
     * 
     * @return
     *         Subject: A Unauthenticated Subject
     */
    Subject createUnauthenticatedSubject();

    /**
     * Get Proxy Authentication instance
     * 
     * @return
     *         Authentication
     */
    Authentication getAuthenticationInstance();

    /**
     * Get Proxy Authorization instance
     * 
     * @return
     */
    Authorization getAuthorizationInstance();

    /**
     * Get Unique User name for the Subject
     * 
     * @param subject
     * @return
     * @throws MessagingSecurityException
     */
    String getUniqueUserName(Subject subject) throws MessagingSecurityException;

    /**
     * Check if the subject passed is Authenticated
     * <ul>
     * <li> If Messaging Security is disabled, it returns false
     * <li> If Messaging Security is enabled, it calls MessagingAuthenticationService
     * from Messaging Security component </li>
     * 
     * @param subject
     *            Subject which need to be checked
     * @return
     *         true : If the Subject is unauthenticated
     *         false: If the Subject is authenticated
     */
    boolean isUnauthenticated(Subject subject) throws MessagingAuthenticationException;

}