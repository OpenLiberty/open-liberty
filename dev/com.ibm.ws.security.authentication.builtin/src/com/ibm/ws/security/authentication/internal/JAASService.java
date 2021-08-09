/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import com.ibm.ws.security.authentication.AuthenticationData;

/**
 *
 */
public interface JAASService {

    /**
     * Performs a JAAS login.
     * 
     * @param jaasEntryName
     * @param authenticationData
     * @param partialSubject
     * @return the authenticated subject.
     * @throws javax.security.auth.login.LoginException
     */
    public abstract Subject performLogin(String jaasEntryName, AuthenticationData authenticationData, Subject partialSubject) throws LoginException;

    /**
     * Performs a JAAS login.
     * 
     * @param jaasEntryName
     * @param callbackHandler
     * @param partialSubject
     * @return the authenticated subject.
     * @throws javax.security.auth.login.LoginException
     */
    public abstract Subject performLogin(String jaasEntryName, CallbackHandler callbackHandler, Subject partialSubject) throws LoginException;

}