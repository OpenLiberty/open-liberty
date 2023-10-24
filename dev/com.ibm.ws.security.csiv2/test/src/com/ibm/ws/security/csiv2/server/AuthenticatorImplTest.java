/*
 * Copyright (c) 2014,2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.ws.security.csiv2.server;

import static org.junit.Assert.assertNotNull;

import java.security.cert.X509Certificate;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.WSAuthenticationData;
import com.ibm.ws.security.csiv2.Authenticator;

public class AuthenticatorImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private AuthenticationService authenticationService;
    private final Subject authenticatedSubject = new Subject();
    private final String jaasEntryName = "system.RMI_INBOUND";
    private final String testUser = "user1";
    private final String testPwd = "user1pwd";
    private X509Certificate[] certificateChain;
    private final byte[] ltpaTokenBytes = "Some token bytes".getBytes();

    @Before
    public void setUp() {
        authenticationService = mockery.mock(AuthenticationService.class);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void authenticateWithBasic() throws Exception {
        AuthenticationData authenticationData = createAuthenticationData(testUser, testPwd);
        setAuthenticationServiceExpectations(authenticationData);
        Authenticator authenticator = new AuthenticatorImpl(authenticationService);
        Subject subject = authenticator.authenticate("user1", "user1pwd");
        assertNotNull("There must be an authenticated subject.", subject);
    }

    @Test
    public void authenticateWithCertChain() throws Exception {
        AuthenticationData authenticationData = createAuthenticationData(certificateChain);
        setAuthenticationServiceExpectations(authenticationData);
        Authenticator authenticator = new AuthenticatorImpl(authenticationService);
        Subject subject = authenticator.authenticate(certificateChain);
        assertNotNull("There must be an authenticated subject.", subject);
    }

    @Test
    public void authenticateWithAssertedUser() throws Exception {
        setInternalAssertionExpectations();
        Authenticator authenticator = new AuthenticatorImpl(authenticationService);
        Subject subject = authenticator.authenticate(testUser);
        assertNotNull("There must be an authenticated subject.", subject);
    }

    @Test
    public void authenticateWithLtpaToken() throws Exception {
        AuthenticationData authenticationData = createAuthenticationData(ltpaTokenBytes);
        setAuthenticationServiceExpectations(authenticationData);
        Authenticator authenticator = new AuthenticatorImpl(authenticationService);
        Subject subject = authenticator.authenticate(ltpaTokenBytes);
        assertNotNull("There must be an authenticated subject.", subject);
    }

    private AuthenticationData createAuthenticationData(String username, @Sensitive String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }

    private AuthenticationData createAuthenticationData(@Sensitive X509Certificate certificateChain[]) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.CERTCHAIN, certificateChain);
        return authenticationData;
    }

    private AuthenticationData createAuthenticationData(@Sensitive byte[] tokenBytes) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.TOKEN, tokenBytes);
        return authenticationData;
    }

    private void setAuthenticationServiceExpectations(final AuthenticationData authenticationData) throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticationService).authenticate(jaasEntryName, authenticationData, null);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void setInternalAssertionExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(authenticationService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(false));
                one(authenticationService).authenticate(with(jaasEntryName), with(any(Subject.class)));
                will(returnValue(authenticatedSubject));
            }
        });
    }

}
