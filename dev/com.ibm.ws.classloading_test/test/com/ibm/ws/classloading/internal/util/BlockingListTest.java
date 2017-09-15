/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.classloading.internal.util;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.EMPTY_SET;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

/**
 * Test the blocking list blocks until a requested element is available.
 * Test the list times out if a timeout has been specified.
 * Test that the list degrades to a condensed list after a timeout.
 * Test that the iterators work consistently in the face of timeouts.
 */
public class BlockingListTest {
    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    @Test
    public void testCreateEmptyList() throws Exception {
        BlockingList<Integer, Void> bList = BlockingListMaker.defineList().make();
        assertFalse(bList.isTimedOut());
        assertEquals(EMPTY_LIST, bList);
        assertFalse(bList.iterator().hasNext());
        assertFalse(bList.isTimedOut());
    }

    @Test
    public void testCreateEmptyListWithTimeout() throws Exception {
        BlockingList<Integer, Void> bList = BlockingListMaker
                        .defineList()
                        .waitFor(2, SECONDS)
                        .make();
        assertFalse(bList.isTimedOut());
        assertEquals(EMPTY_LIST, bList);
        assertEquals(EMPTY_SET, bList.getUnmatchedKeys());
        assertFalse(bList.iterator().hasNext());
        assertFalse(bList.isTimedOut());
        assertEquals(EMPTY_SET, bList.getUnmatchedKeys());
    }

