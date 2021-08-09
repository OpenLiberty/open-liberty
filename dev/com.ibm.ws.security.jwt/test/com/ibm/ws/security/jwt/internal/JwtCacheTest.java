/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.jwt.config.JwtConsumerConfig;
import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JwtCacheTest extends CommonTestClass {

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final JwtConsumerConfig config = mockery.mock(JwtConsumerConfig.class);
    private final JwtContext jwtContext = mockery.mock(JwtContext.class);
    private final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);
    private final NumericDate date = mockery.mock(NumericDate.class);

    private JwtCache cache;

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
        cache = new JwtCache(1000 * 60, config);
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_evictStaleEntries_emptyCache() {
        // Ensure we don't throw an exception or anything bad
        cache.evictStaleEntries();
    }

    // TODO

    @Test
    public void test_isJwtExpired_contextMissingClaims() {
        mockery.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(null));
            }
        });
        boolean result = cache.isJwtExpired(jwtContext);
        assertTrue("JWT with null claims object should have been considered expired, but was not.", result);
    }

    @Test
    public void test_isJwtExpired_malformedExpirationClaim() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(throwException(new MalformedClaimException(defaultExceptionMsg)));
                }
            });
            boolean result = cache.isJwtExpired(jwtContext);
            assertTrue("JWT with null claims object should have been considered expired, but was not.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isJwtExpired_jwtVeryOld() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365)));
                    allowing(config).getClockSkew();
                    will(returnValue(1000L * 60 * 60));
                }
            });
            boolean result = cache.isJwtExpired(jwtContext);
            assertTrue("JWT with very old 'exp' claim should have been considered expired, but was not.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isJwtExpired_jwtExpiredButWithinClockSkew() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(System.currentTimeMillis() - (1000 * 60)));
                    allowing(config).getClockSkew();
                    will(returnValue(1000L * 60 * 60));
                }
            });
            boolean result = cache.isJwtExpired(jwtContext);
            assertFalse("JWT with old 'exp' claim still within the clock skew should not have been considered expired, but was.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isJwtExpired_jwtNotExpired() {
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(System.currentTimeMillis() + (1000 * 60 * 60 * 2)));
                    allowing(config).getClockSkew();
                    will(returnValue(1000L * 60 * 5));
                }
            });
            boolean result = cache.isJwtExpired(jwtContext);
            assertFalse("JWT with 'exp' claim in the future should not have been considered expired, but was.", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
