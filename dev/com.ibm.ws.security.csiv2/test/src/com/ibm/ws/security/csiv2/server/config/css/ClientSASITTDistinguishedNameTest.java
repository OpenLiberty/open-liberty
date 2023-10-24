/*
 * Copyright (c) 2015,2023 IBM Corporation and others.
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
package com.ibm.ws.security.csiv2.server.config.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.CredentialExpiredException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
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
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.util.Util;

public class ClientSASITTDistinguishedNameTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private ClientSASITTDistinguishedName clientSASITTDistinguishedName;
    private final String distinguishedName = "CN=user1, OU=security, O=ibm, C=us";
    private WSCredential wsCredential;
    private Codec codec = new TestCodec();
    private SubjectManager subjectManager;

    @Before
    public void setUp() {
        codec = new TestCodec();
        wsCredential = mockery.mock(WSCredential.class);
        subjectManager = new SubjectManager();
        clientSASITTDistinguishedName = new ClientSASITTDistinguishedName();
    }

    @After
    public void tearDown() {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.server.config.css.ClientSASITTDistinguishedName#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTDistinguishedName.value, clientSASITTDistinguishedName.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.server.config.css.ClientSASITTDistinguishedName#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeIdentityToken() throws Exception {
        WSCredential wsCredential = new WSCredentialImpl("realmName", "user1", distinguishedName, "unauthenticated", "group", "user1AccessId", null, null);
        subjectManager.setInvocationSubject(createAuthenticatedSubject(wsCredential));

        IdentityToken identityToken = clientSASITTDistinguishedName.encodeIdentityToken(codec);
        String decodedDN = Util.decodeDN(codec, identityToken.dn());

        assertEquals("The DN must be encoded and decoded.", distinguishedName, decodedDN);
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.server.config.css.ClientSASITTDistinguishedName#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeIdentityTokenWithInvalidWSCredential_NOPERM() throws Exception {
        createInvalidWSCredentialExpectations(Expectations.throwException(new CredentialExpiredException("The credential is expired.")));
        subjectManager.setInvocationSubject(createAuthenticatedSubject(wsCredential));

        try {
            clientSASITTDistinguishedName.encodeIdentityToken(codec);
            fail("The encodeIdentityToken method must fail with a NO_PERMISSION.");
        } catch (NO_PERMISSION noPerm) {
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9640E: The client cannot create the ITTDistinguishedName identity assertion token for distinguished name null. The exception message is: The credential is expired.",
                         noPerm.getMessage());
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.server.config.css.ClientSASITTDistinguishedName#encodeIdentityToken(org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeIdentityTokenWithInvalidWSCredentialNoDN_NOPERM() throws Exception {
        createInvalidWSCredentialExpectations(Expectations.returnValue(null));
        subjectManager.setInvocationSubject(createAuthenticatedSubject(wsCredential));

        try {
            clientSASITTDistinguishedName.encodeIdentityToken(codec);
            fail("The encodeIdentityToken method must fail with a NO_PERMISSION.");
        } catch (NO_PERMISSION noPerm) {
            assertTrue("The NO_PERMISSION message must be set.", noPerm.getMessage().startsWith("CWWKS9640E"));
        }
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.server.config.css.ClientSASITTDistinguishedName#toString()}.
     */
    @Test
    public void testToString() {
        assertTrue("The toString method must write the class name.", clientSASITTDistinguishedName.toString().contains("ClientSASITTDistinguishedName"));
    }

    private Subject createAuthenticatedSubject(WSCredential wsCredential) {
        final Subject authenticatedSubject = new Subject();
        Set<Object> publicCredentials = authenticatedSubject.getPublicCredentials();
        publicCredentials.add(wsCredential);
        return authenticatedSubject;
    }

    private void createInvalidWSCredentialExpectations(final Action action) throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(wsCredential).getUniqueSecurityName();
                will(action);
            }
        });
    }

}
