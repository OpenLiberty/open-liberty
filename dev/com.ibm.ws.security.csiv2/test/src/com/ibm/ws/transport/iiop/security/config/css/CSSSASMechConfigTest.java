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

import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class CSSSASMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private CSSSASMechConfig sasMechConfig;
    private CSSSASIdentityToken identityTokenAnonymous;
    private CSSSASIdentityToken identityTokenPrincipalName;
    private CSSSASIdentityToken identityTokenDistinguishedName;
    private final String trustedIdentity = "trustedIdentity";
    private final String trustedPassword = "trustedPassword";
    private IdentityToken encodedIdentityToken;
    private TSSSASMechConfig tssSASMechConfig;
    private SubjectManager subjectManager;
    private Codec codec;

    @Before
    public void setUp() throws Exception {
        identityTokenAnonymous = createCSSSASIdentityToken(ITTAnonymous.value);
        identityTokenPrincipalName = createCSSSASIdentityToken(ITTPrincipalName.value);
        identityTokenDistinguishedName = createCSSSASIdentityToken(ITTDistinguishedName.value);
        tssSASMechConfig = mockery.mock(TSSSASMechConfig.class);
        encodedIdentityToken = new IdentityToken();
        codec = mockery.mock(Codec.class);
        sasMechConfig = new CSSSASMechConfig();
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        subjectManager = new SubjectManager();
    }

    private CSSSASIdentityToken createCSSSASIdentityToken(final int type) {
        final CSSSASIdentityToken cssSASIdentityToken = mockery.mock(CSSSASIdentityToken.class, Integer.toString(type));
        mockery.checking(new Expectations() {
            {
                allowing(cssSASIdentityToken).getType();
                will(returnValue(type));
            }
        });
        return cssSASIdentityToken;
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    @Test
    public void testEncodeIdentityTokenAbsent() {
        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertTrue("There must be an identity token of type ITTAbsent.", identityToken.absent());
    }

    @Test
    public void testEncodeIdentityTokenAnonymous() {
        createTSSSASMechConfigExpectations(ITTAnonymous.value);
        createCSSSASIdentityTokenExpectations(identityTokenAnonymous);
        sasMechConfig.addIdentityToken(identityTokenAnonymous);

        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertEquals("There must be an identity token.", encodedIdentityToken, identityToken);
    }

    @Test
    public void testEncodeIdentityTokenAnonymous_NOPERM() throws Exception {
        createTSSSASMechConfigExpectations(ITTPrincipalName.value);
        sasMechConfig.addIdentityToken(identityTokenAnonymous);

        try {
            sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);
            fail("The encodeIdentityToken must throw a CORBA.NO_PERMISSION exception.");
        } catch (NO_PERMISSION noPerm) {
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9545E: The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of the remote server.",
                         noPerm.getMessage());
        }
    }

    @Test
    public void testEncodeIdentityTokenAnonymousClientDoesNotSupport_NOPERM() throws Exception {
        createTSSSASMechConfigExpectations(ITTPrincipalName.value);
        sasMechConfig.addIdentityToken(identityTokenPrincipalName);

        try {
            sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);
            fail("The encodeIdentityToken must throw a CORBA.NO_PERMISSION exception.");
        } catch (NO_PERMISSION noPerm) {
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9546E: The client cannot create the ITTAnonymous identity assertion token because it is not supported by the configuration of this client.",
                         noPerm.getMessage());
        }
    }

    @Test
    public void testEncodeIdentityToken_ITTPrincipalName() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTPrincipalName.value | ITTDistinguishedName.value);
        createCSSSASIdentityTokenExpectations(identityTokenPrincipalName);
        sasMechConfig.addIdentityToken(identityTokenPrincipalName);

        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertEquals("There must be an identity token.", encodedIdentityToken, identityToken);
    }

    @Test
    public void testEncodeIdentityToken_ITTPrincipalName_NOPERM() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTDistinguishedName.value);
        sasMechConfig.addIdentityToken(identityTokenPrincipalName);

        try {
            sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);
            fail("The encodeIdentityToken must throw a CORBA.NO_PERMISSION exception.");
        } catch (NO_PERMISSION noPerm) {
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9548E: The client cannot assert an authenticated subject because the configuration of the remote server does not support identity assertions with types <ITTPrincipalName>.",
                         noPerm.getMessage());
        }
    }

    @Test
    public void testEncodeIdentityToken_ITTDistinguishedName() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTPrincipalName.value | ITTDistinguishedName.value);
        createCSSSASIdentityTokenExpectations(identityTokenDistinguishedName);
        sasMechConfig.addIdentityToken(identityTokenDistinguishedName);

        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertEquals("There must be an identity token.", encodedIdentityToken, identityToken);
    }

    @Test
    public void testEncodeIdentityToken_ITTDistinguishedName_NOPERM() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTPrincipalName.value);
        sasMechConfig.addIdentityToken(identityTokenDistinguishedName);

        try {
            sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);
            fail("The encodeIdentityToken must throw a CORBA.NO_PERMISSION exception.");
        } catch (NO_PERMISSION noPerm) {
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9548E: The client cannot assert an authenticated subject because the configuration of the remote server does not support identity assertions with types <ITTDistinguishedName>.",
                         noPerm.getMessage());
        }
    }

    @Test
    public void testEncodeIdentityToken_ITTDistinguishedNameMustBeChosen() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTPrincipalName.value | ITTDistinguishedName.value);
        createCSSSASIdentityTokenExpectations(identityTokenDistinguishedName);
        sasMechConfig.addIdentityToken(identityTokenDistinguishedName);
        sasMechConfig.addIdentityToken(identityTokenPrincipalName);

        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertEquals("There must be an identity token.", encodedIdentityToken, identityToken);
    }

    @Test
    public void testEncodeIdentityToken_ITTPrincipalNameMustBeChosen() throws Exception {
        subjectManager.setInvocationSubject(createSubject());
        createTSSSASMechConfigExpectations(ITTPrincipalName.value | ITTDistinguishedName.value);
        createCSSSASIdentityTokenExpectations(identityTokenPrincipalName);
        sasMechConfig.addIdentityToken(identityTokenPrincipalName);
        sasMechConfig.addIdentityToken(identityTokenDistinguishedName);

        IdentityToken identityToken = sasMechConfig.encodeIdentityToken(tssSASMechConfig, codec);

        assertEquals("There must be an identity token.", encodedIdentityToken, identityToken);
    }

    private void createTSSSASMechConfigExpectations(final int types) {
        mockery.checking(new Expectations() {
            {
                one(tssSASMechConfig).getSupportedIdentityTypes();
                will(returnValue(types));
            }
        });
    }

    private void createCSSSASIdentityTokenExpectations(final CSSSASIdentityToken cssSASIdentityToken) {
        mockery.checking(new Expectations() {
            {
                one(cssSASIdentityToken).encodeIdentityToken(codec);
                will(returnValue(encodedIdentityToken));
            }
        });
    }

    private Subject createSubject() {
        final Subject authenticatedSubject = new Subject();
        WSCredential wsCredential = new WSCredentialImpl("realmName", "user1", "user1UniqueSecurityName", "unauthenticated", "group", "user1AccessId", null, null);
        Set<Object> publicCredentials = authenticatedSubject.getPublicCredentials();
        publicCredentials.add(wsCredential);
        return authenticatedSubject;
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig#toString()}.
     */
    @Test
    public void testToString() {
        assertNotNull("The CSSSASMechConfig must override toString().",
                      sasMechConfig.toString().startsWith("CSSSASMechConfig: ["));
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig#getTrustedIdentity()}.
     */
    @Test
    public void testGetTrustedIdentity() {
        sasMechConfig.setTrustedIdentity(trustedIdentity);
        assertEquals("The trusted identity must be set.", trustedIdentity, sasMechConfig.getTrustedIdentity());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig#getTrustedPassword()}.
     */
    @Test
    public void testGetTrustedPassword() {
        sasMechConfig.setTrustedPassword(new SerializableProtectedString(trustedPassword.toCharArray()));
        assertEquals("The trusted password must be set.", trustedPassword, new String(sasMechConfig.getTrustedPassword().getChars()));
    }

}
