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
import static org.junit.Assert.assertTrue;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.omg.CSI.ITTAnonymous;
import org.omg.CSI.IdentityToken;
import org.omg.IOP.Codec;

import com.ibm.ws.security.authentication.UnauthenticatedSubjectService;

public class TSSITTAnonymousTest {

    private final Mockery mockery = new JUnit4Mockery();

    private TSSITTAnonymous tssITTAnonymous;
    private UnauthenticatedSubjectService unauthenticatedSubjectService;
    // The value of the test subject does not matter, we are testing for the collaboration with the UnauthenticatedSubjectService
    private final Subject unauthenticatedSubject = new Subject();

    @Before
    public void setUp() {
        unauthenticatedSubjectService = mockery.mock(UnauthenticatedSubjectService.class);
        tssITTAnonymous = new TSSITTAnonymous(unauthenticatedSubjectService);
    }

    @After
    public void tearDown() {
        mockery.assertIsSatisfied();
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTAnonymous#getType()}.
     */
    @Test
    public void testGetType() {
        assertEquals("The token type must be set.", ITTAnonymous.value, tssITTAnonymous.getType());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTAnonymous#getOID()}.
     */
    @Test
    public void testGetOID() {
        assertEquals("The token OID must be empty.", "", tssITTAnonymous.getOID());
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTAnonymous#check(org.omg.CSI.IdentityToken, org.omg.IOP.Codec)}.
     */
    @Test
    public void testCheck() throws Exception {
        createUnauthenticatedSubjectServiceExpectations();

        IdentityToken identityToken = new IdentityToken();
        identityToken.anonymous(true);
        Codec codec = mockery.mock(Codec.class);
        Subject assertionSubject = tssITTAnonymous.check(identityToken, codec);

        assertEquals("The asserted subject must be the unauthenticated subject.", unauthenticatedSubject, assertionSubject);
    }

    /**
     * Test method for {@link com.ibm.ws.transport.iiop.security.config.tss.TSSITTAnonymous#toString(java.lang.String, java.lang.StringBuilder)}.
     */
    @Test
    public void testToStringStringStringBuilder() {
        StringBuilder sb = new StringBuilder();
        tssITTAnonymous.toString("", sb);
        assertTrue("The toString method must write the class name.", sb.toString().contains("TSSITTAnonymous"));
    }

    private void createUnauthenticatedSubjectServiceExpectations() {
        mockery.checking(new Expectations() {
            {
                one(unauthenticatedSubjectService).getUnauthenticatedSubject();
                will(returnValue(unauthenticatedSubject));
            }
        });
    }

}
