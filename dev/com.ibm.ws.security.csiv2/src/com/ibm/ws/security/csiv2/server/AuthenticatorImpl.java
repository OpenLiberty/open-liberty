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
package com.ibm.ws.security.csiv2.server;

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.authentication.helper.AuthenticateUserHelper;
import com.ibm.ws.security.csiv2.Authenticator;

public class AuthenticatorImpl implements Authenticator {

    private final String jaasEntryName = "system.RMI_INBOUND";
    private final AuthenticationService authenticationService;

    public AuthenticatorImpl(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Authenticate with user name and password.
     * 
     * @param username
     * @param password
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    @Override
    public Subject authenticate(String username, @Sensitive String password) throws AuthenticationException {
        AuthenticationData authenticationData = createAuthenticationData(username, password);
        return authenticationService.authenticate(jaasEntryName, authenticationData, null);
    }

    /**
     * Authenticate with certificate chain.
     * 
     * @param certificateChain
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    @Override
    public Subject authenticate(@Sensitive X509Certificate[] certificateChain) throws AuthenticationException {
        AuthenticationData authenticationData = createAuthenticationData(certificateChain);
        return authenticationService.authenticate(jaasEntryName, authenticationData, null);
    }

    /**
     * Authenticate with asserted user.
     * 
     * @param assertedUser
     * @return The authenticated subject.
     * @throws AuthenticationException
     */
    @Override
    public Subject authenticate(String assertedUser) throws AuthenticationException {
        AuthenticateUserHelper authHelper = new AuthenticateUserHelper();
        return authHelper.authenticateUser(authenticationService, assertedUser, jaasEntryName);
    }

    /**
     * Authenticate with token bytes.
     * 
     * @param tokenBytes
     * @return The authenticated subject.
     */
    @Override
    public Subject authenticate(@Sensitive byte[] tokenBytes) throws AuthenticationException {
        AuthenticationData authenticationData = createAuthenticationData(tokenBytes);
        return authenticationService.authenticate(jaasEntryName, authenticationData, null);
    }

    @Trivial
    protected AuthenticationData createAuthenticationData(String username, @Sensitive String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }

    @Trivial
    private AuthenticationData createAuthenticationData(@Sensitive X509Certificate certificateChain[]) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.CERTCHAIN, certificateChain);
        return authenticationData;
    }

    @Trivial
    private AuthenticationData createAuthenticationData(@Sensitive byte[] tokenBytes) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.TOKEN, tokenBytes);
        return authenticationData;
    }

}
