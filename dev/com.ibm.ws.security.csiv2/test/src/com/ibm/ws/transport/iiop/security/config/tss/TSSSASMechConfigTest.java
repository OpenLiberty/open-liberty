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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSI.EstablishContext;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.ITTDistinguishedName;
import org.omg.CSI.ITTPrincipalName;
import org.omg.CSI.ITTX509CertChain;
import org.omg.CSI.IdentityToken;
import org.omg.CSIIOP.SAS_ContextSec;
import org.omg.CSIIOP.ServiceConfiguration;
import org.omg.GSSUP.GSSUPMechOID;
import org.omg.IOP.Codec;

import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.SASException;
import com.ibm.ws.transport.iiop.security.util.Util;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

public class TSSSASMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TSSSASMechConfig tssSASMechConfig;
    private TSSSASIdentityToken tssIdentityToken;
    private TrustedIDEvaluator trustedIDEvaluator;
    private TSSCompoundSecMechConfig compoundSecMech;
    private TSSTransportMechConfig transportMechConfig;
    private TSSASMechConfig asMechConfig;
    private IdentityToken identityToken;
    private SSLSession session;
    private EstablishContext msg;
    private Codec codec;

    private final Subject attributeLayerSubject = new Subject();
    private final String principalName = "user1";
    private final byte[] principalNameEncoding = Util.encodeGSSExportName(GSSUPMechOID.value, principalName);

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
        trustedIDEvaluator = mockery.mock(TrustedIDEvaluator.class);
        asMechConfig = mockery.mock(TSSASMechConfig.class);
        transportMechConfig = mockery.mock(TSSTransportMechConfig.class);
        codec = mockery.mock(Codec.class);
        identityToken = new IdentityToken();
        createCompoundSecMech();
        createTSSSASMechConfig();
    }

    private void createCompoundSecMech() {
        compoundSecMech = new TSSCompoundSecMechConfig();
        compoundSecMech.setAs_mech(asMechConfig);
        compoundSecMech.setTransport_mech(transportMechConfig);
    }

    private void createTSSSASMechConfig() {
        tssSASMechConfig = new TSSSASMechConfig(trustedIDEvaluator);
        tssSASMechConfig.addIdentityToken(new TSSITTAbsent());
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig#check(com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig, javax.net.ssl.SSLSession, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void testCheckWitNullMessageReturnsNullSubject() throws Exception {
        Subject authenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, null, codec);
        assertNull("There must not be an attribute layer subject.", authenticatedSubject);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSSASMechConfig#check(com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig, javax.net.ssl.SSLSession, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}
     * .
     */
    @Test
    public void testCheckWithoutIdentityToken() throws Exception {
        identityToken.absent(true);
        createEstablishContext(null);

        Subject authenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNull("There must not be an attribute layer subject.", authenticatedSubject);
    }

    @Test
    public void testCheckWithITTAbsent() throws Exception {
        identityToken.absent(true);
        createEstablishContext(identityToken);

        Subject authenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNull("There must not be an attribute layer subject.", authenticatedSubject);
    }

    @Test
    public void testCheckWithTrustAtAuthenticationLayer() throws Exception {
        identityToken.principal_name(principalNameEncoding);
        createEstablishContext(identityToken);
        createTSSSASIdentityTokenTrustedPathExpectations((short) ITTPrincipalName.value);
        createTrustedIDEvaluatorExpectations();
        createTSSASMechConfigExpectations(true);

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        Subject authenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNotNull("There must be an attribute layer subject.", authenticatedSubject);
    }

    @Test
    public void testCheckWithTrustAtTransportLayer() throws Exception {
        identityToken.principal_name(principalNameEncoding);
        createEstablishContext(identityToken);
        createTSSSASIdentityTokenTrustedPathExpectations((short) ITTPrincipalName.value);
        createTrustedIDEvaluatorExpectations();
        createTSSASMechConfigExpectations(false);
        createTSSTransportMechExpectations(true);

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        Subject authenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNotNull("There must be an attribute layer subject.", authenticatedSubject);
    }

    @Test(expected = SASException.class)
    public void testCheckWithUnsupportedTokenType() throws Exception {
        IdentityToken identityToken = new IdentityToken();
        identityToken.principal_name(principalNameEncoding);
        createEstablishContext(identityToken);

        tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        fail("The check method must fail with a SASException.");
    }

    @Test(expected = SASException.class)
    public void testCheckWithNonTrusted() throws Exception {
        identityToken.principal_name(principalNameEncoding);
        createEstablishContext(identityToken);
        tssIdentityToken = createTSSSASIdentityToken((short) ITTPrincipalName.value);
        createTrustedIDEvaluatorExpectations();
        createTSSASMechConfigExpectations(false);
        createTSSTransportMechExpectations(false);

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        fail("The check method must fail with a SASException.");
    }

    @Test
    public void testCheckWithITTAnonymous() throws Exception {
        identityToken.anonymous(true);
        createEstablishContext(identityToken);
        createTSSSASIdentityTokenTrustedPathExpectations((short) ITTAnonymous.value);

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        Subject unauthenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNotNull("There must be an attribute layer subject.", unauthenticatedSubject);
    }

    @Test
    public void testCheckWithITTDistinguishedName() throws Exception {
        codec = new TestCodec();
        String distinguishedName = "CN=user1, OU=security, O=ibm, C=us";
        identityToken.dn(Util.encodeDN(codec, distinguishedName));
        createEstablishContext(identityToken);
        createTSSSASIdentityTokenTrustedPathExpectations((short) ITTDistinguishedName.value);
        createPresumedTrustExpectation();

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        Subject unauthenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNotNull("There must be an attribute layer subject.", unauthenticatedSubject);
    }

    @Test
    public void testCheckWithITTX509CertChain() throws Exception {
        codec = new TestCodec();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(base64TestCertificateString.getBytes()));
        X509Certificate[] certificateChain = new X509Certificate[] { certificate };

        identityToken.certificate_chain(Util.encodeCertChain(codec, certificateChain));
        createEstablishContext(identityToken);
        createTSSSASIdentityTokenTrustedPathExpectations((short) ITTX509CertChain.value);
        createPresumedTrustExpectation();

        tssSASMechConfig.addIdentityToken(tssIdentityToken);
        Subject unauthenticatedSubject = tssSASMechConfig.check(compoundSecMech, session, msg, codec);
        assertNotNull("There must be an attribute layer subject.", unauthenticatedSubject);
    }

    @Test
    public void testGetSupportedIdentityTypesAtServerSide() throws Exception {
        TSSSASIdentityToken tssAnonymousIdentityToken = createTSSSASIdentityToken((short) ITTAnonymous.value);
        TSSSASIdentityToken tssPrincipalNameIdentityToken = createTSSSASIdentityToken((short) ITTPrincipalName.value);
        TSSSASIdentityToken tssDistinguishedNameIdentityToken = createTSSSASIdentityToken((short) ITTDistinguishedName.value);

        tssSASMechConfig = new TSSSASMechConfig(trustedIDEvaluator);
        tssSASMechConfig.addIdentityToken(tssPrincipalNameIdentityToken);
        tssSASMechConfig.addIdentityToken(tssAnonymousIdentityToken);
        tssSASMechConfig.addIdentityToken(tssDistinguishedNameIdentityToken);

        int supportedIdentityTypes = tssSASMechConfig.getSupportedIdentityTypes();

        assertEquals("The supported identity types must include all token types set.", (ITTAnonymous.value | ITTPrincipalName.value | ITTDistinguishedName.value),
                     supportedIdentityTypes);
    }

    @Test
    public void testGetSupportedIdentityTypesAtClientSide() throws Exception {
        SAS_ContextSec sasContextSec = new SAS_ContextSec();
        sasContextSec.privilege_authorities = new ServiceConfiguration[0];
        sasContextSec.supported_naming_mechanisms = new byte[0][];
        sasContextSec.supported_identity_types |= ITTAnonymous.value | ITTPrincipalName.value;
        tssSASMechConfig = new TSSSASMechConfig(sasContextSec);

        int supportedIdentityTypes = tssSASMechConfig.getSupportedIdentityTypes();

        assertEquals("The supported identity types must be read from the SAS_ContextSec structure.", sasContextSec.supported_identity_types, supportedIdentityTypes);
    }

    @Test
    public void testEncodeIOR() throws Exception {
        TSSSASIdentityToken tssPrincipalNameIdentityToken = createTSSSASIdentityTokenWithOid((short) ITTPrincipalName.value, GSSUPMechOID.value);
        TSSSASIdentityToken tssAnonymousIdentityToken = createTSSSASIdentityTokenWithOid((short) ITTAnonymous.value, "");

        tssSASMechConfig = new TSSSASMechConfig(trustedIDEvaluator);
        tssSASMechConfig.addIdentityToken(tssPrincipalNameIdentityToken);
        tssSASMechConfig.addIdentityToken(tssAnonymousIdentityToken);

        SAS_ContextSec sasContextSec = tssSASMechConfig.encodeIOR(codec);
        assertEquals("The supported identity types must be set in the SAS_ContextSec structure.", (ITTPrincipalName.value | ITTAnonymous.value),
                     sasContextSec.supported_identity_types);
    }

    private void createTSSSASIdentityTokenTrustedPathExpectations(final short type) throws Exception {
        tssIdentityToken = createTSSSASIdentityToken(type);
        mockery.checking(new Expectations() {
            {
                one(tssIdentityToken).check(identityToken, codec);
                will(returnValue(attributeLayerSubject));
            }
        });
    }

    private TSSSASIdentityToken createTSSSASIdentityToken(final short type) throws Exception {
        final TSSSASIdentityToken tssSASIdentityToken = mockery.mock(TSSSASIdentityToken.class, "TSSSASIdentityToken-" + type);
        mockery.checking(new Expectations() {
            {
                allowing(tssSASIdentityToken).getType();
                will(returnValue(type));
            }
        });
        return tssSASIdentityToken;
    }

    private TSSSASIdentityToken createTSSSASIdentityTokenWithOid(final short type, final String oid) throws Exception {
        final TSSSASIdentityToken tssSASIdentityToken = createTSSSASIdentityToken(type);
        mockery.checking(new Expectations() {
            {
                allowing(tssSASIdentityToken).getOID();
                will(returnValue(oid));
            }
        });
        return tssSASIdentityToken;
    }

    private void createTrustedIDEvaluatorExpectations() {
        mockery.checking(new Expectations() {
            {
                one(trustedIDEvaluator).isTrusted(with("*"));
                will(returnValue(false));
            }
        });
    }

    private void createTSSASMechConfigExpectations(final boolean trusted) {
        mockery.checking(new Expectations() {
            {
                one(asMechConfig).isTrusted(trustedIDEvaluator, msg, codec);
                will(returnValue(trusted));
            }
        });
    }

    private void createTSSTransportMechExpectations(final boolean trusted) {
        mockery.checking(new Expectations() {
            {
                one(transportMechConfig).isTrusted(trustedIDEvaluator, session);
                will(returnValue(trusted));
            }
        });
    }

    private void createEstablishContext(IdentityToken identityToken) {
        msg = new EstablishContext();
        msg.identity_token = identityToken;
    }

    private void createPresumedTrustExpectation() {
        mockery.checking(new Expectations() {
            {
                one(trustedIDEvaluator).isTrusted(with("*"));
                will(returnValue(true));
            }
        });
    }

}
