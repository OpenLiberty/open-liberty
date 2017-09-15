/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.csiv2;

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.AuthenticationException;

public interface Authenticator {

    /**
     * Authenticate with user name and password.
     * 
     * @param username
     * @param password
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    Subject authenticate(String username, @Sensitive String password) throws AuthenticationException;

    /**
     * Authenticate with certificate chain.
     * 
     * @param certificateChain
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    Subject authenticate(@Sensitive X509Certificate[] certificateChain) throws AuthenticationException;

    /**
     * Authenticate with asserted user.
     * 
     * @param assertedUser
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    Subject authenticate(String assertedUser) throws AuthenticationException;

    /**
     * Authenticate with token bytes.
     * 
     * @param tokenBytes
     * @return The authenticated subject.
     */
    Subject authenticate(@Sensitive byte[] tokenBytes) throws AuthenticationException;
}
