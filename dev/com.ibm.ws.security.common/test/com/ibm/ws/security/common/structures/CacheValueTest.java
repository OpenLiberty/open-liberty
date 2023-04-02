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
package com.ibm.ws.security.common.structures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class CacheValueTest extends CommonTestClass {

    private static SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("com.ibm.ws.security.common.*=all");

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        outputMgr.captureStreams();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        outputMgr.dumpStreams();
        outputMgr.restoreStreams();
    }

    @Before
    public void beforeTest() {
        System.out.println("Entering test: " + testName.getMethodName());
    }

    @After
    public void tearDown() throws Exception {
        System.out.println("Exiting test: " + testName.getMethodName());
        outputMgr.resetStreams();
    }

    @Test
    public void test_equals_identicalValues() {
        try {
            String value = "value";
            CacheValue value1 = createCacheValue(value, 0);
            // Ensure the createdAt time for each is different
            Thread.sleep(1000);
            CacheValue value2 = createCacheValue(value, 0);

            assertTrue("Value [" + value1 + "] was not considered equal to [" + value2 + "].", value1.equals(value2));
            assertTrue("Value [" + value2 + "] was not considered equal to [" + value1 + "].", value2.equals(value1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_equals_differentValues() {
        try {
            String value1 = "value1";
            String value2 = "value2";
            CacheValue cacheValue1 = createCacheValue(value1, 0);
            CacheValue cacheValue2 = createCacheValue(value2, 0);

            assertFalse("Value [" + cacheValue1 + "] was considered equal to [" + cacheValue2 + "].", cacheValue1.equals(cacheValue2));
            assertFalse("Value [" + cacheValue2 + "] was considered equal to [" + cacheValue1 + "].", cacheValue2.equals(cacheValue1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeout0s_clockSkew0s() {
        try {
            String value = "value";
            int clockSkew = 0;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 0;

            Thread.sleep(10);

            assertExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeout0s_clockSkewShort() {
        try {
            String value = "value";
            long clockSkew = 100;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 0;

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the clock skew
            Thread.sleep(clockSkew * 2);

            assertExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutShort_clockSkew0s() {
        try {
            String value = "value";
            int clockSkew = 0;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 50;

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the timeout
            Thread.sleep(timeout * 2);

            assertExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutShort_clockSkewShorter() {
        try {
            String value = "value";
            long clockSkew = 100;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 500;

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the clock skew
            Thread.sleep(clockSkew * 2);

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the timeout
            Thread.sleep(timeout);

            assertExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutShort_clockSkewLonger() {
        try {
            String value = "value";
            long clockSkew = 500;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 100;

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the timeout
            Thread.sleep(timeout * 2);

            assertNotExpired(cacheValue, timeout, clockSkew);

            // Sleep past the clock skew
            Thread.sleep(clockSkew);

            assertExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutLong_clockSkew0s() {
        try {
            String value = "value";
            int clockSkew = 0;
            CacheValue cacheValue = createCacheValue(value, clockSkew);
            long timeout = 1000 * 60;

            assertNotExpired(cacheValue, timeout, clockSkew);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    private CacheValue createCacheValue(Object value, long clockSkew) {
        CacheValue cacheValue;
        if (clockSkew > 0) {
            cacheValue = new CacheValue(value, clockSkew);
        } else {
            cacheValue = new CacheValue(value);
        }
        // Sleep the minimal amount to ensure subsequent System.currentTimeMillis() calls won't return the same time as the creation time
        // for the new cache value.
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            // Don't bother
        }
        return cacheValue;
    }

    private void assertExpired(CacheValue cacheValue, long timeout, long clockSkew) {
        assertResult(true, cacheValue, timeout, clockSkew, "Cache value should be considered expired once the timeout (+ clock skew) has passed.");
    }

    private void assertNotExpired(CacheValue cacheValue, long timeout, long clockSkew) {
        assertResult(false, cacheValue, timeout, clockSkew, "Cache value within the timeout and clock skew should not have been considered expired.");
    }

    private void assertResult(boolean expectedResult, CacheValue cacheValue, long timeout, long clockSkew, String failureMsgPrefix) {
        assertEquals(failureMsgPrefix + " Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Timeout: [" + timeout + "]. Clock skew: [" + clockSkew + "].", expectedResult, cacheValue.isExpired(timeout));
    }

}
