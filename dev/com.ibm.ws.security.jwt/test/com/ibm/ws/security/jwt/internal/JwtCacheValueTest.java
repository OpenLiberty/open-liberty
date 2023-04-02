/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jmock.Expectations;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.NumericDate;
import org.jose4j.jwt.consumer.JwtContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class JwtCacheValueTest extends CommonTestClass {

    private final SharedOutputManager outputMgr = SharedOutputManager.getInstance();

    private final JwtContext jwtContext = mockery.mock(JwtContext.class);
    private final JwtClaims jwtClaims = mockery.mock(JwtClaims.class);
    private final NumericDate date = mockery.mock(NumericDate.class);

    @Before
    public void setUp() throws Exception {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        mockery.assertIsSatisfied();
    }

    @Test
    public void test_isExpired_contextMissingClaims() {
        JwtCacheValue cacheValue = new JwtCacheValue(jwtContext, 0);
        long timeout = 5 * 1000;
        mockery.checking(new Expectations() {
            {
                one(jwtContext).getJwtClaims();
                will(returnValue(null));
            }
        });
        boolean result = cacheValue.isExpired(timeout);
        assertTrue("JWT with null claims object should have been considered expired, but was not. Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Current time: [" + System.currentTimeMillis() + "].", result);
    }

    @Test
    public void test_isExpired_malformedExpirationClaim() {
        JwtCacheValue cacheValue = new JwtCacheValue(jwtContext, 0);
        long timeout = 5 * 1000;
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(throwException(new MalformedClaimException(defaultExceptionMsg)));
                }
            });
            boolean result = cacheValue.isExpired(timeout);
            assertTrue("JWT with null claims object should have been considered expired, but was not. Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_jwtVeryOld() {
        long clockSkew = 60 * 1000;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 5 * 60 * 1000;
        long expTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * 365);
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(expTime));
                }
            });
            assertExpired(cacheValue, timeout, clockSkew, expTime, "JWT with very old 'exp' claim should have been considered expired.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeout0s_clockSkew0s() {
        long clockSkew = 0;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 0;
        try {
            assertExpired(cacheValue, timeout, clockSkew, Long.MAX_VALUE, "Cache value should be considered expired once the timeout (+ clock skew) has passed.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeout0s_clockSkewShort() {
        long clockSkew = 100;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 0;
        long expTime = System.currentTimeMillis();
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(expTime));
                }
            });
            assertNotExpired(cacheValue, timeout, clockSkew, expTime);

            // Sleep past the clockSkew
            Thread.sleep(clockSkew * 2);

            assertExpired(cacheValue, timeout, clockSkew, expTime, "Cache value should be considered expired once the timeout (+ clock skew) has passed.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutShort_clockSkew0s() {
        long clockSkew = 0;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 100;
        long expTime = System.currentTimeMillis() + (60 * 60 * 1000);
        try {
            mockery.checking(new Expectations() {
                {
                    one(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    one(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    one(date).getValueInMillis();
                    will(returnValue(expTime));
                }
            });
            assertNotExpired(cacheValue, timeout, clockSkew, expTime);

            // Sleep past the timeout
            Thread.sleep(timeout * 2);

            assertExpired(cacheValue, timeout, clockSkew, expTime, "Cache value should be considered expired once the timeout (+ clock skew) has passed.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutShort_clockSkewLonger() {
        long clockSkew = 500;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 100;
        long expTime = System.currentTimeMillis();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    allowing(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    allowing(date).getValueInMillis();
                    will(returnValue(expTime));
                }
            });
            assertNotExpired(cacheValue, timeout, clockSkew, expTime);

            // Sleep past the timeout
            Thread.sleep(timeout * 2);

            assertNotExpired(cacheValue, timeout, clockSkew, expTime);

            // Sleep past the clock skew
            Thread.sleep(clockSkew);

            assertExpired(cacheValue, timeout, clockSkew, expTime, "Cache value should be considered expired once the timeout (+ clock skew) has passed.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutLonger_clockSkewShort() {
        long clockSkew = 100;
        JwtCacheValue cacheValue = createJwtCacheValue(clockSkew);
        long timeout = 500;
        long expTime = System.currentTimeMillis();
        try {
            mockery.checking(new Expectations() {
                {
                    allowing(jwtContext).getJwtClaims();
                    will(returnValue(jwtClaims));
                    allowing(jwtClaims).getExpirationTime();
                    will(returnValue(date));
                    allowing(date).getValueInMillis();
                    will(returnValue(expTime));
                }
            });
            assertNotExpired(cacheValue, timeout, clockSkew, expTime);

            // Sleep past the clock skew
            Thread.sleep(clockSkew * 2);

            assertExpired(cacheValue, timeout, clockSkew, expTime, "JWT with 'exp' claim in the past beyond the clock skew should have been considered expired.");
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private JwtCacheValue createJwtCacheValue(long clockSkew) {
        JwtCacheValue cacheValue = new JwtCacheValue(jwtContext, clockSkew);
        // Sleep the minimal amount to ensure subsequent System.currentTimeMillis() calls won't return the same time as the creation time
        // for the new cache value.
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Don't bother
        }
        return cacheValue;
    }

    private void assertExpired(JwtCacheValue cacheValue, long timeout, long clockSkew, long jwtExpTime, String failureMsgPrefix) {
        assertResult(true, cacheValue, timeout, clockSkew, jwtExpTime, failureMsgPrefix);
    }

    private void assertNotExpired(JwtCacheValue cacheValue, long timeout, long clockSkew, long jwtExpTime) {
        assertResult(false, cacheValue, timeout, clockSkew, jwtExpTime, "Cache value within the timeout and with 'exp' claim within clock skew should not have been considered expired.");
    }

    private void assertResult(boolean expectedResult, JwtCacheValue cacheValue, long timeout, long clockSkew, long jwtExpTime, String failureMsgPrefix) {
        assertEquals(failureMsgPrefix + " Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Timeout: [" + timeout + "]. Clock skew: [" + clockSkew + "]. JWT 'exp' value: [" + jwtExpTime + "]. Current time: [" + System.currentTimeMillis() + "].", expectedResult, cacheValue.isExpired(timeout));
    }

}
