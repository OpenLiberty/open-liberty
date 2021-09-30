/*******************************************************************************
 * Copyright (c) 2015, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.collector.manager.buffer;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 *
 */
public class RingBufferTest {

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
    public void testRingBufferIllegalInstantiation() {
        new Buffer<Integer>(0);
    }

    @Test
    public void testRingBuffer() {
        final String m = "testRingBuffer";

        final int capacity = 5;
        final int excessElements = 2;
        final int noOfEvents = capacity + excessElements;

        Buffer<Integer> buffer = new Buffer<Integer>(capacity);
        long result = 0;
        long expectedResult = 0;

        try {
            //Add elements so that the buffer wraps
            for (int i = 0; i < noOfEvents; i++) {
                buffer.add(new Integer(i));
            }

            //Check if the sequence numbers are correct
            result = (Long) getField(buffer, "currentSeqNum");
            expectedResult = 8;
            Assert.assertEquals("Current sequence number is not as expected." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);

            result = (Long) getField(buffer, "earliestSeqNum");
            expectedResult = 3;
            Assert.assertEquals("Earliest sequence number is not as expected." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRingBufferWrapMoreThanOnce() {
        final String m = "testRingBufferWrapMoreThanOnce";

        final int capacity = 5;
        final int excessElements = 10;
        final int noOfEvents = capacity + excessElements;

        Buffer<Integer> buffer = new Buffer<Integer>(capacity);
        long result = 0;
        long expectedResult = 0;

        try {
            //Add elements so that the buffer wraps
            for (int i = 0; i < noOfEvents; i++) {
                buffer.add(new Integer(i));
            }

            //Check if the sequence numbers are correct
            result = (Long) getField(buffer, "currentSeqNum");
            expectedResult = 16;
            Assert.assertEquals("Current sequence number is not as expected." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);

            result = (Long) getField(buffer, "earliestSeqNum");
            expectedResult = 11;
            Assert.assertEquals("Earliest sequence number is not as expected." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRingBufferGet() {
        final String m = "testRingBufferGet";

        final int capacity = 5;
        final int excessElements = 2;
        final int noOfEvents = capacity + excessElements;

        Buffer<Integer> buffer = new Buffer<Integer>(capacity);
        Event<Integer> result = null;
        Event<Integer> expectedResult = null;

        try {
            //Add elements so that the buffer wraps
            for (int i = 0; i < noOfEvents; i++) {
                buffer.add(new Integer(i));
            }

            //Fetch a sequence number that has been overwritten
            //This should return an element corresponding to the earliest
            //sequence number
            result = buffer.get(1);
            expectedResult = new Event<Integer>(3, 2);
            Assert.assertEquals("Get on sequence number 1 did not return expected results." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);

            //Fetch the element corresponding to earliest sequence number
            result = buffer.get(3);
            expectedResult = new Event<Integer>(3, 2);
            Assert.assertEquals("Get on sequence number 3 did not return expected results." + "\nResult = " + result
                                + "\nExpected result = " + expectedResult,
                                result, expectedResult);

            //Fetch the element corresponding to latest sequence number
            result = buffer.get(7);
            expectedResult = new Event<Integer>(7, 6);
            Assert.assertEquals("Get on sequence number 7 did not return expected results." + "Result = " + result
                                + "Expected result = " + expectedResult,
                                result, expectedResult);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRingBufferBulkGet() {
        final String m = "testRingBufferBulkGet";

        final int capacity = 5;
        final int excessElements = 2;
        final int noOfEvents = capacity + excessElements;

        Buffer<Integer> buffer = new Buffer<Integer>(capacity);
        ArrayList<Event<Integer>> result = null;
        ArrayList<Event<Integer>> expectedResult = null;

        try {
            //Add elements so that the buffer wraps
            for (int i = 0; i < noOfEvents; i++) {
                buffer.add(new Integer(i));
            }

            //Fetch the elements starting from earliest sequence number
            result = buffer.get(3, 2);
            expectedResult = new ArrayList<Event<Integer>>() {
                {
                    add(new Event<Integer>(3, 2));
                    add(new Event<Integer>(4, 3));
                }
            };
            assertTrue("Bulk get with sequence number 3 and number of elements as 2 did not return expected results."
                       + "\nResult = " + result
                       + "\nExpected result = " + expectedResult,
                       result.containsAll(expectedResult));

            //Fetch more elements than what is available in the buffer
            result = buffer.get(5, 10);
            expectedResult = new ArrayList<Event<Integer>>() {
                {
                    add(new Event<Integer>(5, 4));
                    add(new Event<Integer>(6, 5));
                    add(new Event<Integer>(7, 6));
                }
            };
            assertTrue("Bulk get with sequence number 5 and number of elements as 10 did not return expected results."
                       + "\nResult = " + result
                       + "\nExpected result = " + expectedResult,
                       result.containsAll(expectedResult));

            //Fetch elements so that we test the wrap around functionality of bulk get
            result = buffer.get(4, 3);
            expectedResult = new ArrayList<Event<Integer>>() {
                {
                    add(new Event<Integer>(4, 3));
                    add(new Event<Integer>(5, 4));
                    add(new Event<Integer>(6, 5));
                }
            };
            assertTrue("Bulk get with sequence number 4 and number of elements as 3 did not return expected results."
                       + "\nResult = " + result
                       + "\nExpected result = " + expectedResult,
                       result.containsAll(expectedResult));
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        }
    }

    @Test
    public void testRingBufferWaitOnEmpty() {
        final String m = "bufferGetWaitOnEmptyBuffer";

        final int capacity = 2;
        final int seqNum = 1;

        final Buffer<Integer> buffer = new Buffer<Integer>(capacity);

        //Create a buffer with no elements, a get on this should block
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buffer.get(seqNum);
                } catch (InterruptedException e) {

                }
            }
        });
        thread.start();

        try {
            assertBlocked("Get on empty buffer failed to block", thread);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            cleanUp(thread, true);
        }
    }

    @Test
    public void testRingBufferWaitIfNotAvailable() {
        final String m = "testRingBufferWaitIfNotAvailable";

        final int capacity = 5;
        final int noOfEventsToAdd = 3;
        final int seqNum = 4;

        final Buffer<Integer> buffer = new Buffer<Integer>(capacity);

        //Add elements to the buffer and try to retrieve a sequence number
        //greater than the latest sequence number, a get with this sequence number should block
        for (int i = 0; i < noOfEventsToAdd; i++) {
            buffer.add(new Integer(i));
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buffer.get(seqNum);
                } catch (InterruptedException e) {

                }
            }
        });
        thread.start();

        try {
            assertBlocked("Get on future sequence number failed to block.", thread);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            cleanUp(thread, true);
        }
    }

    @Test
    public void testRingBufferWaitTillAvailable() {
        final String m = "testRingBufferWaitTillAvailable";

        final int capacity = 5;
        final int seqNum = 1;

        final Buffer<Integer> buffer = new Buffer<Integer>(capacity);

        //Create a buffer with no elements, a get on this should block
        //Add an element, this should notify the waiting thread
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    buffer.get(seqNum);
                } catch (InterruptedException t) {
                    //Check to see if we're swallowing an interruption
                    t.printStackTrace();
                }
            }
        });
        thread.start();

        try {
            assertBlocked("Get on empty buffer failed to block", thread);
            //Now add an element to the buffer
            buffer.add(new Integer(0));
            assertNotBlocked("Get failed to unblock on adding an element", thread);
        } catch (Throwable t) {
            outputMgr.failWithThrowable(m, t);
        } finally {
            cleanUp(thread, true);
        }
    }

    public void assertBlocked(String message, Thread thread) {

        long threadWaitTimeOutInMilliSecs = 40000;
        long waitTimeInMilliSecs = 4000;
        long timeElapsedInMilliSecs = 0;

        System.out.println("Waiting for thread to enter either blocked or waiting state... Current state " + thread.getState());
        while (!(thread.getState() == Thread.State.WAITING || thread.getState() == Thread.State.BLOCKED)
               && timeElapsedInMilliSecs < threadWaitTimeOutInMilliSecs) {
            try {
                Thread.sleep(waitTimeInMilliSecs);
            } catch (InterruptedException e) {
                //Nothing to do here!!
            }
            timeElapsedInMilliSecs += waitTimeInMilliSecs;
            System.out.println("Thread id : " + thread.getId() + ", wait time (ms) : " + timeElapsedInMilliSecs + ", state : " + thread.getState());
        }
        assertTrue(message + ". state: " + thread.getState(), thread.getState() == Thread.State.WAITING || thread.getState() == Thread.State.BLOCKED);
    }

    public void assertNotBlocked(String message, Thread thread) {

        long threadWaitTimeOutInMilliSecs = 70000;//originally 10 seconds (i.e. 10000)
        long waitTimeInMilliSecs = 1000;
        long timeElapsedInMilliSecs = 0;

        System.out.println("Waiting for thread to enter either runnable or terminated state... Current state " + thread.getState());
        while (!(thread.getState() == Thread.State.RUNNABLE || thread.getState() == Thread.State.TERMINATED)
               && timeElapsedInMilliSecs < threadWaitTimeOutInMilliSecs) {
            try {
                Thread.sleep(waitTimeInMilliSecs);
            } catch (InterruptedException e) {
                //Check to see if we are swallowing an interruption.
                e.printStackTrace();
            }
            timeElapsedInMilliSecs += waitTimeInMilliSecs;
            System.out.println("Thread id : " + thread.getId() + ", wait time (ms) : " + timeElapsedInMilliSecs + ", state : " + thread.getState());
        }
        assertTrue(message + ". state: " + thread.getState(), thread.getState() == Thread.State.RUNNABLE || thread.getState() == Thread.State.TERMINATED);

        /*
         * For tests that passed (i.e. unblocked) after the original 10 seconds.
         * We want it to fail so that we can record/see how long it took for
         * problematic systems.
         */
        assertTrue("This test took longer than 10 seconds to \"pass\". It took " + timeElapsedInMilliSecs + " instead.", timeElapsedInMilliSecs <= 10000);
    }

    //Utility methods
    public Object getField(Object instance, String fieldName) throws Throwable {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    public void cleanUp(Thread thread, boolean isInterruptable) {

        long threadWaitTimeOutInMilliSecs = 10000;

        if (isInterruptable)
            thread.interrupt();
        try {
            thread.join(threadWaitTimeOutInMilliSecs);
        } catch (InterruptedException e) {

        }
    }
}
