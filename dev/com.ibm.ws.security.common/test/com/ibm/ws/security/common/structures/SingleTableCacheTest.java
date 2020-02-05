/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.security.test.common.CommonTestClass;

import test.common.SharedOutputManager;

public class SingleTableCacheTest extends CommonTestClass {

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
    public void test_constructor_zeroTimeout() {
        try {
            SingleTableCache cache = new SingleTableCache(0);
            assertEquals("Cache size did not equal the expected value.", 50000, cache.size());
            assertEquals("Cache timeout duration did not equal the expected value.", 5 * 60 * 1000, cache.getTimeoutInMilliseconds());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_zeroSizeAndTimeout() {
        try {
            SingleTableCache cache = new SingleTableCache(0, 0);
            assertEquals("Cache size did not equal the expected value.", 50000, cache.size());
            assertEquals("Cache timeout duration did not equal the expected value.", 5 * 60 * 1000, cache.getTimeoutInMilliseconds());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_constructor_nonDefaultValues() {
        try {
            int size = 42;
            long timeout = 123456789;
            SingleTableCache cache = new SingleTableCache(size, timeout);
            assertEquals("Cache size did not equal the expected value.", size, cache.size());
            assertEquals("Cache timeout duration did not equal the expected value.", timeout, cache.getTimeoutInMilliseconds());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_minimumSize_singleEntry_notExpired() {
        try {
            SingleTableCache cache = new SingleTableCache(1, 1000);
            assertEquals("Cache size did not equal the expected value.", 1, cache.size());

            String key = "key";
            String value = "value";

            cache.put(key, value);
            assertEquals("Cache size did not equal the expected value.", 1, cache.size());

            String returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_minimumSize_multipleEntries() {
        try {
            SingleTableCache cache = new SingleTableCache(1, 1000);
            String key1 = "key1";
            String value1 = "value1";
            String key2 = "key2";
            String value2 = "value2";
            String key3 = "key3";
            String value3 = "value3";

            cache.put(key1, value1);
            assertEquals("Cache size did not equal the expected value after adding an entry.", 1, cache.size());
            cache.put(key2, value2);
            assertEquals("Cache size did not equal the expected value after adding a second entry.", 1, cache.size());
            cache.put(key3, value3);
            assertEquals("Cache size did not equal the expected value after adding a third entry.", 1, cache.size());

            String returnedValue = (String) cache.get(key1);
            assertNull("Should not have found an entry for " + key1 + " but did. Entry was [" + returnedValue + "].", returnedValue);
            returnedValue = (String) cache.get(key2);
            assertNull("Should not have found an entry for " + key2 + " but did. Entry was [" + returnedValue + "].", returnedValue);
            returnedValue = (String) cache.get(key3);
            assertEquals("Returned value did not match the inputted value.", value3, returnedValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_singleEntry_allowEntryToExpire() {
        try {
            int timeout = 1000;
            SingleTableCache cache = new SingleTableCache(10, timeout);

            String key = "key";
            String value = "value";

            cache.put(key, value);
            String returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            Thread.sleep((long) (timeout * 1.5));

            returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + " after waiting, but did. Entry was [" + returnedValue + "].", returnedValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_remove_existingEntry() {
        try {
            SingleTableCache cache = new SingleTableCache(10, 1000);

            String key = "key";
            String value = "value";

            cache.put(key, value);
            String returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            cache.remove(key);

            returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + " after removing, but did. Entry was [" + returnedValue + "].", returnedValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_remove_noEntries() {
        try {
            SingleTableCache cache = new SingleTableCache(10, 1000);

            String key = "key";

            String returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + ", but did. Entry was [" + returnedValue + "].", returnedValue);

            cache.remove(key);

            returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + " after removing, but did. Entry was [" + returnedValue + "].", returnedValue);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_rescheduleCleanup_multipleReschedules() {
        try {
            long timeout = 500;
            SingleTableCache cache = new SingleTableCache(10, timeout);
            assertEquals("Cache timeout duration did not equal the expected value.", timeout, cache.getTimeoutInMilliseconds());

            cache.rescheduleCleanup(-1);
            assertEquals("Cache timeout duration did not equal the original value.", timeout, cache.getTimeoutInMilliseconds());

            long newTimeout = 123456;
            cache.rescheduleCleanup(newTimeout);
            assertEquals("Cache timeout duration did not equal the new value.", newTimeout, cache.getTimeoutInMilliseconds());
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

    @Test
    public void test_rescheduleCleanup() {
        try {
            long originalTimeout = 100;
            long newTimeout = originalTimeout * 3;
            SingleTableCache cache = new SingleTableCache(10, originalTimeout);

            String key = "key";
            String value = "value";

            cache.put(key, value);

            // Ensure we can repeatedly get the recently added value
            String returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);
            returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            // Wait for the timeout and verify that the cache is cleared
            Thread.sleep((long) (originalTimeout * 1.5));
            returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + " after waiting for the original timeout, but did. Entry was [" + returnedValue + "].", returnedValue);

            // Cache the value again
            cache.put(key, value);
            returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            cache.rescheduleCleanup(newTimeout);

            // Make sure the value remains in the cache even after the reschedule
            returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            // Wait the length of the original timeout and ensure the value is still cached
            Thread.sleep((long) (originalTimeout * 1.5));
            returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);
            returnedValue = (String) cache.get(key);
            assertEquals("Returned value did not match the inputted value.", value, returnedValue);

            // Wait the length of the new timeout and verify that the cache has been cleared
            Thread.sleep((long) (newTimeout * 1.5));
            returnedValue = (String) cache.get(key);
            assertNull("Should not have found an entry for " + key + " after waiting for the new timeout, but did. Entry was [" + returnedValue + "].", returnedValue);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(testName.getMethodName(), t);
        }
    }

}
