package com.ibm.ws.security.common.structures;

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
            CacheValue entry1 = new CacheValue(value);
            // Ensure the createdAt time for each is different
            Thread.sleep(100);
            CacheValue entry2 = new CacheValue(value);

            assertTrue("Entry [" + entry1 + "] was not considered equal to [" + entry2 + "].", entry1.equals(entry2));
            assertTrue("Entry [" + entry2 + "] was not considered equal to [" + entry1 + "].", entry2.equals(entry1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_equals_differentValues() {
        try {
            String value1 = "value1";
            String value2 = "value2";
            CacheValue cacheValue1 = new CacheValue(value1);
            CacheValue cacheValue2 = new CacheValue(value2);

            assertFalse("Value [" + cacheValue1 + "] was considered equal to [" + cacheValue2 + "].", cacheValue1.equals(cacheValue2));
            assertFalse("Value [" + cacheValue2 + "] was considered equal to [" + cacheValue1 + "].", cacheValue2.equals(cacheValue1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutZero() {
        try {
            String value = "value";
            CacheValue cacheValue = new CacheValue(value);
            long timeout = 0;

            Thread.sleep(10);

            boolean result = cacheValue.isExpired(timeout);

            assertTrue("Value should have been considered expired, but wasn't. Value created at [" + cacheValue.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_shortTimeout_waitToExpire() {
        try {
            String value = "value";
            CacheValue cacheValue = new CacheValue(value);
            long timeout = 50;

            Thread.sleep(100);

            boolean result = cacheValue.isExpired(timeout);

            assertTrue("Value should have been considered expired, but wasn't. Value created at [" + cacheValue.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_longTimeout() {
        try {
            String value = "value";
            CacheValue cacheValue = new CacheValue(value);
            long timeout = 1000 * 60;

            boolean result = cacheValue.isExpired(timeout);

            assertFalse("Value should not have been considered expired yet, but was. Value created at [" + cacheValue.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_clockSkew2s_timeoutZero() {
        try {
            String value = "value";
            long clockSkew = 2 * 1000;
            CacheValue cacheValue = new CacheValue(value, clockSkew);
            long timeout = 0;

            Thread.sleep(10);

            boolean result = cacheValue.isExpired(timeout);

            assertFalse("Value should not have been considered expired, but was. Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_clockSkew1s_timeout1s_waitBeyondTimeout_beforeClockSkew() {
        try {
            String value = "value";
            long clockSkew = 1000;
            CacheValue cacheValue = new CacheValue(value, clockSkew);
            long timeout = 1000;

            Thread.sleep(1050);

            boolean result = cacheValue.isExpired(timeout);

            assertFalse("Value should not have been considered expired, but was. Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_clockSkew1s_timeout1s_waitBeyondTimeoutAndClockSkew() {
        try {
            String value = "value";
            long clockSkew = 1000;
            CacheValue cacheValue = new CacheValue(value, clockSkew);
            long timeout = clockSkew;

            Thread.sleep(clockSkew + timeout + 100);

            boolean result = cacheValue.isExpired(timeout);

            assertTrue("Value should have been considered expired, but wasn't. Value created at [" + cacheValue.getCreatedAt() + "] (includes clock skew). Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
