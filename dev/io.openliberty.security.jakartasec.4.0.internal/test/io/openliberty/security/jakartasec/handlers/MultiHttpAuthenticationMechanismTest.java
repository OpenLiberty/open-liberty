/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.handlers;

import static org.junit.Assert.assertTrue;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.context.SubjectManager;
//import com.ibm.ws.security.javaeesec.cdi.beans.BasicHttpAuthenticationMechanism;

import io.openliberty.security.jakartasec.handlers.wrappers.MultiHttpAuthenticationMechanismWrapper;
import jakarta.security.enterprise.AuthenticationException;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;

/**
 * Testing the MultiHttpAuthenticationMechanism class which simply adds
 * Multi HAM specific functionality to the HttpAuthenticationMechanism
 * (Form, Basic, etc ...) to augment (decorate) existing functionality.
 *
 */
public class MultiHttpAuthenticationMechanismTest {

    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    private HttpAuthenticationMechanism httpAuthenticationMechanism;

    private final SubjectManager subjectManager = new SubjectManager();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        httpAuthenticationMechanism = mockery.mock(HttpAuthenticationMechanism.class);
    }

    @After
    public void tearDown() throws Exception {
        subjectManager.clearSubjects();
        mockery.assertIsSatisfied();
    }

    /**
     * Ensure that all the in-built HAMs are ordered correctly, not worrying
     * about the absolute priority values (as these could change), but about
     * their relative (to each other) ordering.
     *
     * Also, ensure an application HAM (i.e. not an in-built one) has the highest
     * priority.
     *
     * @throws AuthenticationException
     */

    @Test
    public void testGetPriority() throws Exception {
        // given/when ...
        int customFormPriority = getPriority("CustomFormAuthenticationMechanism");
        int oidcPriority = getPriority("OidcHttpAuthenticationMechanism");
        int formPriority = getPriority("FormAuthenticationMechanism");
        int basicPriority = getPriority("BasicHttpAuthenticationMechanism");
        int applicationPriority = getPriority(null);

        // then
        assertTrue("Expected BasicHttpAuthenticationMechanism to be lower than FormAuthenticationMechanism.",
                   basicPriority < formPriority);
        assertTrue("Expected FormAuthenticationMechanism to be lower than CustomFormAuthenticationMechanism.",
                   formPriority < customFormPriority);
        assertTrue("Expected CustomFormAuthenticationMechanism to be lower than OidcAuthenticationMechanism.",
                   customFormPriority < oidcPriority);
        assertTrue("Expected OidcAuthenticationMechanism to be lower any application defined one.",
                   oidcPriority < applicationPriority);
    }

    private int getPriority(String hamName) {
        return new MultiHttpAuthenticationMechanismWrapper(httpAuthenticationMechanism, hamName).getPriority();
    }
}
