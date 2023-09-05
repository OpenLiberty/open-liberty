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
package com.ibm.ws.security.csiv2.server.config.tss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.security.auth.Subject;

import org.hamcrest.Matcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.portable.InputStream;
import org.omg.CSI.EstablishContext;
import org.omg.CSIIOP.AS_ContextSec;
import org.omg.CSIIOP.EstablishTrustInClient;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.FormatMismatch;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.IOP.CodecPackage.TypeMismatch;

import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.token.TokenManager;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;
import com.ibm.wsspi.security.ltpa.Token;

public class ServerLTPAMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private final String targetName = "testRealm";
    private final String decodedLtpaOID = "1.3.18.0.2.30.2";
    private TrustedIDEvaluator trustedIDEvaluator;
    private Authenticator authenticator;
    private TokenManager tokenManager;
    private Token ltpaToken;
    private Codec codec;
    private final byte[] ltpaTokenBytes = "Some token bytes".getBytes();
    // It does not matter what bytes, we are testing that the codec is used to encode them.
    private final byte[] encodedBytes = ltpaTokenBytes;
    private final byte[] decodedLtpaTokenBytes = new byte[ltpaTokenBytes.length];
    private final String trustedId = "trustedId1";
    private final String[] accessIDs = new String[] { "user:" + targetName + "/" + trustedId };

    @Before
    public void setUp() throws Exception {
        trustedIDEvaluator = mockery.mock(TrustedIDEvaluator.class);
        authenticator = mockery.mock(Authenticator.class);
        tokenManager = mockery.mock(TokenManager.class);
        ltpaToken = mockery.mock(Token.class);
        codec = mockery.mock(Codec.class);
    }

    @Test
    public void optionsRequired() {
        boolean required = true;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, ltpaConfig.getSupports());
        assertEquals("The requires association option must be set.", EstablishTrustInClient.value, ltpaConfig.getRequires());
    }

    @Test
    public void optionsSupportedOnly() {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, ltpaConfig.getSupports());
        assertEquals("The requires association option must be set to 0.", 0, ltpaConfig.getRequires());
    }

    @Test
    public void encodeIOR() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        AS_ContextSec asContextSec = ltpaConfig.encodeIOR(codec);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, asContextSec.target_supports);
        assertEquals("The requires association option must be set to 0.", 0, asContextSec.target_requires);
        assertEquals("The authentication mechanism encoded OID must be set.", decodedLtpaOID, Util.decodeOID(asContextSec.client_authentication_mech));
        assertEquals("The authentication mechanism encoded target name must be set.",
                     targetName, Util.decodeGSSExportedName(asContextSec.target_name).getName());
    }

    @Test
    public void encodeIORRequired() throws Exception {
        boolean required = true;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        AS_ContextSec asContextSec = ltpaConfig.encodeIOR(codec);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, asContextSec.target_supports);
        assertEquals("The requires association option must be set.", EstablishTrustInClient.value, asContextSec.target_requires);
    }

    @Test
    public void check() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = createEstablishContextMessage();
        createDecodingExpectations();
        createAuthenticatorExpectations();

        Subject authenticationLayerSubject = ltpaConfig.check(msg, codec);

        assertNotNull("There must be an authentication layer subject.", authenticationLayerSubject);
    }

    @Test
    public void checkWithAuthenticationExceptionThrowsException() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = createEstablishContextMessage();
        createDecodingExpectations();
        createAuthenticatorExpectationsThrowsAuthenticationException();

        try {
            ltpaConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    @Test
    public void checkWithNullClientAuthenticationTokenReturnsNullSubject() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        Subject authenticationLayerSubject = ltpaConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    @Test
    public void checkWithEmptyClientAuthenticationTokenReturnsNullSubject() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        Subject authenticationLayerSubject = ltpaConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    @Test
    public void checkWithNullMessageReturnsNullSubject() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = null;

        Subject authenticationLayerSubject = ltpaConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    @Test
    public void checkWhenRequiredWithNullClientAuthenticationTokenThrowsException() throws Exception {
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, true);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        try {
            ltpaConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    @Test
    public void checkWhenRequiredWithEmptyClientAuthenticationTokenThrowsException() throws Exception {
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, true);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        try {
            ltpaConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    @Test
    public void checkWhenRequiredWithNullMessageThrowsException() throws Exception {
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, true);
        EstablishContext msg = null;

        try {
            ltpaConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    @Test
    public void isTrusted() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = createEstablishContextMessage();
        createDecodingExpectations();
        createTokenManagerExpectations();
        createTokenExpectations(accessIDs);
        createTrustExpectations();

        assertTrue("The authentication layer principal must be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    @Test
    public void isTrustedWithNullAccessIDsInLtpaTokenReturnsFalse() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = createEstablishContextMessage();
        createDecodingExpectations();
        createTokenManagerExpectations();
        createTokenExpectations(null);

        assertFalse("The authentication layer principal must not be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    @Test
    public void isTrustedWithEmptyAccessIDsInLtpaTokenReturnsFalse() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = createEstablishContextMessage();
        createDecodingExpectations();
        createTokenManagerExpectations();
        createTokenExpectations(new String[] {});

        assertFalse("The authentication layer principal must not be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    @Test
    public void isTrustedWithNullClientAuthenticationTokenReturnsFalse() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        assertFalse("The authentication layer principal must not be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    @Test
    public void isTrustedWithEmptyClientAuthenticationTokenReturnsFalse() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        assertFalse("The authentication layer principal must not be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    @Test
    public void isTrustedWithNullMessageReturnsFalse() throws Exception {
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, false);
        EstablishContext msg = null;

        assertFalse("The authentication layer principal must not be trusted.", ltpaConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    private void createDecodingExpectations() throws FormatMismatch, TypeMismatch {
        final Any anyObject = mockery.mock(Any.class);
        final InputStream inputStream = mockery.mock(InputStream.class);
        mockery.checking(new Expectations() {
            {
                one(codec).decode_value(with(encodedBytes), with(org.omg.Security.OpaqueHelper.type()));
                will(returnValue(anyObject));
                one(anyObject).create_input_stream();
                will(returnValue(inputStream));
                one(inputStream).read_long();
                will(returnValue(ltpaTokenBytes.length));
                Matcher<byte[]> matcher = anything();
                one(inputStream).read_octet_array(with(matcher), with(0), with(ltpaTokenBytes.length));
            }
        });
    }

    @Test
    public void testToString() {
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, false);
        String theToString = ltpaConfig.toString();
        assertTrue("The toString must start with \"ServerLTPAMechConfig: [\".",
                   theToString.startsWith("ServerLTPAMechConfig: ["));
        assertTrue("The toString must contain the target name.",
                   theToString.contains("targetName:   " + targetName));
        assertTrue("The toString must contain if the mechanism is required.",
                   theToString.contains("required  :   false"));
    }

    private EstablishContext createEstablishContextMessage() throws Exception {
        createEncodingExpectations();
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = Util.encodeLTPAToken(codec, ltpaTokenBytes);
        return msg;
    }

    private void createEncodingExpectations() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(codec).encode_value(with(any(org.omg.CORBA.Any.class)));
                will(returnValue(encodedBytes));
            }
        });
    }

    private void createAuthenticatorExpectations() throws AuthenticationException {
        final Subject authenticatedSubject = new Subject();
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(decodedLtpaTokenBytes);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createAuthenticatorExpectationsThrowsAuthenticationException() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(decodedLtpaTokenBytes);
                will(throwException(new AuthenticationException("There was a problem authenticating.")));
            }
        });
    }

    private void createTokenManagerExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                one(tokenManager).recreateTokenFromBytes(decodedLtpaTokenBytes);
                will(returnValue(ltpaToken));
            }
        });
    }

    private void createTokenExpectations(final String[] accessIDs) {
        mockery.checking(new Expectations() {
            {
                one(ltpaToken).getAttributes("u");
                will(returnValue(accessIDs));
            }
        });
    }

    private void createTrustExpectations() {
        mockery.checking(new Expectations() {
            {
                one(trustedIDEvaluator).isTrusted(trustedId);
                will(returnValue(true));
            }
        });
    }

    @Test
    public void constructorFromClientSideSupportedOnly() throws Exception {
        boolean required = false;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        AS_ContextSec asContextSec = ltpaConfig.encodeIOR(codec);
        ServerLTPAMechConfig ltpaConfigAtClientSide = new ServerLTPAMechConfig(asContextSec);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, ltpaConfigAtClientSide.getSupports());
        assertEquals("The requires association option must be set to 0.", 0, ltpaConfigAtClientSide.getRequires());

    }

    @Test
    public void constructorFromClientSideRequired() throws Exception {
        boolean required = true;
        ServerLTPAMechConfig ltpaConfig = new ServerLTPAMechConfig(authenticator, tokenManager, targetName, required);
        AS_ContextSec asContextSec = ltpaConfig.encodeIOR(codec);
        ServerLTPAMechConfig ltpaConfigAtClientSide = new ServerLTPAMechConfig(asContextSec);

        assertEquals("The supports association option must be set.", EstablishTrustInClient.value, ltpaConfigAtClientSide.getSupports());
        assertEquals("The requires association option must be set.", EstablishTrustInClient.value, ltpaConfigAtClientSide.getRequires());

    }

    private void assertSASException(Exception e) {
        assertTrue("The exception thrown must be a SASException.", e instanceof SASException);
        SASException sasException = (SASException) e;
        assertEquals("The major code must be set.", 1, sasException.getMajor());
        assertEquals("The minor code must be set.", 1, sasException.getMinor());
        assertTrue("The cause exception must be a NO_PERMISSION exception.", sasException.getCause() instanceof NO_PERMISSION);
    }

}
