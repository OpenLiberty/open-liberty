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
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.cache.AuthCacheConfig;

/**
 *
 */
public class AuthCacheConfigTest {

    private static SharedOutputManager outputMgr;
    private static AuthCacheConfig authCacheConfig;

    private static int initialSize = 50;
    private static int maxSize = 25000;
    private static long timeout = 600;
    private final static boolean allowBasicAuthLookup = true;

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

        authCacheConfig = new AuthCacheConfigImpl(initialSize, maxSize, timeout, allowBasicAuthLookup);
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
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            assertNotNull("There must be an auth cache config.", authCacheConfig);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetInitialSize() {
        final String methodName = "testGetInitialSize";
        try {
            assertEquals("The initial size must be set.", initialSize, authCacheConfig.getInitialSize());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetMaxSize() {
        final String methodName = "testGetMaxSize";
        try {
            assertEquals("The max size must be set.", maxSize, authCacheConfig.getMaxSize());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testGetTimeout() {
        final String methodName = "testGetTimeout";
        try {
            assertEquals("The timeout must be set.", timeout, authCacheConfig.getTimeout());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testIsBasicAuthLookupAllowed() {
        final String methodName = "testIsBasicAuthLookupAllowed";
        try {
            assertEquals("The basic auth lookup flag must be set.", allowBasicAuthLookup, authCacheConfig.isBasicAuthLookupAllowed());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
