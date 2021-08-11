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

public class CacheEntryTest extends CommonTestClass {

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
            CacheEntry entry1 = new CacheEntry(value);
            // Ensure the createdAt time for each is different
            Thread.sleep(100);
            CacheEntry entry2 = new CacheEntry(value);

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
            CacheEntry entry1 = new CacheEntry(value1);
            CacheEntry entry2 = new CacheEntry(value2);

            assertFalse("Entry [" + entry1 + "] was considered equal to [" + entry2 + "].", entry1.equals(entry2));
            assertFalse("Entry [" + entry2 + "] was considered equal to [" + entry1 + "].", entry2.equals(entry1));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_timeoutZero() {
        try {
            String value = "value";
            CacheEntry entry = new CacheEntry(value);
            long timeout = 0;

            Thread.sleep(10);

            boolean result = entry.isExpired(timeout);

            assertTrue("Entry should have been considered expired, but wasn't. Entry created at [" + entry.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_shortTimeout_waitToExpire() {
        try {
            String value = "value";
            CacheEntry entry = new CacheEntry(value);
            long timeout = 50;

            Thread.sleep(100);

            boolean result = entry.isExpired(timeout);

            assertTrue("Entry should have been considered expired, but wasn't. Entry created at [" + entry.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_isExpired_longTimeout() {
        try {
            String value = "value";
            CacheEntry entry = new CacheEntry(value);
            long timeout = 1000 * 60;

            boolean result = entry.isExpired(timeout);

            assertFalse("Entry should not have been considered expired yet, but was. Entry created at [" + entry.getCreatedAt() + "]. Current time: [" + System.currentTimeMillis() + "].", result);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
