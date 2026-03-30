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
package com.ibm.ws.transport.iiop.security.config.css;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.ClientRequestInfo;

import com.ibm.websphere.security.cred.WSCredential;
import com.ibm.ws.security.context.SubjectManager;
import com.ibm.ws.security.credentials.wscred.WSCredentialImpl;
import com.ibm.ws.security.csiv2.server.config.tss.ServerLTPAMechConfig;
import com.ibm.ws.security.csiv2.tools.TestCodec;
import com.ibm.ws.transport.iiop.security.config.tss.TSSASMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSGSSUPMechConfig;
import com.ibm.ws.transport.iiop.security.config.tss.TSSNULLASMechConfig;
import com.ibm.wsspi.kernel.service.utils.SerializableProtectedString;

public class CSSGSSUPMechConfigDynamicTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private final String domain = "testRealm";
    private final String realmName = "testRealm";
    private final String securityName = "user1";
    private final String password = "user1pwd";
    private final String trustedIdentity = "trustedIdentity";
    private final String trustedPassword = "trustedPassword";
    private TSSASMechConfig tssasMechConfig;
    private CSSSASMechConfig sasMechConfig;
    private ClientRequestInfo ri;
    private Codec codec;
    private SubjectManager subjectManager;

    @Before
    public void setUp() throws Exception {
        sasMechConfig = new CSSSASMechConfig();
        ri = mockery.mock(ClientRequestInfo.class);
        codec = new TestCodec();
        subjectManager = new SubjectManager();
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncode() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(null, domain, false);
        subjectManager.setInvocationSubject(createBasicAuthSubject());

        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = cssGSSUPMechConfigDynamic.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeDuringIdentityAssertion() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(null, domain, false);

        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain);
        sasMechConfig.addIdentityToken(new CSSSASITTPrincipalNameDynamic(null, domain));
        sasMechConfig.setTrustedIdentity(trustedIdentity);
        sasMechConfig.setTrustedPassword(new SerializableProtectedString(trustedPassword.toCharArray()));
        byte[] encoding = cssGSSUPMechConfigDynamic.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an encoding.", encoding.length != 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeNeverAtTarget() throws Exception {
        tssasMechConfig = new TSSNULLASMechConfig();
        subjectManager.setInvocationSubject(createBasicAuthSubject());

        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = cssGSSUPMechConfigDynamic.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an empty encoding.", encoding.length == 0);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.css.CSSGSSUPMechConfigDynamic#encode(com.ibm.ws.transport.iiop.security.config.css.CSSSASMechConfig, org.omg.IOP.Codec)}.
     */
    @Test
    public void testEncodeDifferentAtTarget() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(null, null, domain, false);
        subjectManager.setInvocationSubject(createBasicAuthSubject());

        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain);
        sasMechConfig.addIdentityToken(new CSSSASITTAbsent());
        byte[] encoding = cssGSSUPMechConfigDynamic.encode(tssasMechConfig, sasMechConfig, ri, codec);

        assertTrue("There must be an empty encoding.", encoding.length == 0);
    }

    @Test
    public void testCanHandle() throws Exception {
        tssasMechConfig = new TSSGSSUPMechConfig(null, domain, false);
        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain, false);

        assertTrue("The client authentication layer must be able handle the target when it is the same kind.",
                   cssGSSUPMechConfigDynamic.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentSupportedAtTarget() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(null, null, domain, false);
        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain, false);

        assertTrue("The client authentication layer must be able handle the target when it is supported and the client does not require authentication.",
                   cssGSSUPMechConfigDynamic.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentRequiredAtTarget() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(null, null, domain, true);
        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain, false);

        assertFalse("The client authentication layer must not be able handle the target when it is required and the client does not require authentication.",
                    cssGSSUPMechConfigDynamic.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentSupportedAtTargetRequiredAtClient() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(null, null, domain, false);
        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain, true);

        assertFalse("The client authentication layer must not be able handle the target when it is supported and the client requires authentication.",
                    cssGSSUPMechConfigDynamic.canHandle(tssasMechConfig));
    }

    @Test
    public void testCanHandleDifferentRequiredAtTargetRequiredAtClient() throws Exception {
        tssasMechConfig = new ServerLTPAMechConfig(null, null, domain, true);
        CSSGSSUPMechConfigDynamic cssGSSUPMechConfigDynamic = new CSSGSSUPMechConfigDynamic(domain, true);

        assertFalse("The client authentication layer must not be able handle the target when it is required and the client requires authentication.",
                    cssGSSUPMechConfigDynamic.canHandle(tssasMechConfig));
    }

    // TODO: Add exception and error conditions cases

    private Subject createBasicAuthSubject() {
        Subject basicAuthSubject = new Subject();
        WSCredential basicAuthCredential = new WSCredentialImpl(realmName, securityName, password);
        basicAuthSubject.getPublicCredentials().add(basicAuthCredential);
        return basicAuthSubject;
    }

}
