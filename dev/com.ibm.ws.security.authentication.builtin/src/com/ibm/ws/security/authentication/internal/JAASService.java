/*******************************************************************************
 * Copyright (c) 2011, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.CertificateAuthenticator;
import com.ibm.ws.security.authentication.collective.CollectiveAuthenticationPlugin;
import com.ibm.ws.security.credentials.CredentialsService;
import com.ibm.ws.security.registry.RegistryException;
import com.ibm.ws.security.registry.UserRegistry;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.wsspi.kernel.service.utils.ConcurrentServiceReferenceMap;

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

    /**
     * Get the {@link CollectiveAuthenticationPlugin} that have registered with this {@link JAASService}.
     *
     * @return The {@link CollectiveAuthenticationPlugin} that has registered with this {@link JAASService}.
     */
    public CollectiveAuthenticationPlugin getCollectiveAuthenticationPlugin();

    /**
     * Get the {@link UserRegistry} that have registered with this {@link JAASService}.
     *
     * @return The {@link UserRegistry} that has registered with this {@link JAASService}.
     */
    public UserRegistry getUserRegistry() throws RegistryException;

    /**
     * Get the {@link TokenManager} that have registered with this {@link JAASService}.
     *
     * @return The {@link TokenManager} that has registered with this {@link JAASService}.
     */
    public TokenManager getTokenManager();

    /**
     * Get the {@link CredentialsService} that have registered with this {@link JAASService}.
     *
     * @return The {@link CredentialsService} that has registered with this {@link JAASService}.
     */
    public CredentialsService getCredentialsService();

    /**
     * Get the {@link AuthenticationService} that have registered with this {@link JAASService}.
     *
     * @return The {@link AuthenticationService} that has registered with this {@link JAASService}.
     */
    public AuthenticationService getAuthenticationService();

    /**
     * Get the {@link ConcurrentServiceReferenceMap} of {@link CertificateAuthenticator}s that have registered with this {@link JAASService}.
     *
     * @return The {@link ConcurrentServiceReferenceMap} of {@link CertificateAuthenticator}s that have registered with this {@link JAASService}.
     */
    public ConcurrentServiceReferenceMap<String, CertificateAuthenticator> getCertificateAuthenticators();
}