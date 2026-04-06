/*
 * Copyright 2015, 2026 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.security.auth.CredentialDestroyedException;
import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.Constants;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;

public class TSSITTX509CertChainTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TSSITTX509CertChain tssITTX509CertChain;
    private Subject authenticatedSubject;
    private Authenticator authenticator;
    private Codec codec = new TestCodec();
    private X509Certificate certificate;
    private X509Certificate[] certificateChain;
    private WSCredential wsCredential;

    private final String base64TestCertificateString = "-----BEGIN CERTIFICATE-----\n" +
                                                       "MIICmjCCAligAwIBAgIETyFM+jALBgcqhkjOOAQDBQAwLzELMAkGA1UEBhMCdXMxDDAKBgNVBAoT\n" +
                                                       "A2libTESMBAGA1UEAxMJbG9jYWxob3N0MCAXDTEyMDEyNjEyNTQxOFoYDzIwOTkwMTA0MTI1NDE4\n" +
                                                       "WjAvMQswCQYDVQQGEwJ1czEMMAoGA1UEChMDaWJtMRIwEAYDVQQDEwlsb2NhbGhvc3QwggG4MIIB\n" +
                                                       "LAYHKoZIzjgEATCCAR8CgYEA/X9TgR11EilS30qcLuzk5/YRt1I870QAwx4/gLZRJmlFXUAiUftZ\n" +
                                                       "PY1Y+r/F9bow9subVWzXgTuAHTRv8mZgt2uZUKWkn5/oBHsQIsJPu6nX/rfGG/g7V+fGqKYVDwT7\n" +
                                                       "g/bTxR7DAjVUE1oWkTL2dfOuK2HXKu/yIgMZndFIAccCFQCXYFCPFSMLzLKSuYKi64QL8Fgc9QKB\n" +
                                                       "gQD34aCF1ps93su8q1w2uFe5eZSvu/o66oL5V0wLPQeCZ1FZV4661FlP5nEHEIGAtEkWcSPoTCgW\n" +
                                                       "E7fPCTKMyKbhPBZ6i1R8jSjgo64eK7OmdZFuo38L+iE1YvH7YnoBJDvMpPG+qFGQiaiD3+Fa5Z8G\n" +
                                                       "kotmXoB7VSVkAUw7/s9JKgOBhQACgYEA82RoYhzAlwU8f1pyc9gyGkvZenKZ6xo3J0Hnbsxy1OhM\n" +
                                                       "OXEQ4ZTUQuw4x2KVkM+5wmEiwRWYFmdbf0UvVPym4uhxbKJwiuJ8HodH2gCaFx+KtXz1ACUn3brU\n" +
                                                       "B3/a6xcyOKoZTk+Bdyx2h+A2Zpl6TjZiw+BesmgZ4sUuEVYPpBcwCwYHKoZIzjgEAwUAAy8AMCwC\n" +
                                                       "FDN9V5D4e/zjTJu9fz7mlU3ovvqqAhReOrnh0dxFrq6rrMDXutBtVg9vBQ==\n" +
                                                       "-----END CERTIFICATE-----";

    @Before
    public void setUp() throws Exception {
        codec = new TestCodec();
        authenticator = mockery.mock(Authenticator.class);
        wsCredential = mockery.mock(WSCredential.class);
        authenticatedSubject = createAuthenticatedSubject(wsCredential);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(base64TestCertificateString.getBytes()));
        certificateChain = new X509Certificate[] { certificate };
        tssITTX509CertChain = new TSSITTX509CertChain(authenticator);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTX509CertChain.value, tssITTX509CertChain.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain#getOID()}.
     */
    @Test
    public void testGetOID() {
        assertEquals("The token OID must be empty.", "", tssITTX509CertChain.getOID());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        createAuthenticatorExpectations();
        createCredentialExpectations();
        IdentityToken identityToken = createX509CertChainIdentityToken();

        Subject assertionSubject = tssITTX509CertChain.check(identityToken, codec);

        assertEquals("There must be an authenticated subject.", authenticatedSubject, assertionSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithAuthenticationExceptionThrowsException() throws Exception {
        createAuthenticatorExpectationsThrowsAuthenticationException();
        IdentityToken identityToken = createX509CertChainIdentityToken();

        try {
            tssITTX509CertChain.check(identityToken, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTX509CertChain#toString(java.lang.String, java.lang.StringBuilder)}.
     */
    @Test
    public void testToStringStringStringBuilder() {
        StringBuilder sb = new StringBuilder();
        tssITTX509CertChain.toString("", sb);
        assertTrue("The toString method must write the class name.", sb.toString().contains("TSSITTX509CertChain"));
    }

    private Subject createAuthenticatedSubject(WSCredential wsCredential) {
        final Subject authenticatedSubject = new Subject();
        Set<Object> privateCredentials = authenticatedSubject.getPublicCredentials();
        privateCredentials.add(wsCredential);
        return authenticatedSubject;
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

    private IdentityToken createX509CertChainIdentityToken() {
        byte[] encodedCertChain = Util.encodeCertChain(codec, certificateChain);
        IdentityToken identityToken = new IdentityToken();
        identityToken.certificate_chain(encodedCertChain);
        return identityToken;
    }

    private void assertSASException(Exception e) {
        assertTrue("The exception thrown must be a SASException.", e instanceof SASException);
        SASException sasException = (SASException) e;
        assertEquals("The major code must be set.", 1, sasException.getMajor());
        assertEquals("The minor code must be set.", 1, sasException.getMinor());
        assertTrue("The cause exception must be a NO_PERMISSION exception.", sasException.getCause() instanceof NO_PERMISSION);
    }

}
