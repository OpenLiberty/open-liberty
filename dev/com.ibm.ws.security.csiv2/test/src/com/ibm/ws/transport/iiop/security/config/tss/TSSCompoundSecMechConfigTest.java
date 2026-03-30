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

import static org.junit.Assert.assertNotNull;

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
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

public class TSSCompoundSecMechConfigTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private TSSCompoundSecMechConfig tssCompoundSecMechConfig;
    private TSSTransportMechConfig transportMechConfig;
    private TSSASMechConfig asMechConfig;
    private TSSSASMechConfig sasMechConfig;
    private Codec codec;
    private SSLSession session;
    private EstablishContext msg;

    private final Subject authLayerSubject = new Subject();

    @Before
    public void setUp() throws Exception {
        transportMechConfig = mockery.mock(TSSTransportMechConfig.class);
        asMechConfig = mockery.mock(TSSASMechConfig.class);
        sasMechConfig = mockery.mock(TSSSASMechConfig.class);
        codec = mockery.mock(Codec.class);
        msg = new EstablishContext();

        tssCompoundSecMechConfig = new TSSCompoundSecMechConfig();
        tssCompoundSecMechConfig.setTransport_mech(transportMechConfig);
        tssCompoundSecMechConfig.setAs_mech(asMechConfig);
        tssCompoundSecMechConfig.setSas_mech(sasMechConfig);

        createToStringExpectationsInCaseOfMockFailure();
    }

    /*
     * If there is a failure during a JUnit assertion, then the failure will output the tssCompoundSecMechConfig.toString().
     * These expectations are not used during testing, but in case of these failures.
     */
    private void createToStringExpectationsInCaseOfMockFailure() {
        mockery.checking(new Expectations() {
            {
                allowing(transportMechConfig).getSupports();
                allowing(transportMechConfig).getRequires();
                allowing(transportMechConfig).toString(with(any(String.class)), with(any(StringBuilder.class)));
                allowing(asMechConfig).getSupports();
                allowing(asMechConfig).getRequires();
                allowing(asMechConfig).toString(with(any(String.class)), with(any(StringBuilder.class)));
                allowing(sasMechConfig).getSupports();
                allowing(sasMechConfig).getRequires();
                allowing(sasMechConfig).toString(with(any(String.class)), with(any(StringBuilder.class)));
            }
        });
    }

    @After
    public void tearDown() throws Exception {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig#check(javax.net.ssl.SSLSession, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        createTransportMechConfigExpectations();
        createASMechConfigExpectations();
        createSASMechConfigExpectations();

        Subject authenticatedSubject = tssCompoundSecMechConfig.check(session, msg, codec);

        assertNotNull("There must be an authenticated subject.", authenticatedSubject);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.transport.iiop.security.config.tss.TSSCompoundSecMechConfig#check(javax.net.ssl.SSLSession, org.omg.CSI.EstablishContext, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheckWithAnonymousAssertion() throws Exception {
        createAnonymousExpectations();
        IdentityToken token = new IdentityToken();
        token.anonymous(true);
        msg.identity_token = token;

        Subject unauthenticatedSubject = tssCompoundSecMechConfig.check(session, msg, codec);

        assertNotNull("There must be an unauthenticated subject.", unauthenticatedSubject);
    }

    private void createTransportMechConfigExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                // Contents do not matter since we are testing the collaboration.
                one(transportMechConfig).check(session);
                will(returnValue(null));
            }
        });
    }

    private void createASMechConfigExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                // Contents do not matter since we are testing the collaboration.
                one(asMechConfig).check(msg, codec);
                will(returnValue(authLayerSubject));
            }
        });
    }

    private void createSASMechConfigExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                // Contents do not matter since we are testing the collaboration.
                one(sasMechConfig).check(tssCompoundSecMechConfig, session, msg, codec);
                will(returnValue(null));
            }
        });
    }

    /*
     * Only the attribute layer must be invoked for anonymous assertion.
     */
    private void createAnonymousExpectations() throws Exception {
        mockery.checking(new Expectations() {
            {
                never(transportMechConfig).check(session);
                never(asMechConfig).check(msg, codec);
                one(sasMechConfig).check(tssCompoundSecMechConfig, session, msg, codec);
                will(returnValue(new Subject()));
            }
        });
    }

}
