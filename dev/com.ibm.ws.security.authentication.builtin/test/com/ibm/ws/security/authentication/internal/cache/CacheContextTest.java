/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.security.auth.Subject;

import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheObject;

/**
 *
 */
public class CacheContextTest {

    private static SharedOutputManager outputMgr;
    private static Mockery mockery = new JUnit4Mockery();
    private static Subject testSubject;
    private static AuthCacheConfig config;
    private static CacheContext simpleCacheContext;
    private static CacheContext cacheContextWithUseridPassword;
    private static String testUser = "user1";
    private static String testPassword = "user1pwd";

    /**
     * Capture stdout/stderr output to the manager.
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // There are variations of this constructor: 
        // e.g. to specify a log location or an enabled trace spec. Ctrl-Space for suggestions
        outputMgr = SharedOutputManager.getInstance();
        outputMgr.captureStreams();

        testSubject = new Subject();
        CacheObject cacheObject = new CacheObject(testSubject);
        config = mockery.mock(AuthCacheConfig.class);
        simpleCacheContext = new CacheContext(config, cacheObject);
        cacheContextWithUseridPassword = new CacheContext(config, cacheObject, testUser, testPassword);
    }

    /**
     * Final teardown work when class is exiting.
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // Make stdout and stderr "normal"
        outputMgr.restoreStreams();
    }

    /**
     * Individual teardown after each test.
     * 
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        // Clear the output generated after each method invocation
        outputMgr.resetStreams();
    }

    @Test
    public void testGetAuthCacheConfig_SimpleCacheContext() {
        final String methodName = "testGetAuthCacheConfig_SimpleCacheContext";
        try {
            AuthCacheConfig authCacheConfig = simpleCacheContext.getAuthCacheConfig();

            assertSame("The auth cache config must be found in the context.", config, authCacheConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_SimpleCacheContext() {
        final String methodName = "testGetSubject_SimpleCacheContext";
        try {
            Subject actualSubject = simpleCacheContext.getSubject();

            assertSame("The subject must be found in the context.", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetUserid_SimpleCacheContext() {
        final String methodName = "testGetUserid_SimpleCacheContext";
        try {
            String userid = simpleCacheContext.getUserid();

            assertNull("There must not be a userid in the context.", userid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPassword_SimpleCacheContext() {
        final String methodName = "testGetPassword_SimpleCacheContext";
        try {
            String password = simpleCacheContext.getPassword();

            assertNull("There must not be a password in the context.", password);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetAuthCacheConfig_CacheContextWithUseridPassword() {
        final String methodName = "testGetAuthCacheConfig_CacheContextWithUseridPassword";
        try {
            AuthCacheConfig authCacheConfig = cacheContextWithUseridPassword.getAuthCacheConfig();

            assertSame("The auth cache config must be found in the context.", config, authCacheConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetSubject_CacheContextWithUseridPassword() {
        final String methodName = "testGetSubject_CacheContextWithUseridPassword";
        try {
            Subject actualSubject = cacheContextWithUseridPassword.getSubject();

            assertSame("The subject must be found in the context.", testSubject, actualSubject);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetUserid_CacheContextWithUseridPassword() {
        final String methodName = "testGetUserid_CacheContextWithUseridPassword";
        try {
            String userid = cacheContextWithUseridPassword.getUserid();

            assertEquals("There must be a userid in the context.", testUser, userid);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetPassword_CacheContextWithUseridPassword() {
        final String methodName = "testGetPassword_CacheContextWithUseridPassword";
        try {
            String password = cacheContextWithUseridPassword.getPassword();

            assertEquals("There must be a password in the context.", testPassword, password);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
