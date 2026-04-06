/*
 * Copyright 2014, 2026 IBM Corporation and others.
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
package com.ibm.ws.transport.iiop.security.config.tss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CSIIOP.TransportAddress;

import test.common.SharedOutputManager;

import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.Constants;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

public class TSSSSLTransportConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private Authenticator authenticator;
    private SSLSession sslSession;
    private X509Certificate certificate;
    private X509Certificate[] certificateChain;
    private final String[] supportedOptions = new String[] { "Integrity" };
    private final String[] requiredOptions = new String[] { "EstablishTrustInClient" };
    private final List<TransportAddress> addresses = new ArrayList<TransportAddress>();
    private Subject authenticatedSubject;
    private TSSSSLTransportConfig sslTransportConfig;
    private TrustedIDEvaluator trustedIDEvaluator;
    private WSCredential wsCredential;

    private final SSLPeerUnverifiedException spue = new SSLPeerUnverifiedException("The peer cannot be verified.");

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    @Before
    public void setUp() throws Exception {
        // Set mock objects for each test to avoid intermittent defects if tests run in a different order.
        authenticator = mockery.mock(Authenticator.class);
        certificate = mockery.mock(X509Certificate.class);
        certificateChain = new X509Certificate[] { certificate };
        sslSession = mockery.mock(SSLSession.class);
        trustedIDEvaluator = mockery.mock(TrustedIDEvaluator.class);
        wsCredential = mockery.mock(WSCredential.class);
        authenticatedSubject = createAuthenticatedSubject(wsCredential);

        sslTransportConfig = new TSSSSLTransportConfig(authenticator);
        sslTransportConfig.setSupports(TSSConfigHelper.extractAssociationOptions(supportedOptions));
        sslTransportConfig.setRequires(TSSConfigHelper.extractAssociationOptions(requiredOptions));
        sslTransportConfig.setTransportAddresses(addresses);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    @Test
    public void authenticateWithCertificateChain() throws Exception {
        createSSLSessionPeerCertificatesExpectations();
        createAuthenticatorExpectations();
        createCredentialExpectations();

        Subject transportSubject = sslTransportConfig.check(sslSession);

        assertEquals("There must be a transport subject.", authenticatedSubject, transportSubject);
    }

    @Test
    public void throwSASExceptionForUnverifiedPeerWhenClientCertAuthRequired() throws Exception {
        createSSLSessionThrowsSSLPeerUnverifiedException();

        try {
            sslTransportConfig.check(sslSession);
            fail("A SASException must be thrown for an SSLPeerUnverifiedException when client certificate authentication is required.");
        } catch (SASException e) {
            assertEquals("The SASException major must be set to 1.", 1, e.getMajor());
            assertTrue("The SASException cause must be set.", e.getCause() instanceof org.omg.CORBA.NO_PERMISSION);
        }
    }

    @Test
    public void nullSubjectForUnverifiedPeerWhenSSLNotRequired() throws Exception {
        createSSLSessionThrowsSSLPeerUnverifiedException();
        sslTransportConfig = new TSSSSLTransportConfig(authenticator);
        sslTransportConfig.setTransportAddresses(addresses);

        Subject transportSubject = sslTransportConfig.check(sslSession);

        assertEquals("There must not be a subject for SSLPeerUnverifiedException when client certificate authentication is not required.",
                     null, transportSubject);
    }

    @Test
    public void noPermissionForNullSSLSessionWhenSSLRequired() throws Exception {
        try {
            sslTransportConfig.check(null);
            fail("A SASException with a NO_PERMISSION must be thrown for a null SSLSession when SSL is required.");
        } catch (SASException e) {
            // TODO: Determine what message is expected here.
            NO_PERMISSION np = (NO_PERMISSION) e.getCause();
            assertNotNull("The NO_PERMISSION exception must have a message.", np.getMessage());
        }
    }

    @Test
    public void nullSubjectForNullSSLSessionWhenSSLNotRequired() throws Exception {
        sslTransportConfig = new TSSSSLTransportConfig(authenticator);
        sslTransportConfig.setTransportAddresses(addresses);

        Subject transportSubject = sslTransportConfig.check(null);

        assertEquals("There must not be a subject for a null SSLSession when SSL is not required.",
                     null, transportSubject);
    }

    @Test
    public void throwSASExceptionForAuthenticationExceptionWhenClientCertAuthRequired() throws Exception {
        createSSLSessionPeerCertificatesExpectations();
        createAuthenticatorExpectationsThrowsAuthenticationException();

        try {
            sslTransportConfig.check(sslSession);
            fail("A SASException must be thrown for an AuthenticationException when client certificate authentication is required.");
        } catch (SASException e) {
            assertEquals("The SASException major must be set to 1.", 1, e.getMajor());
        }
    }

    private Subject createAuthenticatedSubject(WSCredential wsCredential) {
        final Subject authenticatedSubject = new Subject();
        Set<Object> privateCredentials = authenticatedSubject.getPublicCredentials();
        privateCredentials.add(wsCredential);
        return authenticatedSubject;
    }

    @Test
    public void nullSubjectForAuthenticationExceptionWhenClientCertAuthNotRequired() throws Exception {
        createSSLSessionPeerCertificatesExpectations();
        createAuthenticatorExpectationsThrowsAuthenticationException();
        sslTransportConfig = new TSSSSLTransportConfig(authenticator);
        sslTransportConfig.setTransportAddresses(addresses);

        Subject transportSubject = sslTransportConfig.check(sslSession);

        assertEquals("There must not be a subject for an AuthenticationException when client certificate authentication is not required.",
                     null, transportSubject);
    }

    private void createSSLSessionPeerCertificatesExpectations() throws SSLPeerUnverifiedException {
        mockery.checking(new Expectations() {
            {
                one(sslSession).getPeerCertificates();
                will(returnValue(certificateChain));
            }
        });
    }

    private void createSSLSessionThrowsSSLPeerUnverifiedException() throws SSLPeerUnverifiedException {
        mockery.checking(new Expectations() {
            {
                one(sslSession).getPeerCertificates();
                will(throwException(spue));
            }
        });
    }

    private void createAuthenticatorExpectations() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(certificateChain);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createAuthenticatorExpectationsThrowsAuthenticationException() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(certificateChain);
                will(throwException(new AuthenticationException("There was a problem authenticating.")));
            }
        });
    }

    private void createCredentialExpectations() throws CredentialDestroyedException, CredentialExpiredException {
        mockery.checking(new Expectations() {
            {
                one(wsCredential).set(Constants.IDENTITY_NAME, Constants.ClientCertificate);
                one(wsCredential).set(Constants.IDENTITY_VALUE, certificateChain);
            }
        });
    }

    @Test
    public void testIsTrusted() throws Exception {
        createSSLSessionPeerCertificatesExpectations();
        createTrustedIDEvaluatorExpectations();
        assertTrue("The trust validation must return true.",
                   sslTransportConfig.isTrusted(trustedIDEvaluator, sslSession));
    }

    @Test
    public void testIsTrustedForNullSSLSession() {
        assertFalse("The trust validation must return false when there is no SSL session.",
                    sslTransportConfig.isTrusted(trustedIDEvaluator, null));
    }

    @Test
    public void testIsTrustedForUnverifiedPeer() throws Exception {
        createSSLSessionThrowsSSLPeerUnverifiedException();
        assertFalse("The trust validation must return false for SSLPeerUnverifiedException.",
                    sslTransportConfig.isTrusted(trustedIDEvaluator, sslSession));
    }

    private void createTrustedIDEvaluatorExpectations() {
        mockery.checking(new Expectations() {
            {
                one(trustedIDEvaluator).isTrusted(certificateChain);
                will(returnValue(true));
            }
        });
    }

}
