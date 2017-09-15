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

import java.util.Hashtable;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.security.authentication.cache.AuthCacheConfig;
import com.ibm.ws.security.authentication.cache.CacheContext;
import com.ibm.ws.security.authentication.cache.CacheKeyProvider;
import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.wsspi.security.token.AttributeNameConstants;
import com.ibm.wsspi.security.token.SingleSignonToken;

/**
 *
 */
public class CustomCacheKeyProviderTest {

    private static SharedOutputManager outputMgr;
    private static Mockery mockery = new JUnit4Mockery();
    static AuthCacheConfig config = mockery.mock(AuthCacheConfig.class);
    private static SingleSignonToken ssoToken;
    private static String ssoTokenContents = "testUser";
    private static Subject testSubject;
    private static CacheContext cacheContext;
    private static String CUSTOM_CACHE_KEY = "customCacheKey";

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
        testSubject = new Subject();
        CacheObject cacheObject = new CacheObject(testSubject);
        cacheContext = new CacheContext(config, cacheObject);
    }

    private static SingleSignonToken createSSOToken() {
        final SingleSignonToken ssoToken = mockery.mock(SingleSignonToken.class);
        mockery.checking(new Expectations() {
            {
                allowing(ssoToken).addAttribute(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
                allowing(ssoToken).getBytes();
                will(returnValue(ssoTokenContents.getBytes()));
            }
        });
        ssoToken.addAttribute(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
        return ssoToken;
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

    /**
     * @return
     */
    private Subject createTestSubjectWithCustomCacheKey() {
        Hashtable<String, Object> hashtable = new Hashtable<String, Object>();
        hashtable.put(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY, CUSTOM_CACHE_KEY);
        Subject subject = new Subject();
        subject.getPublicCredentials().add(hashtable);
        return subject;
    }

    @Test
    public void testConstructor() {
        final String methodName = "testConstructor";
        try {
            CacheKeyProvider provider = new CustomCacheKeyProvider();
            assertNotNull("There must be a custom cache key provider.", provider);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testProvideKeyFromHashtable() {
        final String methodName = "testProvideKeyFromHashtable";
        Subject subject = createTestSubjectWithCustomCacheKey();
        CacheObject cacheObject = new CacheObject(subject);
        cacheContext = new CacheContext(config, cacheObject);
        try {
            CacheKeyProvider provider = new CustomCacheKeyProvider();
            Object cacheKey = provider.provideKey(cacheContext);
            assertEquals("The key must be the string customCacheKey.", CUSTOM_CACHE_KEY, cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }

    @Test
    public void testProvideKeyFromSSOToken() {
        final String methodName = "testProvideKeyFromSSOToken";
        addSSOTokenToSubject(testSubject);
        final String[] customCacheKey = { CUSTOM_CACHE_KEY };
        try {
            CacheKeyProvider provider = new CustomCacheKeyProvider();
            mockery.checking(new Expectations() {
                {
                    allowing(ssoToken).getAttributes(AttributeNameConstants.WSCREDENTIAL_CACHE_KEY);
                    will(returnValue(customCacheKey));
                }
            });

            Object cacheKey = provider.provideKey(cacheContext);
            assertEquals("The key must be the string customCacheKey.", CUSTOM_CACHE_KEY, cacheKey);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(methodName, t);
        }
    }
}
