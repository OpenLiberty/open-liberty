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

import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.ASYNC;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.FAILASYNC;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.FAILFETCH;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.FAILSYNC;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.FETCH;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.JOINSYNC;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.MISS;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.PUT;
import static com.ibm.ws.classloading.internal.util.BlockingListListenerTest.ElementDelivery.SYNC;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import test.common.SharedOutputManager;

import com.ibm.ws.classloading.internal.util.BlockingList.Listener;
import com.ibm.ws.classloading.internal.util.BlockingList.Retriever;
import com.ibm.ws.classloading.internal.util.BlockingList.Slot;

/**
 * Test the blocking list blocks until a requested element is available.
 * Test the list times out if a timeout has been specified.
 * Test that the list degrades to a condensed list after a timeout.
 * Test that the iterators work consistently in the face of timeouts.
 */
public class BlockingListListenerTest {

    @Rule
    public SharedOutputManager outputMgr = SharedOutputManager.getInstance().trace("*=all");

    private final Mockery context = new JUnit4Mockery();
    private final List<Thread> threads = new ArrayList<Thread>();

    @Before
    public void clearThreads() {
        threads.clear();
    }

    private void waitForThreads() {
        for (Thread t : threads)
            try {
                t.join();
            } catch (InterruptedException stopsTheJoin) {
            }
        clearThreads();
    }

