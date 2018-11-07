/*******************************************************************************
 * Copyright (c) 2014, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
/**
 * 04/26/10 F743-25523     leou      Initial version
 * 05/10/10 F743-25523.1   leou      Move Jaspi hooks to WebAuthenticator
 * 05/27/10 654357         leou      CTS6: jaspic failure - testName:  CheckValidateReqAuthException, do not call secureResponse during postInvoke
 * 08/11/10 665302         leou      Authorization problem with cache key using JASPI authentication
 */
package com.ibm.ws.webcontainer.security;

import java.util.Hashtable;

import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibm.ws.security.authentication.AuthenticationException;

public interface JaspiService extends WebAuthenticator {

    void postInvoke(WebSecurityContext webSecurityContext) throws AuthenticationException;

    Hashtable<String, Object> getCustomCredentials(Subject subject);

    Subject getUnauthenticatedSubject();

    interface JaspiAuthContext {
        Object getServerAuthContext();

        Object getMessageInfo();

        boolean runSecureResponse();

        void setRunSecureResponse(boolean isSet);
    }

    /**
     * Invoke the matching JASPI provider's cleanSubject method.
     * Throw an AuthenticationException if cleanSubject throws an
     * exception.
     *
     * @param req
     * @param res
     * @param webAppSecConfig
     * @throws AuthenticationException
     */
    void logout(HttpServletRequest req,
                HttpServletResponse res,
                WebAppSecurityConfig webAppSecConfig) throws AuthenticationException;

    /**
     * Returns true if any providers are registered
     */
    boolean isAnyProviderRegistered(WebRequest webRequest);

    /**
     * returns true if JSR-375 HttpAuthenticationMechansim is avaiable and newAuthentication attribute is set as true in AuthenticationParameters.
     * otherwise return false;
     */
    boolean isProcessingNewAuthentication(HttpServletRequest req);

    /**
     * returns true if JSR-375 HttpAuthenticationMechansim is avaiable and credential object is set in AuthenticationParameters.
     * otherwise return false;
     */
    boolean isCredentialPresent(HttpServletRequest req);
}
