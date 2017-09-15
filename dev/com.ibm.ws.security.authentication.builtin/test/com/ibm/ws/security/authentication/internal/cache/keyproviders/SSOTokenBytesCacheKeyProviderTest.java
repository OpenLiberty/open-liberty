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
package com.ibm.ws.security.authentication.internal.cache.keyproviders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class SSOTokenBytesCacheKeyProviderTest {

    private static SharedOutputManager outputMgr;
    private static Mockery mockery = new JUnit4Mockery();
    private static SingleSignonToken ssoToken;
    private static String ssoTokenContents = "testUser";
    private static Subject testSubject;
    private static CacheContext cacheContext;
    private static AuthCacheConfig authCacheConfig;

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

        ssoToken = createSSOToken();
        testSubject = createTestSubject();
        CacheObject cacheObject = new CacheObject(testSubject);
        authCacheConfig = mockery.mock(AuthCacheConfig.class);
        cacheContext = new CacheContext(authCacheConfig, cacheObject);
    }

    private static SingleSignonToken createSSOToken() {
        final SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class);
        mockery.checking(new Expectations() {
            {
                allowing(ssoToken).getBytes();
                will(returnValue(ssoTokenContents.getBytes()));
            }
        });
        return ssoToken;
    }

    private static Subject createTestSubject() throws Exception {
        Subject subject = new Subject();
        addSSOTokenToSubject(subject);
        return subject;
    }

    private static void addSSOTokenToSubject(Subject subject) {
        subject.getPrivateCredentials().add(ssoToken);
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
            CacheKeyProvider provider = new SSOTokenBytesCacheKeyProvider();
            assertNotNull("There must be an SSO token bytes cache key provider.", provider);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testProvideKey() {
        final String methodName = "testProvideKey";
        try {
            CacheKeyProvider provider = new SSOTokenBytesCacheKeyProvider();
            Object cacheKey = provider.provideKey(cacheContext);
            String ssoTokenCacheKey = Base64Coder.base64Encode(ssoTokenContents);
            assertEquals("The key must be the SSO token bytes.", ssoTokenCacheKey, cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testProvideKeyWithNoTokenReturnsNull() {
        final String methodName = "testProvideKeyWithNoTokenReturnsNull";
        try {
            CacheKeyProvider provider = new SSOTokenBytesCacheKeyProvider();
            CacheObject cacheObject = new CacheObject(new Subject());
            CacheContext cacheContext = new CacheContext(authCacheConfig, cacheObject);
            Object cacheKey = provider.provideKey(cacheContext);
            assertNull("There must not be a key.", cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
