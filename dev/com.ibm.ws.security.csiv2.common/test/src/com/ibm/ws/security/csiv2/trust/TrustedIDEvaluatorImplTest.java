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
package com.ibm.ws.security.csiv2.trust;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl;
import com.ibm.wsspi.security.csiv2.TrustedIDEvaluator;

public class TrustedIDEvaluatorImplTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TrustedIDEvaluator trustedIDEvaluator;
    private final String trustedUser = "trustedUser";
    private final String anotherTrustedUser = "anotherTrustedUser";
    private final String nonTrustedUser = "nonTrustedUser";
    private final String password = "trusedUserPwd";
    private final String trustedIssuerDN = "CN=trustedUser,OU=Security,O=IBM,C=US";
    private final String pipeSeparatedTrustedIdentities = trustedUser + "|" + anotherTrustedUser;
    private Set<String> trustedIdentities;

    @Before
    public void setUp() throws Exception {
        trustedIdentities = new HashSet<String>();
        trustedIdentities.add(trustedUser);
        trustedIdentities.add(trustedIssuerDN);
        trustedIDEvaluator = new TrustedIDEvaluatorImpl(trustedIdentities);
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl#isTrusted(java.lang.String)}.
     */
    @Test
    public void testIsTrustedUser() {
        assertTrue("The user must be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl#isTrusted(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testIsTrustedUserAndPassword() {
        assertTrue("The user must be trusted.", trustedIDEvaluator.isTrusted(trustedUser, password));
    }

    /**
     * Test method for {@link com.ibm.ws.security.csiv2.trust.TrustedIDEvaluatorImpl#isTrusted(java.security.cert.X509Certificate[])}.
     */
    @Test
    public void testIsTrustedX509CertificateChain() {
        final X509Certificate cert = mockery.mock(X509Certificate.class);
        final X500Principal x500principal = new X500Principal(trustedIssuerDN);
        X509Certificate[] certChain = new X509Certificate[] { cert };

        mockery.checking(new Expectations() {
            {
                one(cert).getIssuerX500Principal();
                will(returnValue(x500principal));
            }
        });

        assertTrue("The certificate issuer DN must be trusted.", trustedIDEvaluator.isTrusted(certChain));
    }

    @Test
    public void testWithNonTrustedIdentity() {
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(nonTrustedUser));
    }

    @Test
    public void testWithNullTrustedIdentity() {
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted((String) null));
    }

    @Test
    public void testWithEmptyTrustedIdentity() {
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(""));
    }

    @Test
    public void testWithSpaceTrustedIdentity() {
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(" "));
    }

    @Test
    public void testWithPipeSeparatedString() {
        trustedIDEvaluator = new TrustedIDEvaluatorImpl(pipeSeparatedTrustedIdentities);
        assertTrue("The user must be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }

    @Test
    public void testWithNullPipeSeparatedString() {
        trustedIDEvaluator = new TrustedIDEvaluatorImpl((String) null);
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }

    @Test
    public void testWithEmptyPipeSeparatedString() {
        trustedIDEvaluator = new TrustedIDEvaluatorImpl("");
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }

    @Test
    public void testWithEmptyTrustedIdentityInPipeSeparatedString() {
        trustedIDEvaluator = new TrustedIDEvaluatorImpl(" |");
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }

    @Test
    public void testWithSpaceTrustedIdentityWhenPipeSeparatedStringHasSpace() {
        trustedIDEvaluator = new TrustedIDEvaluatorImpl(" |");
        assertFalse("The user must not be trusted.", trustedIDEvaluator.isTrusted(trustedUser));
    }
}
