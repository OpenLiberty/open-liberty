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
package com.ibm.ws.transport.iiop.security.config.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;

import com.ibm.ws.security.authentication.principals.WSPrincipal;
import com.ibm.ws.security.context.SubjectManager;

public class CSSSASITTPrincipalNameDynamicTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private CSSSASITTPrincipalNameDynamic cssSASITTPrincipalNameDynamic;
    private final String domain = "testRealm";
    private SubjectManager subjectManager;
    private Codec codec;
    private WSPrincipal principal;
    private final String securityName = "user1";
    // It does not matter what bytes, we are testing that the codec is used to encode them.
    private final byte[] principalNameEncoding = "Encoding for principal name".getBytes();

    @Before
    public void setUp() {
        cssSASITTPrincipalNameDynamic = new CSSSASITTPrincipalNameDynamic(null, domain);
        subjectManager = new SubjectManager();
        codec = mockery.mock(Codec.class);
        principal = new WSPrincipal(securityName, "userAccessID", WSPrincipal.AUTH_METHOD_PASSWORD);
    }

    @After
    public void tearDown() {
        subjectManager.clearSubjects();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeIdentityToken() throws Exception {
        createEncodingExpectations();
        subjectManager.setInvocationSubject(createAuthenticatedSubject(principal));
        IdentityToken identityToken = cssSASITTPrincipalNameDynamic.encodeIdentityToken(codec);

        assertEquals("There must be an identity token with the principal name encoding.",
                     principalNameEncoding, identityToken.principal_name());
    }

    @Test
    public void testEncodeIdentityTokenUsingCallerSubject() throws Exception {
        createEncodingExpectations();
        subjectManager.setCallerSubject(createAuthenticatedSubject(principal));
        IdentityToken identityToken = cssSASITTPrincipalNameDynamic.encodeIdentityToken(codec);

        assertEquals("There must be an identity token with the principal name encoding.",
                     principalNameEncoding, identityToken.principal_name());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test(expected = IllegalStateException.class)
    public void testEncodeIdentityTokenWithEncodingException() throws Exception {
        createEncodingExpectationsThrowsException();
        subjectManager.setInvocationSubject(createAuthenticatedSubject(principal));
        cssSASITTPrincipalNameDynamic.encodeIdentityToken(codec);
        fail("An IllegalStateException must be thrown for an encoding failure.");
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeIdentityTokenNoPrincipal() throws Exception {
        subjectManager.setInvocationSubject(createAuthenticatedSubject(null));
        IdentityToken identityToken = cssSASITTPrincipalNameDynamic.encodeIdentityToken(codec);

        assertTrue("The identity token must be anonymous.", identityToken.anonymous());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTPrincipalName.value, cssSASITTPrincipalNameDynamic.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic#toString()}.
     */
    @Test
    public void testToString() {
        assertNotNull("The identity token must override toString().",
                      cssSASITTPrincipalNameDynamic.toString().startsWith("CSSSASITTPrincipalNameDynamic: ["));
    }

    private Subject createAuthenticatedSubject(Principal principal) {
        final Subject authenticatedSubject = new Subject();
        if (principal != null) {
            Set<Principal> principals = authenticatedSubject.getPrincipals();
            principals.add(principal);
        }
        return authenticatedSubject;
    }

    private void createEncodingExpectations() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(codec).encode_value(with(any(org.omg.CORBA.Any.class)));
                will(returnValue(principalNameEncoding));
            }
        });
    }

    private void createEncodingExpectationsThrowsException() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(codec).encode_value(with(any(org.omg.CORBA.Any.class)));
                will(throwException(new InvalidTypeForEncoding()));
            }
        });
    }

}
