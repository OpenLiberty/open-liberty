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

package com.ibm.ws.security.csiv2.server.config.css;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CORBA.BAD_PARAM;
import org.omg.IOP.Codec;
import org.omg.IOP.CodecPackage.InvalidTypeForEncoding;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.csiv2.Authenticator;
import com.ibm.ws.security.csiv2.config.LTPAMech;
import com.ibm.ws.security.csiv2.server.config.tss.ServerLTPAMechConfig;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTAbsent;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASITTPrincipalNameDynamic;
import com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLASMechConfig;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.token.SingleSignonToken;

public class ClientLTPAMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private Authenticator authenticator;
    private final String domain = "testRealm";
    private final String trustedIdentity = "trustedIdentity";
    private TSSASMechConfig tssasMechConfig;
    private CSSSASMechConfig sasMechConfig;

    private Codec codec;
    private ClientRequestInfo ri;
    private SingleSignonToken ssoToken;
    private final byte[] ssoTokenBytes = "Some token bytes".getBytes();
    // It does not matter what bytes, we are testing that the codec is used to encode them.
    private SubjectManager subjectManager;

    @Before
    public void setUp() throws Exception {
        authenticator = mockery.mock(Authenticator.class);
        sasMechConfig = new CSSSASMechConfig();
        codec = new TestCodec();
        ri = mockery.mock(ClientRequestInfo.class);
        ssoToken = mockery.mock(SingleSignonToken.class);
        subjectManager = new SubjectManager();
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncode() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        createSSOTokenExpectations();
        createEncodingExpectations();
        subjectManager.setInvocationSubject(createAuthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeFromCallerSubject() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        createSSOTokenExpectations();
        createEncodingExpectations();
        subjectManager.setCallerSubject(createAuthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeDuringIdentityAssertion() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        createAuthenticatorExpectations();
        createSSOTokenExpectations();
        createEncodingExpectations();

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTPrincipalNameDynamic(null, domain));
        sasMechConfig.setTrustedIdentity(trustedIdentity);
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeNeverAtTarget() throws Exception {
        tssasMechConfig = new TSSNULLASMechConfig();
        subjectManager.setInvocationSubject(createAuthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an empty encoding.", encoding.length == 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeDifferentAtTarget() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(authenticator, domain, false);
        subjectManager.setInvocationSubject(createAuthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an empty encoding.", encoding.length == 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeForWASClassic() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        createSSOTokenExpectations();
        createEncodingExpectationsForWASClassic();
        subjectManager.setInvocationSubject(createAuthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
        String expectedOID = LTPAMech.LTPA_OID.substring(4);
        byte[] embeddedData = Util.readGSSTokenData(expectedOID, encoding);
        assertTrue("The GSS token must contain another GSS token.", Util.isGSSToken(expectedOID, embeddedData));
        byte[] decodedBytes = Util.decodeLTPAToken(codec, encoding);
        assertEquals("The decoded bytes must be the same as the bytes before encoding", Util.byteToString(ssoTokenBytes), Util.byteToString(decodedBytes));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.csiv2.server.config.css.ClientLTPAMechConfig#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeUnauthenticated() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        subjectManager.setInvocationSubject(createUnauthenticatedSubject());

        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = clientLTPAMechConfig.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("The encoding must be an empty byte array.", encoding.length == 0);
    }

    @Test
    public void testCanHandle() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(authenticator, null, domain, false);
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);

        assertTrue("The client authentication layer must be able handle the target when it is the same kind.",
                   clientLTPAMechConfig.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentSupportedAtTarget() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(authenticator, domain, false);
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);

        assertTrue("The client authentication layer must be able handle the target when it is supported and the client does not require authentication.",
                   clientLTPAMechConfig.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentRequiredAtTarget() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(authenticator, domain, true);
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);

        assertFalse("The client authentication layer must not be able handle the target when it is required and the client does not require authentication.",
                    clientLTPAMechConfig.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentSupportedAtTargetRequiredAtClient() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(authenticator, domain, false);
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, true);

        assertFalse("The client authentication layer must not be able handle the target when it is supported and the client requires authentication.",
                    clientLTPAMechConfig.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentRequiredAtTargetRequiredAtClient() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(authenticator, domain, true);
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, true);

        assertFalse("The client authentication layer must not be able handle the target when it is required and the client requires authentication.",
                    clientLTPAMechConfig.canHandle(tssasMechConfig));
    }

    @Test
    public void testGetMechanism() throws Exception {
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        assertEquals("The mechanism type must be set.", "LTPA", clientLTPAMechConfig.getMechanism());
    }

    @Test
    public void testToString() {
        ClientLTPAMechConfig clientLTPAMechConfig = new ClientLTPAMechConfig(authenticator, domain, false);
        String theToString = clientLTPAMechConfig.toString();
        assertNotNull("the toString must not be empty.", theToString.isEmpty());
        assertTrue("The toString must contain the target name.",
                   theToString.contains("domain:   " + domain));
        assertTrue("The toString must contain if the mechanism is required.",
                   theToString.contains("required  :   false"));
    }

    private Subject createAuthenticatedSubject() {
        final WSCredential wsCredential = createWSCredential(false);
        final Subject authenticatedSubject = new Subject();
        Set<Object> privateCredentials = authenticatedSubject.getPrivateCredentials();
        privateCredentials.add(ssoToken);
        authenticatedSubject.getPublicCredentials().add(wsCredential);
        return authenticatedSubject;
    }

    private Subject createUnauthenticatedSubject() {
        final WSCredential wsCredential = createWSCredential(true);
        final Subject authenticatedSubject = new Subject();
        authenticatedSubject.getPublicCredentials().add(wsCredential);
        return authenticatedSubject;
    }

    private WSCredential createWSCredential(final boolean unauthenticated) {
        final WSCredential wsCredential = mockery.mock(WSCredential.class);
        mockery.checking(new Expectations() {
            {
                allowing(wsCredential).isUnauthenticated();
                will(returnValue(unauthenticated));
            }
        });
        return wsCredential;
    }

    private void createAuthenticatorExpectations() throws AuthenticationException {
        final Subject authenticatedSubject = createAuthenticatedSubject();

        mockery.checking(new Expectations() {
            {
                one(authenticator).authenticate(trustedIdentity);
                will(returnValue(authenticatedSubject));
            }
        });
    }

    private void createSSOTokenExpectations() {
        mockery.checking(new Expectations() {
            {
                one(ssoToken).getBytes();
                will(returnValue(ssoTokenBytes));
            }
        });
    }

    private void createEncodingExpectations() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(ri).get_effective_component(0x49424d0a);
                will(throwException(new BAD_PARAM())); // Default for Liberty
            }
        });
    }

    private void createEncodingExpectationsForWASClassic() throws InvalidTypeForEncoding {
        mockery.checking(new Expectations() {
            {
                one(ri).get_effective_component(0x49424d0a);
                // Just return for WAS Classic
            }
        });
    }

}
