/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import java.util.Arrays;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.ws.collector.manager.buffer.BufferManagerImpl;

import test.common.SharedOutputManager;

/**
 *
 */
public class BufferManagerTest {

    private static SharedOutputManager outputMgr;

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

    @Test(expected = IllegalArgumentException.class)
    public void testGetAddRemoveHandler() {
        final String m = "testGetAddRemoveHandler";

        final int capacity = 5;
        final String handlerId = "testHandler";

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Register a handler with the buffer manager
            //Get using this handler should return elements added to the buffer
            bufferMgr.addHandler(handlerId);

            Integer expectedResult = new Integer(0);
            bufferMgr.add(expectedResult);
            Integer result = (Integer) bufferMgr.getNextEvent(handlerId);

            Assert.assertEquals("Get on buffer manager using a registered handler returned unexpected results \nResult = " + result
                                + "\nExpected result = " + expectedResult, expectedResult, result);

            //Now de-register the handler
            //Get using this handler should throw an exception
            bufferMgr.removeHandler(handlerId);
            expectedResult = null;
            result = (Integer) bufferMgr.getNextEvent(handlerId);
        } catch (IllegalArgumentException e){   
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBulkGetAddRemoveHandler() {
        final String m = "testBulkGetAddRemoveHandler";

        final int capacity = 5;
        final String handlerId = "testHandler";
        final int noOfEvents = 2;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Register a handler with the buffer manager
            //Bulk get using this handler should return elements added to the buffer
            bufferMgr.addHandler(handlerId);

            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }

            Object[] expectedResult = new Integer[] { new Integer(0), new Integer(1) };
            Object[] result = bufferMgr.getEvents(handlerId, noOfEvents);

            Assert.assertArrayEquals("Bulk get on buffer manager using registered handler returned unexpected results \nResult = " + result
                                     + "\nExpected result = " + expectedResult, expectedResult, result);

            //Now de-register the handler
            //Get using this handler should throw an exception
            bufferMgr.removeHandler(handlerId);
            expectedResult = null;
            result = bufferMgr.getEvents(handlerId, noOfEvents);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testGet() {
        final String m = "testGet";

        final int capacity = 5;
        final String handlerId = "testHandler";
        final int noOfEvents = 3;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Register a handler with the buffer manager
            //Get using this handler should return elements added to the buffer
            bufferMgr.addHandler(handlerId);

            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }

            for (int i = 0; i < noOfEvents; i++) {
                Integer result = (Integer) bufferMgr.getNextEvent(handlerId);
                Integer expectedResult = new Integer(i);
                Assert.assertEquals("Get on buffer manager returned unexpected results \nResult = " + result
                                    + "\nExpected result = " + expectedResult, expectedResult, result);

            }

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testBulkGet() {
        final String m = "testBulkGet";

        final int capacity = 5;
        final String handlerId = "testHandler";
        final int excessElements = 2;
        final int noOfEvents = capacity + excessElements;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Register a handler with the buffer manager
            //Bulk get using this handler should return elements added to the buffer
            bufferMgr.addHandler(handlerId);

            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }

            //Retrieve first two elements
            Object[] result = bufferMgr.getEvents(handlerId, 2);
            Object[] expectedResult = new Integer[] { new Integer(2), new Integer(3) };
            Assert.assertArrayEquals("Bulk get on buffer manager returned unexpected results \nResult = " + Arrays.toString(result)
                                     + "\nExpected result = " + Arrays.toString(expectedResult), expectedResult, result);

            //Retrieve remaining elements
            result = bufferMgr.getEvents(handlerId, 5);
            expectedResult = new Integer[] { new Integer(4), new Integer(5), new Integer(6) };
            Assert.assertArrayEquals("Bulk get on buffer manager returned unexpected results \nResult = " + Arrays.toString(result)
                                     + "\nExpected result = " + Arrays.toString(expectedResult), expectedResult, result);

        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetWithoutAdd() {
        final String m = "testGetWithoutAdd";

        final int capacity = 5;
        final String handlerId = "testHandler";
        final int noOfEvents = 2;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Fetch an element using a non existent handler id
            //This should throw an exception
            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }
            Integer result = (Integer) bufferMgr.getNextEvent(handlerId);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBulkGetWithoutAdd() {
        final String m = "testBulkGetWithoutAdd";

        final int capacity = 5;
        final String handlerId = "testHandler";
        final int noOfEvents = 2;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Fetch an element using a non existent handler id
            //This should throw an exception
            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }
            Object[] result = bufferMgr.getEvents(handlerId, 2);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testMultipleGet() {
        final String m = "testMutilpeGet";

        final int capacity = 5;
        final String handlerId1 = "testHandler1";
        final String handlerId2 = "testHandler2";
        final int excessElements = 2;
        final int noOfEvents = capacity + excessElements;

        BufferManagerImpl bufferMgr = new BufferManagerImpl(capacity, "testSource");

        try {
            //Register multiple handlers and check if their get results are
            //consistent with what is expected.
            bufferMgr.addHandler(handlerId1);
            bufferMgr.addHandler(handlerId2);

            for (int i = 0; i < noOfEvents; i++) {
                bufferMgr.add(new Integer(i));
            }

            Integer result1 = null, result2 = null;
            Integer expectedResult1 = new Integer(6);
            Integer expectedResult2 = new Integer(4);
            for (int i = 0; i < capacity; i++) {
                result1 = (Integer) bufferMgr.getNextEvent(handlerId1);
                if (i % 2 == 0) {
                    result2 = (Integer) bufferMgr.getNextEvent(handlerId2);
                }
            }

            Assert.assertEquals("Get on buffer manager returned unexpected results \nResult = " + result1
                                + "\nExpected result = " + expectedResult1, result1, expectedResult1);

            Assert.assertEquals("Get on buffer manager returned unexpected results \nResult = " + result2
                                + "\nExpected result = " + expectedResult2, result2, expectedResult2);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }
}
