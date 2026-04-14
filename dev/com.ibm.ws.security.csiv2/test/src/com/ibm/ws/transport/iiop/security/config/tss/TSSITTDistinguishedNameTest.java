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

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;

public class TSSITTDistinguishedNameTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TSSITTDistinguishedName tssITTDistinguishedName;
    private final String distinguishedName = "CN=user1, OU=security, O=ibm, C=us";
    private final Subject authenticatedSubject = new Subject();
    private Authenticator authenticator;
    private Codec codec = new TestCodec();

    @Before
    public void setUp() {
        codec = new TestCodec();
        authenticator = mockery.mock(Authenticator.class);
        tssITTDistinguishedName = new TSSITTDistinguishedName(authenticator);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTDistinguishedName.value, tssITTDistinguishedName.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName#getOID()}.
     */
    @Test
    public void testGetOID() {
        assertEquals("The token OID must be empty.", "", tssITTDistinguishedName.getOID());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        createAuthenticatorExpectations(new WSCredentialImpl("realmName", "user1", "user1UniqueSecurityName", "unauthenticated", "group", "user1AccessId", null, null));
        IdentityToken identityToken = createDistinguishedNameIdentityToken();

        Subject assertionSubject = tssITTDistinguishedName.check(identityToken, codec);

        assertEquals("There must be an authenticated subject.", authenticatedSubject, assertionSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithAuthenticationExceptionThrowsException() throws Exception {
        createAuthenticatorExpectationsThrowsAuthenticationException();
        IdentityToken identityToken = createDistinguishedNameIdentityToken();

        try {
            tssITTDistinguishedName.check(identityToken, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTDistinguishedName#toString(java.lang.String, java.lang.StringBuilder)}.
     */
    @Test
    public void testToStringStringStringBuilder() {
        StringBuilder sb = new StringBuilder();
        tssITTDistinguishedName.toString("", sb);
        assertTrue("The toString method must write the class name.", sb.toString().contains("TSSITTDistinguishedName"));
    }

    private void createAuthenticatorExpectations(WSCredential wsCredential) throws AuthenticationException {
        authenticatedSubject.getPublicCredentials().add(wsCredential);
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(distinguishedName);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createAuthenticatorExpectationsThrowsAuthenticationException() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(distinguishedName);
                will(throwException(new AuthenticationException("There was a problem authenticating.")));
            }
        });
    }

    private IdentityToken createDistinguishedNameIdentityToken() throws Exception {
        byte[] encodedDN = Util.encodeDN(codec, distinguishedName);
        IdentityToken identityToken = new IdentityToken();
        identityToken.dn(encodedDN);
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
