/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests that can be used to help catch errors in experimental implementations of a thread-safe queue
 * with two insertion points, one at tail for normal offers, and another at or near head for expedited offers.
 */
public class DoubleInsertionPointQueueTest {
    // TODO toggle between a valid implementation that is not optimized to our needs (we don't need
    //      poll/remove from tail or reverse iteration) and various experimental implementations
    //class DoubleInsertionPointQueue<T> extends BoundedBuffer<T> {
    //class DoubleInsertionPointQueue<T> extends ORDeque<T> {
    //class DoubleInsertionPointQueue<T> extends LinkedBlockingDeque<T> {
    //boolean expeditedOfferIsPush() {
    //    return true;
    //}
    class DoubleInsertionPointQueue<T> extends DoubleQueue<T> {
        boolean expeditedOfferIsPush() {
            return false;
        }
    }

    private static class IntQueueItem extends Number implements QueueItem {
        private static final long serialVersionUID = 1L;
        private final boolean expedite;
        private final int value;

        public IntQueueItem(int value, boolean expedite) {
            this.value = value;
            this.expedite = expedite;
        }

        @Override
        public boolean isExpedited() {
            return expedite;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    private static ExecutorService testThreads;

    private static final long TIMEOUT_NS = TimeUnit.SECONDS.toNanos(10);

    @AfterClass
    public static void afterClass() {
        if (testThreads != null)
            testThreads.shutdownNow();
    }

    @BeforeClass
    public static void beforeClass() {
        testThreads = Executors.newFixedThreadPool(20);
    }

    private static class OfferTask<T> implements Callable<Boolean> {
        private final DoubleInsertionPointQueue<T> q;
        private final T value;

        private OfferTask(DoubleInsertionPointQueue<T> q, T value) {
            this.q = q;
            this.value = value;
        }

        @Override
        public Boolean call() throws Exception {
            return q.offer(value);
        }
    }

    private static class PollTask<T> implements Callable<T> {
        private final DoubleInsertionPointQueue<T> q;
        private final long timeoutMS;

        private PollTask(DoubleInsertionPointQueue<T> q, long timeoutMS) {
            this.q = q;
            this.timeoutMS = timeoutMS;
        }

        @Override
        public T call() throws Exception {
            if (timeoutMS == 0)
                return q.poll();
            else if (timeoutMS == Long.MAX_VALUE)
                return q.take();
            else
                return q.poll(timeoutMS, TimeUnit.MICROSECONDS);
        }
    }

    private static class RemoveTask<T> implements Callable<Boolean> {
        private final DoubleInsertionPointQueue<T> q;
        private final T value;

        private RemoveTask(DoubleInsertionPointQueue<T> q, T value) {
            this.q = q;
            this.value = value;
        }

        @Override
        public Boolean call() throws Exception {
            return q.remove(value);
        }
    }

    private static class RepeatingOfferTask implements Callable<Void> {
        private final AtomicBoolean done;
        private final DoubleInsertionPointQueue<Number> q;
        private final AtomicInteger size;
        private final boolean expedite;

        private RepeatingOfferTask(DoubleInsertionPointQueue<Number> q, AtomicBoolean done, AtomicInteger size, boolean expedite) {
            this.done = done;
            this.q = q;
            this.size = size;
            this.expedite = expedite;
        }

        @Override
        public Void call() throws Exception {
            while (!done.get()) {
                int r = (int) (Math.random() * 5.0);
                Number item = expedite ? new IntQueueItem(r, true) : r;
                if (q.offer(item))
                    size.incrementAndGet();
            }
            return null;
        }
    }

    private static class RepeatingPollTask implements Callable<Void> {
        private final AtomicBoolean done;
        private final DoubleInsertionPointQueue<Number> q;
        private final AtomicInteger size;
        private final long timeoutMS;

        private RepeatingPollTask(DoubleInsertionPointQueue<Number> q, AtomicBoolean done, AtomicInteger size, long timeoutMS) {
            this.done = done;
            this.q = q;
            this.size = size;
            this.timeoutMS = timeoutMS;
        }

        @Override
        public Void call() throws Exception {
            while (!done.get()) {
                if (timeoutMS == 0) {
                    if (q.poll() != null)
                        size.decrementAndGet();
                } else if (timeoutMS == Integer.MAX_VALUE) {
                    q.take();
                    size.decrementAndGet();
                } else {
                    if (q.poll(timeoutMS, TimeUnit.MILLISECONDS) != null)
                        size.decrementAndGet();
                }
            }
            return null;
        }
    }

    private static class RepeatingRemoveTask implements Callable<Void> {
        private final AtomicBoolean done;
        private final DoubleInsertionPointQueue<Number> q;
        private final AtomicInteger size;

        private RepeatingRemoveTask(DoubleInsertionPointQueue<Number> q, AtomicBoolean done, AtomicInteger size) {
            this.done = done;
            this.q = q;
            this.size = size;
        }

        @Override
        public Void call() throws Exception {
            while (!done.get()) {
                if (q.remove((int) (Math.random() * 5.0)))
                    size.decrementAndGet();
            }
            return null;
        }
    }

    // With 1 item in the queue, concurrently offer another, and then poll 2 items, where the second poll waits if necessary.
    // At the end of the test, both items should be polled and there should be nothing left in the queue.
    @Test
    public void testConcurrentOfferPoll() throws Exception {
        final long durationOfTestNS = TimeUnit.SECONDS.toNanos(1);

        DoubleInsertionPointQueue<String> q = new DoubleInsertionPointQueue<String>();
        Callable<Boolean> offerBTask = new OfferTask<String>(q, "B");
        for (long start = System.nanoTime(); System.nanoTime() - start < durationOfTestNS;) {
            assertTrue(q.offer("A"));
            Future<?> future = testThreads.submit(offerBTask);
            assertEquals("A", q.poll());
            assertEquals("B", q.poll(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            future.get(TIMEOUT_NS, TimeUnit.NANOSECONDS);
            assertEquals(0, q.size());
        }
    }

    // Concurrently poll the queue while on another thread offering 2 items and polling once.
    // The second poll which follows the 2 offers must return an item, whereas the first poll might or might not.
    @Test
    public void testConcurrentPollOffer() throws Exception {
        final long durationOfTestNS = TimeUnit.SECONDS.toNanos(1);

        DoubleInsertionPointQueue<String> q = new DoubleInsertionPointQueue<String>();
        Callable<String> pollTask = new PollTask<String>(q, 0);
        for (long start = System.nanoTime(); System.nanoTime() - start < durationOfTestNS;) {
            Future<String> future = testThreads.submit(pollTask);
            assertTrue(q.offer("C"));
            assertTrue(q.offer("D"));

            String item2 = q.poll();
            String item1 = future.get(10, TimeUnit.SECONDS);

            // Poll on same thread as offers must return something because there has been at most 1 other poll
            assertNotNull(item2);
            assertNotSame(item1, item2);
            assertTrue("C".equals(item2) || "D".equals(item2));

            if (item1 == null)
                assertEquals(1, q.size());
            else {
                assertEquals(0, q.size());
                assertTrue("C".equals(item1) || "D".equals(item1));
            }

            q.clear();
            assertTrue(q.isEmpty());
        }
    }

    // Concurrently remove 2 consecutive items and confirm that the list contains the correct items afterwards.
    @Test
    public void testConcurrentRemovals() throws Exception {
        final long durationOfTestNS = TimeUnit.SECONDS.toNanos(1);

        DoubleInsertionPointQueue<String> q = new DoubleInsertionPointQueue<String>();
        Callable<Boolean> removeMTask = new RemoveTask<String>(q, "M");
        Callable<Boolean> removeNTask = new RemoveTask<String>(q, "N");
        for (long start = System.nanoTime(); System.nanoTime() - start < durationOfTestNS;) {
            assertTrue(q.offer("M"));
            assertTrue(q.offer("N"));

            Future<Boolean> futureRemoveM = testThreads.submit(removeMTask);
            Future<Boolean> futureRemoveN = testThreads.submit(removeNTask);

            assertTrue(futureRemoveM.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));
            assertTrue(futureRemoveN.get(TIMEOUT_NS, TimeUnit.NANOSECONDS));

            assertNull(q.peek());
            assertEquals(0, q.size());
            assertTrue(q.isEmpty());

            q.clear();
            assertTrue(q.isEmpty());
        }
    }

    // General test of iterator, including concurrent removal
    @Test
    public void testIterator() throws Exception {
        assertFalse(new DoubleInsertionPointQueue<String>().iterator().hasNext());

        DoubleInsertionPointQueue<String> q = new DoubleInsertionPointQueue<String>();
        assertTrue(q.add("A"));
        assertTrue(q.add("B"));
        assertTrue(q.add("C"));
        assertTrue(q.add("C"));
        assertTrue(q.add("D"));
        assertTrue(q.add("C"));

        assertEquals(6, q.size());

        boolean supportsRemove;
        try {
            for (Iterator<String> it = q.iterator(); it.hasNext();)
                if (it.next().equals("C"))
                    it.remove();
            supportsRemove = true;
        } catch (UnsupportedOperationException x) {
            // use removeAll instead
            q.removeAll(Collections.singleton("C"));
            supportsRemove = false;
        }

        assertEquals(3, q.size());

        Iterator<String> it = q.iterator();
        assertEquals("A", it.next());
        assertTrue(it.hasNext());
        if (supportsRemove)
            it.remove(); // A
        else
            q.remove("A");

        assertEquals(2, q.size());
        assertFalse(q.contains("A"));
        assertTrue(q.contains("B"));

        assertTrue(q.add("E"));

        assertTrue(it.hasNext());
        assertEquals("B", it.next());
        assertEquals("D", it.next());
        assertTrue(q.remove("D"));
        if (supportsRemove)
            try {
                it.remove();
                //fail("should not be able to remove what was already removed"); // TODO expectation for LinkedBlockingQueue differs here
            } catch (NoSuchElementException x) {
            }
        assertTrue(it.hasNext());
        try {
            it.remove();
            fail("remove should be disallowed before next invoked");
        } catch (IllegalStateException x) {
        } catch (UnsupportedOperationException x) {
        }
        assertEquals("E", it.next());
        assertFalse(it.hasNext());
        assertEquals(2, q.size());
    }

    // This test focuses on smaller-sized queues where items that are offered are rapidly polled/removed from the queue.
    @Test
    public void testManyPollsFewOffers() throws Exception {
        final long durationOfTestNS = TimeUnit.SECONDS.toNanos(2);

        final AtomicBoolean done = new AtomicBoolean();
        final DoubleInsertionPointQueue<Number> q = new DoubleInsertionPointQueue<Number>();
        final AtomicInteger size = new AtomicInteger();

        Future<?>[] f = new Future<?>[10];
        f[0] = testThreads.submit(new RepeatingOfferTask(q, done, size, false));
        f[1] = testThreads.submit(new RepeatingOfferTask(q, done, size, true));
        f[2] = testThreads.submit(new RepeatingPollTask(q, done, size, 0)); // poll
        f[3] = testThreads.submit(new RepeatingPollTask(q, done, size, 500)); // poll(timeout)
        f[4] = testThreads.submit(new RepeatingPollTask(q, done, size, Integer.MAX_VALUE)); // take
        f[5] = testThreads.submit(new RepeatingRemoveTask(q, done, size));
        f[6] = testThreads.submit(new RepeatingPollTask(q, done, size, 0)); // poll
        f[7] = testThreads.submit(new RepeatingPollTask(q, done, size, 500)); // poll(timeout)
        f[8] = testThreads.submit(new RepeatingPollTask(q, done, size, Integer.MAX_VALUE)); // take
        f[9] = testThreads.submit(new RepeatingRemoveTask(q, done, size));

        TimeUnit.NANOSECONDS.sleep(durationOfTestNS);

        done.set(true);
        f[4].cancel(true); // interrupt take
        f[8].cancel(true); // interrupt take
        for (Future<?> future : f)
            try {
                future.get();
            } catch (CancellationException x) {
            }

        assertEquals(q.toString(), size.get(), q.size());

        int count = 0;
        for (Iterator<Number> it = q.iterator(); it.hasNext();) {
            assertTrue(it.hasNext());
            assertNotNull(it.next());
            count++;
        }

        assertEquals(size.get(), count);
    }

    // Repeatedly have 2 threads performing offer, 2 performing expedited offer, 2 performing poll, 1 performing timed poll,
    // and one performing removal of specific items. Let the test run for a fixed duration and then compare the
    // reported size against what we think the size should be based on whether or not offers/polls/removes
    // were successful.
    @Test
    public void testMultipleOfferPollConcurrently() throws Exception {
        final long durationOfTestNS = TimeUnit.SECONDS.toNanos(2);

        final AtomicBoolean done = new AtomicBoolean();
        final DoubleInsertionPointQueue<Number> q = new DoubleInsertionPointQueue<Number>();
        final AtomicInteger size = new AtomicInteger();

        Future<?>[] f = new Future<?>[10];
        f[0] = testThreads.submit(new RepeatingOfferTask(q, done, size, false));
        f[1] = testThreads.submit(new RepeatingOfferTask(q, done, size, true));
        f[2] = testThreads.submit(new RepeatingPollTask(q, done, size, 0));
        f[3] = testThreads.submit(new RepeatingPollTask(q, done, size, 500));
        f[4] = testThreads.submit(new RepeatingRemoveTask(q, done, size));
        f[5] = testThreads.submit(new RepeatingOfferTask(q, done, size, false));
        f[6] = testThreads.submit(new RepeatingOfferTask(q, done, size, true));
        f[7] = testThreads.submit(new RepeatingPollTask(q, done, size, 0));
        f[8] = testThreads.submit(new RepeatingPollTask(q, done, size, 500));
        f[9] = testThreads.submit(new RepeatingRemoveTask(q, done, size));
        TimeUnit.NANOSECONDS.sleep(durationOfTestNS);
        done.set(true);
        for (Future<?> future : f)
            future.get();

        assertEquals(size.get(), q.size());

        int count = 0;
        for (Iterator<Number> it = q.iterator(); it.hasNext();) {
            assertTrue(it.hasNext());
            assertNotNull(it.next());
            count++;
        }

        assertEquals(size.get(), count);
    }

    // Sequential test of: offer, remove specific items, poll, offer, and poll.
    @Test
    public void testOfferRemovePollOfferPoll() {
        DoubleInsertionPointQueue<String> q = new DoubleInsertionPointQueue<String>();
        assertTrue(q.offer("A"));
        assertTrue(q.offer("B"));
        assertTrue(q.offer("C"));
        assertTrue(q.offer("D"));
        assertTrue(q.offer("E"));
        assertTrue(q.offer("F"));
        assertEquals(6, q.size());
        assertTrue(q.remove("F"));
        assertTrue(q.remove("E"));
        assertTrue(q.remove("D"));
        assertEquals(3, q.size());
        assertTrue(q.remove("C"));
        assertTrue(q.remove("B"));
        assertTrue(q.remove("A"));
        assertEquals(0, q.size());
        assertNull(q.poll());
        assertEquals(0, q.size());
        assertTrue(q.offer("G"));
        assertEquals(1, q.size());
        assertEquals("G", q.poll());
        assertEquals(0, q.size());
    }

    // Sequential test of: expedited offer, poll, offer, poll.
    @Test
    public void testExpeditedOfferPollOfferPoll() {
        DoubleInsertionPointQueue<Number> q = new DoubleInsertionPointQueue<Number>();
        if (q.expeditedOfferIsPush()) {
            q.offer(new IntQueueItem(20, true));
            q.offer(new IntQueueItem(15, true));
            q.offer(new IntQueueItem(10, true));
            q.offer(new IntQueueItem(5, true));
        } else {
            q.offer(new IntQueueItem(5, true));
            q.offer(new IntQueueItem(10, true));
            q.offer(new IntQueueItem(15, true));
            q.offer(new IntQueueItem(20, true));
        }
        assertEquals(4, q.size());
        assertEquals(5, q.peek().intValue());
        assertEquals(5, q.poll().intValue());
        assertEquals(10, q.peek().intValue());
        assertEquals(10, q.poll().intValue());
        assertEquals(2, q.size());
        assertTrue(q.offer(25));
        assertEquals(15, q.peek().intValue());
        assertEquals(15, q.poll().intValue());
        assertEquals(20, q.peek().intValue());
        assertEquals(20, q.poll().intValue());
        assertEquals(1, q.size());
        assertEquals(25, q.peek().intValue());
        assertEquals(25, q.poll().intValue());
        assertEquals(0, q.size());
        assertNull(q.poll());
        assertEquals(0, q.size());
    }
}
