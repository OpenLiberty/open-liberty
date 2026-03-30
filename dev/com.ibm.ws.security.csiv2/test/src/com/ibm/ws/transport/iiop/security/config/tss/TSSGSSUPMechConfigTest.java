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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import org.omg.CSI.EstablishContext;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.SecurityMinorCodes;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAbsent;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

public class TSSGSSUPMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    String badGSSUPFromWASClassic = "60820240 06066781 02010101 00BDBDBD 00000015 70657273 6F6E6131 404C6461" +
                                 "70526567 69737472 79BDBDBD 000001F1 EFBFBD4A EFBFBD34 EFBFBD19 2DEFBFBD" +
                                 "EFBFBD27 EFBFBD7C C585EFBF BDEFBFBD EFBFBD38 33EFBFBD EFBFBD61 4F76EFBF" +
                                 "BDEFBFBD 36EFBFBD 32EFBFBD EFBFBDEF BFBD5033 52D9B5EF BFBDEFBF BDEFBFBD" +
                                 "64EFBFBD 7769EFBF BD4240EF BFBD5DEF BFBD71EF BFBDEFBF BDEFBFBD 4AC69DEF" +
                                 "BFBDEFBF BDEFBFBD EFBFBDEF BFBDEFBF BD5125EF BFBDEFBF BDEFBFBD EFBFBD1B" +
                                 "EFBFBDEF BFBD59EF BFBD75EF BFBD5E44 CB81EFBF BD674BEF BFBDEFBF BDEFBFBD" +
                                 "6FEFBFBD 46EFBFBD 0028EFBF BD39DAB8 40EFBFBD 3223EFBF BD7AEFBF BD694340" +
                                 "56EFBFBD 4057EFBF BDEFBFBD EFBFBDEF BFBDEFBF BD22EFBF BD40EFBF BD0CD891" +
                                 "4FEFBFBD EFBFBD65 18EFBFBD EFBFBD24 7BEFBFBD EFBFBDEF BFBD4B1F 26EFBFBD" +
                                 "EFBFBDEF BFBDEFBF BDEFBFBD EFBFBD06 6252EFBF BD362CEF BFBDEFBF BD3609EF" +
                                 "BFBDEFBF BD57EFBF BD3FEFBF BD691CEF BFBDEFBF BDEFBFBD 6BEFBFBD 6D721BEF" +
                                 "BFBDEFBF BD30EFBF BDEFBFBD 7944EFBF BD286F79 7D44EFBF BD264E06 EFBFBD7E" +
                                 "EFBFBDEF BFBD150E EFBFBD66 69EFBFBD 5FEFBFBD 59EFBFBD 4EEFBFBD EFBFBDEF" +
                                 "BFBD53EF BFBD40EF BFBD3365 752B4BEF BFBD5EEF BFBDEFBF BD2EEFBF BD38EFBF" +
                                 "BDEFBFBD EFBFBDEF BFBD57EF BFBDDAA8 EFBFBDEF BFBD46EF BFBD1671 EFBFBDEF" +
                                 "BFBD79EF BFBD0FEF BFBD325B 29EFBFBD 157B6D11 27EFBFBD EFBFBDEF BFBDEFBF" +
                                 "BDBDBDBD 0000001C 04010008 06066781 02010101 0000000C 4C646170 52656769" +
                                 "73747279";

    private final String targetName = "testRealm";
    private final String username = "user1";
    private final String password = "user1pwd";
    private Authenticator authenticator;
    private TrustedIDEvaluator trustedIDEvaluator;
    private Codec codec;
    private SubjectManager subjectManager;

    @Before
    public void setUp() throws Exception {
        authenticator = mockery.mock(Authenticator.class);
        trustedIDEvaluator = new TrustedIDEvaluatorImpl(username);
        codec = new TestCodec();
        subjectManager = new SubjectManager();
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = createEstablishContextMessage();
        createAuthenticatorExpectations();

        Subject authenticationLayerSubject = tssGSSUPMechConfig.check(msg, codec);
        assertNotNull("There must be an authentication layer subject.", authenticationLayerSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckFlow() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = createEstablishContextMessage(createEncodingFromCSS());
        createAuthenticatorExpectations();

        Subject authenticationLayerSubject = tssGSSUPMechConfig.check(msg, codec);
        assertNotNull("There must be an authentication layer subject.", authenticationLayerSubject);
    }

    private byte[] createEncodingFromCSS() {
        TSSASMechConfig tssasMechConfig = new TSSGSSUPMechConfig(null, targetName, false);
        subjectManager.setInvocationSubject(createBasicAuthSubject());

        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(targetName);
        CSSSASMechConfig sasMechConfig = new CSSSASMechConfig();
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        return cssGSSUPMechConfigDynamic.encode(tssasMechConfig, sasMechConfig, mockery.mock(ClientRequestInfo.class), codec);
    }

    private Subject createBasicAuthSubject() {
        Subject basicAuthSubject = new Subject();
        WSCredential basicAuthCredential = new WSCredentialImpl(targetName, username, password);
        basicAuthSubject.getPublicCredentials().add(basicAuthCredential);
        return basicAuthSubject;
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithAuthenticationExceptionThrowsException() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = createEstablishContextMessage();
        createAuthenticatorExpectationsThrowsAuthenticationException();

        try {
            tssGSSUPMechConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithNullClientAuthenticationTokenReturnsNullSubject() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        Subject authenticationLayerSubject = tssGSSUPMechConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithEmptyClientAuthenticationTokenReturnsNullSubject() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        Subject authenticationLayerSubject = tssGSSUPMechConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithNullMessageReturnsNullSubject() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = null;

        Subject authenticationLayerSubject = tssGSSUPMechConfig.check(msg, codec);

        assertNull("There must not be an authentication layer subject.", authenticationLayerSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWhenRequiredWithNullClientAuthenticationTokenThrowsException() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, true);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        try {
            tssGSSUPMechConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWhenRequiredWithEmptyClientAuthenticationTokenThrowsException() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, true);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        try {
            tssGSSUPMechConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWhenRequiredWithNullMessageThrowsException() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, true);
        EstablishContext msg = null;

        try {
            tssGSSUPMechConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertSASException(e);
        }
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#check(org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckInvalidWASClassicFlow() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = createEstablishContextMessage(getData(badGSSUPFromWASClassic));

        try {
            tssGSSUPMechConfig.check(msg, codec);
            fail("The check method must fail with a SASException.");
        } catch (Exception e) {
            assertTrue("The exception thrown must be a SASException.", e instanceof SASException);
            SASException sasException = (SASException) e;
            assertEquals("The major code must be set.", 2, sasException.getMajor());
            Throwable cause = sasException.getCause();
            assertTrue("The cause exception must be a NO_PERMISSION exception.", cause instanceof NO_PERMISSION);
            assertEquals("The NO_PERMISSION message must be set.",
                         "CWWKS9549E: The server cannot decode the GSSUP token sent by the client and it cannot authenticate the token.",
                         cause.getMessage());
            assertEquals("The NO_PERMISSION minor code must be set.", SecurityMinorCodes.GSS_FORMAT_ERROR, ((NO_PERMISSION) cause).minor);
        }
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#isTrusted(com.ibm.wsspi.security.csiv2.TrustedIDEvaluator, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void testIsTrusted() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = createEstablishContextMessage();

        assertTrue("The authentication layer principal must be trusted.", tssGSSUPMechConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#isTrusted(com.ibm.wsspi.security.csiv2.TrustedIDEvaluator, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void isTrustedWithNullClientAuthenticationTokenReturnsFalse() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = null;

        assertFalse("The authentication layer principal must not be trusted.", tssGSSUPMechConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#isTrusted(com.ibm.wsspi.security.csiv2.TrustedIDEvaluator, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void isTrustedWithEmptyClientAuthenticationTokenReturnsFalse() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = new byte[0];

        assertFalse("The authentication layer principal must not be trusted.", tssGSSUPMechConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#isTrusted(com.ibm.wsspi.security.csiv2.TrustedIDEvaluator, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void isTrustedWithMalformedClientAuthenticationTokenReturnsFalse() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = "Some malformed client authentication token".getBytes();

        assertFalse("The authentication layer principal must not be trusted.", tssGSSUPMechConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig#isTrusted(com.ibm.wsspi.security.csiv2.TrustedIDEvaluator, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void isTrustedWithNullMessageReturnsFalse() throws Exception {
        TSSGSSUPMechConfig tssGSSUPMechConfig = new TSSGSSUPMechConfig(authenticator, targetName, false);
        EstablishContext msg = null;

        assertFalse("The authentication layer principal must not be trusted.", tssGSSUPMechConfig.isTrusted(trustedIDEvaluator, msg, codec));
    }

    private EstablishContext createEstablishContextMessage() throws Exception {
        return createEstablishContextMessage(Util.encodeGSSUPToken(codec, username, password.toCharArray(), targetName));
    }

    private EstablishContext createEstablishContextMessage(byte[] encoding) throws Exception {
        EstablishContext msg = new EstablishContext();
        msg.client_authentication_token = encoding;
        return msg;
    }

    private void createAuthenticatorExpectations() throws AuthenticationException {
        final Subject authenticatedSubject = new Subject();
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(username, password);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createAuthenticatorExpectationsThrowsAuthenticationException() throws AuthenticationException {
        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(username, password);
                will(throwException(new AuthenticationException("There was a problem authenticating.")));
            }
        });
    }

    private void assertSASException(Exception e) {
        assertTrue("The exception thrown must be a SASException.", e instanceof SASException);
        SASException sasException = (SASException) e;
        assertEquals("The major code must be set.", 1, sasException.getMajor());
        assertEquals("The minor code must be set.", 1, sasException.getMinor());
        assertTrue("The cause exception must be a NO_PERMISSION exception.", sasException.getCause() instanceof NO_PERMISSION);
    }

    private byte[] getData(String aString) {
        String trimmedString = trim(aString);
        int length = trimmedString.length();
        byte[] data = new byte[length / 2];
        int i = 0;
        int j = 0;
        while (i < length) {
            int topHalf = Integer.parseInt(trimmedString.substring(i, i + 1), 16);
            int bottomHalf = Integer.parseInt(trimmedString.substring(i + 1, i + 2), 16);
            data[j] = (byte) (((byte) topHalf << 4) + (byte) bottomHalf);
            i = i + 2;
            j++;
        }
        return data;
    }

    private String trim(String aString) {
        return aString.replaceAll(" ", "");
    }

}
