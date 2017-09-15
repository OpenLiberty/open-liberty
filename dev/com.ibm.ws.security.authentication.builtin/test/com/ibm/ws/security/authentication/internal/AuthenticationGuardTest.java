/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.locks.ReentrantLock;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.AuthenticationData;
import com.ibm.ws.security.authentication.WSAuthenticationData;

/**
 *
 */
public class AuthenticationGuardTest {

    private static SharedOutputManager outputMgr;
    private final Mockery mockery = new JUnit4Mockery();

    private final AuthenticationData user1Data = createAuthenticationData("user1", "user1pwd");
    private final AuthenticationData user2Data = createAuthenticationData("user2", "user2pwd");

    @Rule
    public TestName testName = new TestName();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();
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

    @Test
    public void requestAccess_sameDataReturnsSameLock() {
        AuthenticationGuard guard = new AuthenticationGuard();
        ReentrantLock firstLock = guard.requestAccess(user1Data);
        ReentrantLock secondLock = guard.requestAccess(user1Data);
        assertEquals("The locks must be the same for equal authentication data.", firstLock, secondLock);
    }

    @Test
    public void requestAccess_differentDataReturnsDifferentLocks() {
        AuthenticationGuard guard = new AuthenticationGuard();
        ReentrantLock firstLock = guard.requestAccess(user1Data);
        ReentrantLock secondLock = guard.requestAccess(user2Data);
        assertFalse("The locks must be different for different authentication data.", firstLock.equals(secondLock));
    }

    @Test
    public void requestAccess_sameDataAfterRelinquishingReturnsDifferentLock() {
        AuthenticationGuard guard = new AuthenticationGuard();
        ReentrantLock firstLock = guard.requestAccess(user1Data);
        firstLock.lock();
        guard.relinquishAccess(user1Data, firstLock);
        ReentrantLock secondLock = guard.requestAccess(user1Data);
        assertFalse("The locks must be different for different authentication data.", firstLock.equals(secondLock));
    }

    private AuthenticationData createAuthenticationData(String username, String password) {
        AuthenticationData authenticationData = new WSAuthenticationData();
        authenticationData.set(AuthenticationData.USERNAME, username);
        authenticationData.set(AuthenticationData.PASSWORD, password.toCharArray());
        return authenticationData;
    }

}