    @Test
    public void testTimedOutEmptyList() throws Exception {
        BlockingList<Integer, Void> bList = BlockingListMaker
                        .defineList()
                        .waitFor(2, NANOSECONDS)
                        .useKeys(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
                        .make();
        assertEquals(0, bList.size());
        assertTrue(bList.isTimedOut());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutElementInEmptyList() throws Exception {
        BlockingList<Integer, Void> bList = BlockingListMaker.defineList().make();
        assertFalse(bList.isTimedOut());
        bList.put(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testPutElementInEmptyListWithTimeout() throws Exception {
        BlockingList<Integer, Void> bList = BlockingListMaker.defineList().waitFor(2, MINUTES).make();
        assertFalse(bList.isTimedOut());
        bList.put(0, null);
        assertFalse(bList.isTimedOut());
    }

    @Test
    public void testSingleElementList() throws Exception {
        final int numThreads = 10;
        // use a cyclic barrier to ensure thread synchronization
        final CyclicBarrier eagerCounterBarrier = new CyclicBarrier(numThreads + 1);
        final CyclicBarrier lateCounterBarrier = new CyclicBarrier(numThreads + 1);
        final BlockingList<Integer, Integer> bList = BlockingListMaker.defineList().useKeys(0).make();
        assertFalse(bList.isTimedOut());
        assertEquals(set(0), bList.getUnmatchedKeys());
        assertEquals(EMPTY_LIST, bList.getCondensedList());
        assertFalse(bList.isTimedOut());
        List<FutureTask<Integer>> futures = new ArrayList<FutureTask<Integer>>(100);

        // these threads should just read the list
        for (int i = 0; i < numThreads; i++)
            futures.add(new FutureTask<Integer>(createAdder(bList)));

        // these threads should wait during iteration
        for (int i = 0; i < numThreads; i++)
            futures.add(new FutureTask<Integer>(createSynchronisedAdder(eagerCounterBarrier, bList, 1)));

        // these threads should wait for the list to be completed before iterating
        for (int i = 0; i < numThreads; i++)
            futures.add(new FutureTask<Integer>(createSynchronisedAdder(lateCounterBarrier, bList, 0)));

        // start all the threads
        for (FutureTask<?> future : futures)
            new Thread(future).start();

        bList.put(0, 42);
        assertEquals(EMPTY_SET, bList.getUnmatchedKeys());
        assertFalse(bList.isTimedOut());
        // wait for the eager counters to read the newly added element
        eagerCounterBarrier.await();
        assertFalse(bList.isTimedOut());
        // signal the late counters to start their work
        lateCounterBarrier.await();
        assertFalse(bList.isTimedOut());

        // now wait for and check all the results
        for (FutureTask<Integer> future : futures)
            assertEquals(42, (int) future.get());

        assertFalse(bList.isTimedOut());
        assertEquals(list(42), bList.getCondensedList());
        assertFalse(bList.isTimedOut());
        assertEquals(list(42), bList);
        assertFalse(bList.isTimedOut());
    }

    private Callable<Integer> createAdder(final BlockingList<Integer, Integer> bList) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int sum = 0;
                for (int elem : bList)
                    sum += elem;
                return sum;
            }
        };
    }

    private Callable<Integer> createSynchronisedAdder(final CyclicBarrier barrier, final BlockingList<Integer, Integer> bList, final int waitBeforeIndex) {
        return new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                int count = 0;
                int sum = 0;
                boolean awaitInvoked = false;
                try {
                    System.out.printf("%s: iterating through list%n", Thread.currentThread().getName());
                    for (int elem : bList) {
                        if (count == waitBeforeIndex) {
                            awaitInvoked = true;
                            System.out.printf("%s: waiting for barrier%n", Thread.currentThread().getName());
                            barrier.await();
                        }
                        count++;
                        sum += elem;
                        System.out.printf("%s: sum = %s%n", Thread.currentThread().getName(), Integer.toString(sum, 2));
                    }
                    return sum;
                } finally {
                    // must ensure every expected thread eventually calls await()
                    // so that other threads do not hang indefinitely
                    if (!!!awaitInvoked)
                        barrier.await();
                }
            }
        };
    }

    @Test
    public void testPutWhileIteratingThroughKeys() {
        BlockingList<Integer, String> bList = BlockingListMaker.defineList().useKeys(1, 2, 3, 4).make();
        for (Integer i : bList.getUnmatchedKeys()) {
            bList.put(i, i.toString());
        }
        assertEquals(list("1", "2", "3", "4"), bList);
    }

    @Test
    public void testPutThenPutIfAbsent() {
        BlockingList<Integer, String> bList = BlockingListMaker.defineList().useKeys(0).make();
        assertEquals(EMPTY_LIST, bList.getCondensedList());
        bList.put(0, "put");
        assertEquals(list("put"), bList.getCondensedList());
        assertFalse(bList.putIfAbsent(0, "tryToPut"));
        assertEquals(list("put"), bList.getCondensedList());
    }

    @Test
    public void testPutIfAbsentThenPut() {
        BlockingList<Integer, String> bList = BlockingListMaker.defineList().useKeys(0).make();
        // 2) tryToPut should return true, then put should fail
        assertEquals(EMPTY_LIST, bList.getCondensedList());
        assertTrue(bList.putIfAbsent(0, "tryToPut"));
        assertEquals(list("tryToPut"), bList.getCondensedList());
        try {
            bList.put(0, "put");
            fail("put() should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        assertEquals(list("tryToPut"), bList.getCondensedList());
    }

    @Test
    public void testSingleElementListWithTimeout() {
        BlockingList<Integer, String> bList = BlockingListMaker.defineList().waitFor(2, MINUTES).useKeys(0).make();
        assertFalse(bList.isTimedOut());
        assertEquals(EMPTY_LIST, bList.getCondensedList());
        assertFalse(bList.isTimedOut());
        bList.put(0, "foo");
        assertFalse(bList.isTimedOut());
        assertEquals(list("foo"), bList.getCondensedList());
        assertFalse(bList.isTimedOut());
        assertEquals(list("foo"), bList);
        assertFalse(bList.isTimedOut());
    }

    @Test
    public void testSingleElementListIsEmptyListAfterTimeout() {
        BlockingList<Integer, String> bList = BlockingListMaker.defineList().waitFor(2, MILLISECONDS).useKeys(0).make();
        assertFalse(bList.isTimedOut());
        assertEquals(EMPTY_LIST, bList.getCondensedList());
        assertFalse(bList.isTimedOut());
        try {
            bList.get(0);
            fail("get() should throw IndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException expected) {
        }
        assertTrue(bList.isTimedOut());
        // after the timeout, bList should no longer block
        // and should look like an empty list
        assertEquals(EMPTY_LIST, bList);
        // it should still be possible to provide the expected elements
        bList.put(0, "foo");
        assertFalse(bList.isTimedOut());
        // bList should now look like a list with the single element "foo"
        assertEquals(list("foo"), bList.getCondensedList());
        assertEquals(list("foo"), bList);
        assertFalse(bList.isTimedOut());
    }

    @Test
    public void testMultiElementListWithTimeout() throws Exception {
        partFillListAndTimeoutRetrievingNthElement(4, 0);
        partFillListAndTimeoutRetrievingNthElement(4, 0);
        partFillListAndTimeoutRetrievingNthElement(4, 1);
        partFillListAndTimeoutRetrievingNthElement(4, 2);
        partFillListAndTimeoutRetrievingNthElement(4, 3);
    }

    private void partFillListAndTimeoutRetrievingNthElement(final int size, final int n) throws InterruptedException, BrokenBarrierException, ExecutionException {
        final int elementsToFillPostTimeout = size - n;
        final int threadsPerElementPostTimeout = 10;
        // map of futures to expected values
        Map<Future<Integer>, Integer> futuresMap = new HashMap<Future<Integer>, Integer>(threadsPerElementPostTimeout * elementsToFillPostTimeout);

        // create the list
        BlockingList<Integer, Integer> bList = BlockingListMaker
                        .defineList()
                        .waitFor(0, SECONDS)
                        .useKeys(keysUpTo(size)).make();

        int expectedSum = 0;
        List<Integer> listSoFar = new ArrayList<Integer>();

        CyclicBarrier barrier = new CyclicBarrier(threadsPerElementPostTimeout + 1); // + 1 for this thread!
        // add each element in turn
        for (int elementIndex = 0; elementIndex < size; elementIndex++) {
            assertEquals(listSoFar, bList.getCondensedList());

            // skip to the next iteration if we haven't reached the timeout element yet
            if (elementIndex >= n) {
                // create the sync point for these futures
                // (first time through this code will force the timeout)
                int waitBeforeIndex = elementIndex + 1;
                for (int j = 0; j < threadsPerElementPostTimeout; j++) {
                    FutureTask<Integer> future = new FutureTask<Integer>(createSynchronisedAdder(barrier, bList, waitBeforeIndex));
                    futuresMap.put(future, expectedSum);
                    new Thread(future, "T" + elementIndex + "." + j).start();
                }

                // wait for the threads to reach the barrier
                barrier.await();
                barrier.reset();
                assertEquals("The timed out list should now show the actual number of elements added so far", elementIndex, bList.size());
                assertEquals("The timed out list should now appear to contain only those elements already added", listSoFar, bList);
            }

            if (elementIndex == n)
                // we should just have timed out, check blist.collapse again
                assertEquals(listSoFar, bList.getCondensedList());

            // each element is a unique power of two 
            // so the sum identifies any missing elems
            int elem = 1 << elementIndex;
            expectedSum += elem;
            // set the new element in the list
            bList.put(elementIndex, elem);
            listSoFar.add(elem);
        }

        // now check all the results are what they should be
        for (Entry<Future<Integer>, Integer> entry : futuresMap.entrySet())
            assertEquals(entry.getValue(), entry.getKey().get());
    }

    /**
     * @param size
     * @return
     */
    private Integer[] keysUpTo(final int size) {
        Integer[] keys = new Integer[size];
        for (int i = 0; i < keys.length; i++)
            keys[i] = i;
        return keys;
    }

    /** Test that iteration works ok on a sparse list */
    @Test
    public void testSparseListIsNavigatedCorrectlyWhenCondensed() {
        testSparseListTimingOutAtEachElement(10, 9, 7, 5, 3, 1, 0, 2, 4, 6, 8);
    }

    private void testSparseList(int size, Integer timeoutElem, Integer... elems) {
        BlockingList<Integer, Integer> bList = BlockingListMaker.defineList().waitFor(0, SECONDS).useKeys(keysUpTo(size)).make();
        boolean expectedTimedOut = false;
        for (Integer i : elems) {
            if (i.equals(timeoutElem)) {
                new ArrayList<Integer>(bList);
                expectedTimedOut = true;
            }
            assertEquals(expectedTimedOut, bList.isTimedOut());
            bList.put(i, i);
        }
        // if we filled the whole list, it should no longer be timed out
        if (expectedTimedOut) {
            int uniqueElemCount = new HashSet<Integer>(list(elems)).size();
            if (size == uniqueElemCount)
                expectedTimedOut = false;
        }
        assertEquals(expectedTimedOut, bList.isTimedOut());

        // now create a sorted list of the unique elements and compare 
        final List<Integer> expected = new ArrayList<Integer>(new TreeSet<Integer>(list(elems)));
        // check the condensed list
        assertEquals(expected, bList.getCondensedList());
        // check the blocking list (forcing timeout if not already occurred)
        assertEquals(expected, bList);
        // check the condensed list again
        assertEquals(expected, bList.getCondensedList());
    }

    private void testSparseListTimingOutAtEachElement(int size, Integer... elems) {
        // iterate through elements, eliminating dupes, 
        // timing out before each element in turn
        for (Integer timeoutElem : new LinkedHashSet<Integer>(list(elems)))
            testSparseList(size, timeoutElem, elems);
        // once more without an explicit timeout
        testSparseList(size, -1, elems);
    }

    private <E> List<E> list(E... elements) {
        return Arrays.asList(elements);
    }

    private <E> Set<E> set(E... elements) {
        return new HashSet<E>(list(elements));
    }
}
