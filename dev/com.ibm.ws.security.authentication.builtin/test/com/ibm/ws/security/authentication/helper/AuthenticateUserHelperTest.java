/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.helper;

import static org.junit.Assert.assertNotNull;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.AuthenticationException;
import com.ibm.ws.security.authentication.AuthenticationService;
import com.ibm.ws.security.authentication.utility.JaasLoginConfigConstants;

/**
 *
 */
public class AuthenticateUserHelperTest {

    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };

    AuthenticateUserHelper authnHelper = new AuthenticateUserHelper();
    final Subject authenticatedSubject = new Subject();
    private final AuthenticationService authnService = mockery.mock(AuthenticationService.class);

    private static final String userName = "userName";
    private static final String jaasEntryName = "jaasEntryName";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
    }

    @Before
    public void setUp() {

    }

    @After
    public void tearDown() throws Exception {
        outputMgr.resetStreams();
        mockery.assertIsSatisfied();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.restoreStreams();
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.helper.AuthenticateUserHelper#authenticateUser(com.ibm.ws.security.authentication.AuthenticationService, java.lang.String, java.lang.String)}
     * .
     * 
     * should throw AuthenticationException when authenticationService is null
     */
    @Test(expected = AuthenticationException.class)
    public void testAuthenticateUserNullAuthnService() throws Exception {
        authnHelper.authenticateUser(null, null, null);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.helper.AuthenticateUserHelper#authenticateUser(com.ibm.ws.security.authentication.AuthenticationService, java.lang.String, java.lang.String)}
     * .
     * 
     * should throw AuthenticationException when authenticationService is null
     */
    @Test(expected = AuthenticationException.class)
    public void testAuthenticateUserNullUserName() throws Exception {
        authnHelper.authenticateUser(authnService, null, null);
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.helper.AuthenticateUserHelper#authenticateUser(com.ibm.ws.security.authentication.AuthenticationService, java.lang.String, java.lang.String)}
     * .
     * 
     * should use SYSTEM_DEFAULT when the jaas entry name is not passed in
     * and successfully authenticate
     */
    @Test
    public void testAuthenticateUserNullJaasEntryName() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(authnService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(true));
            }
        });
        final Subject partialSubject = authnHelper.createPartialSubject(userName, authnService, null);
        mockery.checking(new Expectations() {
            {
                allowing(authnService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(true));
                allowing(authnService).authenticate(JaasLoginConfigConstants.SYSTEM_DEFAULT, partialSubject);
                will(returnValue(authenticatedSubject));
            }
        });

        assertNotNull(authnHelper.authenticateUser(authnService, userName, null));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.helper.AuthenticateUserHelper#authenticateUser(com.ibm.ws.security.authentication.AuthenticationService, java.lang.String, java.lang.String)}
     * .
     * 
     * should use SYSTEM_DEFAULT when the jaas entry name is not passed in
     */
    @Test
    public void testAuthenticateUser() throws Exception {
        mockery.checking(new Expectations() {
            {
                allowing(authnService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(true));
            }
        });
        final Subject partialSubject = authnHelper.createPartialSubject(userName, authnService, null);
        mockery.checking(new Expectations() {
            {
                allowing(authnService).authenticate(jaasEntryName, partialSubject);
                will(returnValue(authenticatedSubject));
            }
        });
        assertNotNull("There must be an authenticated subject", authnHelper.authenticateUser(authnService, userName, jaasEntryName));
    }

    /**
     * Test method for
     * {@link com.ibm.ws.security.authentication.helper.AuthenticateUserHelper#createPartialSubject(java.lang.String, com.ibm.ws.security.authentication.AuthenticationService)}.
     */
    @Test
    public void testCreatePartialSubjectAddHashtableProperty() {
        mockery.checking(new Expectations() {
            {
                allowing(authnService).isAllowHashTableLoginWithIdOnly();
                will(returnValue(false));
            }
        });
        assertNotNull("There must be an authenticated subject", authnHelper.createPartialSubject(userName, authnService, null));
    }
}