    private <V> void notifyAsync(final Slot<? super V> slot, final V value) {
        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println("Filling slot");
                slot.fill(value);
                System.out.println("Slot filled");
            }
        };
        t.setDaemon(true);
        t.start();
        threads.add(t);
    }

    private void failAsync(final Slot<?> slot) {
        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println("Deleting slot");
                slot.delete();
                System.out.println("Slot deleted");
            }
        };
        t.setDaemon(true);
        t.start();
        threads.add(t);
    }

    /** Test that a listener is created. */
    @Test
    @SuppressWarnings("unchecked")
    public void testListenerIsCreated() {
        final Listener<String, String> listener = context.mock(Listener.class);
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(listener)
                        .waitFor(2, MILLISECONDS)
                        .useKeys("message")
                        .make();
        context.checking(new Expectations() {
            {
                oneOf(listener).listenFor(with(equal("message")), with(any(Slot.class)));
            }
        });
        for (String unexpected : bList)
            fail("Found unexpected string in blocking list: " + unexpected);
        context.assertIsSatisfied();
    }

    /** Test that a listener can call back to fail to provide an element on the same thread. */
    @Test
    public void testListenerFailsSynchronously() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, Slot<? super String> slot) {
                                slot.delete();
                            }
                        })
                        .useKeys("message")
                        .make();
        assertEquals("List should have size 0", 0, bList.size());
        assertTrue("List should evaluate to empty", bList.isEmpty());
        assertEquals("List should compare equal to an empty list", list(), bList);
    }

    /** Test that a listener can call back to fail to provide an element on a different thread. */
    @Test
    public void testListenerFailsAsynchronously() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, Slot<? super String> slot) {
                                failAsync(slot);
                            }
                        })
                        .useKeys("message")
                        .make();
        bList.size();
        assertEquals("List should have size 0", 0, bList.size());
        assertTrue("List should evaluate to empty", bList.isEmpty());
        assertEquals("List should compare equal to an empty list", list(), bList);
    }

    /** Test that a failed key can not be used. */
    @Test(expected = IllegalArgumentException.class)
    public void testSynchronouslyFailedKeyRemainsFailed() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, Slot<? super String> slot) {
                                slot.delete();
                            }
                        })
                        .useKeys("message")
                        .make();
        bList.size();
        System.out.println(bList);
        bList.put("message", "This should throw an exception");
        System.out.println(bList); // should not reach here
    }

    /** Test that a listener can call back to provide an element on the same thread. */
    @Test
    public void testListenerWorksSynchronously() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, Slot<? super String> slot) {
                                slot.fill("Hello");
                            }
                        })
                        .waitFor(2, MILLISECONDS)
                        .useKeys("message")
                        .make();
        assertEquals(list("Hello"), bList);
    }

    /** Test that a listener can call back on another thread while the reading thread is waiting. */
    @Test
    public void testListenerWorksAsynchronously() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, final Slot<? super String> slot) {
                                notifyAsync(slot, "Hello");
                            }

                        })
                        .waitIndefinitely()
                        .useKeys("message")
                        .make();
        assertEquals(list("Hello"), bList); // iteration should block until element is available
    }

    /** Test that a listener can call back on another thread while the reading thread is waiting. */
    @Test
    public void testListenerWorksAsynchronouslyWithTimeout() {
        BlockingList<String, String> bList = BlockingListMaker
                        .defineList()
                        .listenForElements(new Listener<String, String>() {
                            @Override
                            public void listenFor(String key, final Slot<? super String> slot) {
                                notifyAsync(slot, "Hello");
                            }

                        })
                        .waitFor(10, SECONDS)
                        .useKeys("message")
                        .make();
        assertEquals(list("Hello"), bList); // iteration should block until element is available
    }

    enum ElementDelivery {
        PUT, FETCH, SYNC, ASYNC, JOINSYNC, MISS, FAILFETCH, FAILSYNC, FAILASYNC;
        public boolean shouldSucceed() {
            return this.ordinal() < MISS.ordinal();
        }
    };

    /** a retriever that retrieves only the elements marked with FETCH */
    private static class MockRetriever implements Retriever<Integer, String> {
        final ElementDelivery[] deliveries;

        MockRetriever(ElementDelivery... deliveries) {
            this.deliveries = deliveries;
        }

        @Override
        public String fetch(Integer key) throws ElementNotReadyException, ElementNotValidException {
            switch (deliveries[key]) {
                case FETCH:
                    return deliveries[key] + "#" + key;
                case FAILFETCH:
                    throw new ElementNotValidException();
                default:
                    throw new ElementNotReadyException();
            }
        }

    }

    /**
     * define a listener for these types of element delivery
     * <ul> <li>SYNC synchronously supplies a value
     * </li><li>ASYNC asynchronously supplies a value
     * </li><li>JOINSYNC as for SYNC, but also waits for any ASYNC threads to complete
     * </li><li>FAILSYNC delete the slot synchronously
     * </li><li>FAILASYNC delete the slot asynchronously</li></ul>
     */
    private class MockListener implements Listener<Integer, String> {
        final ElementDelivery[] deliveries;

        MockListener(ElementDelivery... deliveries) {
            this.deliveries = deliveries;
        }

        @Override
        public void listenFor(Integer key, Slot<? super String> slot) {
            switch (deliveries[key]) {
                case SYNC:
                    slot.fill(deliveries[key] + "#" + key);
                    break;
                case ASYNC:
                    notifyAsync(slot, deliveries[key] + "#" + key);
                    break;
                case JOINSYNC:
                    waitForThreads();
                    slot.fill(deliveries[key] + "#" + key);
                    break;
                case FAILSYNC:
                    slot.delete();
                    break;
                case FAILASYNC:
                    failAsync(slot);
                    break;
                default: // do nothing
            }
        }
    }

    /** Test that interleaved retriever and listener success and failure work as expected */
    @Test
    public void testMixedElementDeliveryWithTimeout() {
        // define how each element should arrive
        // note the special case JOINSYNC which additionally waits for async threads to complete
        testMixedMode(MISS, PUT, FETCH, SYNC, ASYNC, MISS, PUT, FAILSYNC, FAILASYNC, MISS, PUT, FAILFETCH, MISS, PUT, FETCH, SYNC, ASYNC, JOINSYNC, MISS, PUT, FETCH);
        // get a list of all the possible delivery types
        List<ElementDelivery> enums = Arrays.asList(ElementDelivery.values());
        // make a queue with the delivery types in order, twice
        Queue<ElementDelivery> queue = new LinkedList<ElementDelivery>(enums);
        queue.addAll(enums);
        // loop round invoking the test with the queue of delivery types, 
        // taking one off the head and putting it on the tail
        // until we have started and ended with each delivery type
        for (int i = 0; i < enums.size(); i++, queue.offer(queue.poll()))
            testMixedMode(queue);
    }

    private void testMixedMode(Collection<ElementDelivery> deliveries) {
        testMixedMode(deliveries.toArray(new ElementDelivery[deliveries.size()]));
    }

    private void testMixedMode(ElementDelivery... deliveries) {
        System.out.println("Element delivery mode order: " + Arrays.toString(deliveries));
        // create the blocking list
        BlockingList<Integer, String> actual = BlockingListMaker
                        .defineList()
                        .waitFor(0, SECONDS)
                        .fetchElements(new MockRetriever(deliveries))
                        .listenForElements(new MockListener(deliveries))
                        .useKeys(getIntegerSequence(deliveries.length))
                        .make();
        // directly insert elements marked as PUT
        for (Integer i = 0; i < deliveries.length; i++)
            if (deliveries[i] == PUT)
                actual.put(i, deliveries[i] + "#" + i);
        // create and populate the expected list
        ArrayList<String> expected = new ArrayList<String>();
        for (Integer i = 0; i < deliveries.length; i++)
            if (deliveries[i].shouldSucceed())
                expected.add(deliveries[i] + "#" + i);
        // force a traversal of the list before comparison
        // this ensures any threads we need to wait for are created
        System.out.println("Actual list size at traversal: " + actual.size());

        waitForThreads();
        assertEquals(expected, actual);
    }

    /** @return an array of Integer objects 0,1...(length - 1) */
    private Integer[] getIntegerSequence(int length) {
        final Integer[] keys = new Integer[length];
        for (int i = 0; i < keys.length; i++)
            keys[i] = i;
        return keys;
    }

    private <E> List<E> list(E... elements) {
        return Arrays.asList(elements);
    }
}
