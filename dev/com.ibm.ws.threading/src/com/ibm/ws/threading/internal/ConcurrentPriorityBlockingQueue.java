/*******************************************************************************
 * Copyright (c) 2020, 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.threading.internal;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This BlockingQueue implementation takes the concepts from the DoubleQueue and ConcurrentLinkedQueue
 * and combines them together. The logic in this class is largely from both of those classes
 * with the appropriate updates needed to support having both a queue of priority elements
 * and non priority elements.
 *
 * If no expedited items were ever added to the queue, it is optimized to skip directly to the non
 * expedited portion of the queue.
 *
 * @param <T>
 */
public class ConcurrentPriorityBlockingQueue<T> extends AbstractQueue<T> implements BlockingQueue<T> {

    private static class Node<T> extends AtomicReference<T> {
        private static final long serialVersionUID = 1L;

        final AtomicReference<Node<T>> next = new AtomicReference<>();

        Node(T element) {
            super.set(element);
        }
    }

    private interface Head<T> {
        Node<T> getFirst();

        boolean compareAndSetFirst(Node<T> oldFirst, Node<T> newFirst);
    }

    private static class ExpeditedHead<T> extends AtomicReference<Node<T>> implements Head<T> {
        private static final long serialVersionUID = 1L;

        ExpeditedHead(Node<T> first) {
            set(first);
        }

        @Override
        public Node<T> getFirst() {
            return get();
        }

        @Override
        public boolean compareAndSetFirst(Node<T> oldFirst, Node<T> newFirst) {
            return compareAndSet(oldFirst, newFirst);
        }
    }

    private static class NonExpeditedHead<T> extends Node<T> implements Head<T> {
        private static final long serialVersionUID = 1L;

        NonExpeditedHead(Node<T> first) {
            super(null);
            next.set(first);
        }

        @Override
        public Node<T> getFirst() {
            return next.get();
        }

        @Override
        public boolean compareAndSetFirst(Node<T> oldFirst, Node<T> newFirst) {
            return next.compareAndSet(oldFirst, newFirst);
        }
    }

    final Head<T> expeditedHead;

    final AtomicReference<Node<T>> expeditedTail = new AtomicReference<>();

    final NonExpeditedHead<T> nonExpeditedHead;

    final AtomicReference<Node<T>> nonExpeditedTail = new AtomicReference<>();

    /**
     * Count of items available for poll/removal.
     */
    final ReduceableSemaphore size = new ReduceableSemaphore(0, false);

    /**
     * The currentHead field is set to nonExpeditedHead initially until an expedited item is added to the queue
     */
    final AtomicReference<Head<T>> currentHead = new AtomicReference<>();

    @SuppressWarnings("unchecked")
    final FirstAction<T, Node<T>> GET_FIRST_NODE = GetFirstNode.INSTANCE;

    @SuppressWarnings("unchecked")
    private final FirstAction<T, T> GET_FIRST_ITEM = GetFirstItem.INSTANCE;

    @SuppressWarnings("unchecked")
    private final FirstAction<T, T> REMOVE_FIRST_ITEM = RemoveFirstItem.INSTANCE;

    ConcurrentPriorityBlockingQueue() {
        Node<T> nonExNode = new Node<>(null);
        nonExpeditedHead = new NonExpeditedHead<>(nonExNode);
        nonExpeditedTail.set(nonExNode);
        currentHead.set(nonExpeditedHead);

        // There must be a node for the head and tail to point to
        Node<T> exNode = new Node<>(null);
        exNode.next.set(nonExpeditedHead);
        expeditedHead = new ExpeditedHead<>(exNode);
        expeditedTail.set(exNode);
    }

    @Override
    public boolean add(T e) {
        return offer(e);
    }

    /**
     * Determine if the provided Object is in the queue.
     *
     * If the Object is a QueueItem and it is marked expedited, look at the expedited part of the queue,
     * otherwise look at the non-expedited part of the queue.
     *
     * @param item the item to determine if it is in the queue
     */
    @Override
    public boolean contains(Object item) {
        if (item == null) {
            return false;
        }

        Head<T> head;
        Node<T> end;

        if (item instanceof QueueItem && ((QueueItem) item).isExpedited()) {
            // If it is an expedited item, but no expedited items have been added, return false
            if (currentHead.get() == nonExpeditedHead) {
                return false;
            }
            head = expeditedHead;
            end = nonExpeditedHead;
        } else {
            head = nonExpeditedHead;
            end = null;
        }

        Node<T> current = getFirstWithAction(head, end, GET_FIRST_NODE);
        for (; current != null; current = getNext(current, head, end)) {
            T element = current.get();
            if (element != null && item.equals(element)) {
                return true;
            }
        }
        return false;
    }

    private Node<T> getNext(Node<T> current, Head<T> head, Node<T> end) {
        Node<T> next = current.next.get();
        // If next points back to the current node, it means it was removed
        // In that case start over again at the beginning
        return (current == next) ? head.getFirst() : (next != end ? next : null);
    }

    @Override
    public int drainTo(Collection<? super T> col) {
        if (col == null) {
            throw new NullPointerException();
        }
        if (col == this) {
            throw new IllegalArgumentException();
        }
        int count = 0;
        for (T item; (item = poll()) != null; count++)
            col.add(item);
        return count;
    }

    @Override
    public int drainTo(Collection<? super T> col, int maxElements) {
        if (col == null) {
            throw new NullPointerException();
        }
        if (col == this) {
            throw new IllegalArgumentException();
        }
        int count = 0;
        for (T item; count < maxElements && (item = poll()) != null; count++)
            col.add(item);
        return count;
    }

    @Override
    public boolean isEmpty() {
        return size.availablePermits() <= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new QueueIterator();
    }

    private class QueueIterator implements Iterator<T> {

        // Node associated with the return value from the previous call to next()
        // Required to maintain this value for calls to remove()
        Node<T> current = null;

        // Node associated with the return value for the next call to next()
        Node<T> next = null;

        // Value to be returned from the next call to next()
        T nextItem = null;

        Head<T> head = currentHead.get();

        Node<T> end = head == expeditedHead ? nonExpeditedHead : null;

        QueueIterator() {
            calculateNext();
        }

        private T calculateNext() {
            current = next;
            Node<T> nextCandidate = next == null ? getFirstWithAction(head, end, GET_FIRST_NODE) : getNext(next, head, end);

            while (true) {
                T item;
                boolean doReturn = true;
                if (nextCandidate == null) {
                    // If the current head is the expeditedHead, then update head and end to the non expedited portion
                    // of the queue and get the first one there and start back at the beginning of the queue
                    if (head == expeditedHead) {
                        head = nonExpeditedHead;
                        end = null;
                        nextCandidate = getFirstWithAction(head, end, GET_FIRST_NODE);
                        continue;
                    }
                    item = null;
                } else {
                    item = nextCandidate.get();
                    doReturn = (item != null);
                }

                if (doReturn) {
                    next = nextCandidate;
                    T returnVal = nextItem;
                    nextItem = item;
                    return returnVal;
                }

                // If the Node was non null, but the item in it was null meaning it was removed, then
                // move to the next Node
                nextCandidate = getNext(nextCandidate, head, end);
            }
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            return calculateNext();
        }

        @Override
        public void remove() {
            // Using a local variable to avoid remove being called concurrently causing
            // a NullPointerException if current gets nulled out
            Node<T> node = current;
            if (node == null) {
                throw new IllegalStateException();
            }
            T prevItem = node.get();
            if (prevItem != null && node.compareAndSet(prevItem, null)) {
                size.reducePermits(1);
            }
            // Null out unconditionally since a remove on the queue could be called at the same time
            // as remove on the iterator which would cause the above if statement to not process.
            // Calling Iterator.remove() twice should always throw an IllegalStateException even
            // if the remove() didn't actually remove the entry.
            current = null;
        }
    }

    /**
     * Add the provided Object into the queue.
     *
     * If the Object is a QueueItem and it is marked expedited, add it to the end of the expedited part of the queue,
     * otherwise add it to the end of the non-expedited part of the queue.
     *
     * @param item Object to be added to the queue
     */
    @Override
    public boolean offer(T item) {
        if (item == null) {
            throw new NullPointerException();
        }

        AtomicReference<Node<T>> tail;
        Head<T> head;
        Node<T> end;
        Node<T> newTail = new Node<T>(item);

        // If it is an expedited item
        if (item instanceof QueueItem && ((QueueItem) item).isExpedited()) {
            // If this is the first expedited item, update current head to be the expedited head
            if (currentHead.get() == nonExpeditedHead) {
                currentHead.compareAndSet(nonExpeditedHead, expeditedHead);
            }
            tail = expeditedTail;
            head = expeditedHead;
            end = nonExpeditedHead;
            newTail.next.set(nonExpeditedHead);
        } else {
            tail = nonExpeditedTail;
            head = nonExpeditedHead;
            end = null;
        }

        Node<T> startingTail = tail.get();
        Node<T> currentTail = startingTail;
        while (true) {
            Node<T> currentTailNext = currentTail.next.get();
            // if the current tail's next is the end of the portion of the queue being inserted into
            if (currentTailNext == end) {
                // if we win the race to update the tail's next, the new item is added to the queue
                if (currentTail.next.compareAndSet(end, newTail)) {
                    // increase the queue size
                    size.release();

                    // don't update the tail each time.  update the tail on a later insert into the queue
                    if (currentTail != startingTail) {
                        tail.compareAndSet(startingTail, newTail);
                    }
                    return true;
                }

                // if the current tail's next points to itself, it means that element was removed
            } else if (currentTail == currentTailNext) {
                // get the current tail and start over
                Node<T> possibleNewTail = tail.get();
                currentTail = startingTail != possibleNewTail ? possibleNewTail : head.getFirst();
                startingTail = possibleNewTail;

                // on a secondary update to the queue, we jump to the tail's next to hopefully be the end of the queue now
            } else if (currentTail == startingTail) {
                currentTail = currentTailNext;
            } else {
                // get the current tail and start over
                Node<T> possibleNewTail = tail.get();
                currentTail = startingTail != possibleNewTail ? possibleNewTail : currentTailNext;
                startingTail = possibleNewTail;
            }
        }
    }

    @Override
    public boolean offer(T item, long time, TimeUnit timeout) throws InterruptedException {
        return offer(item); // size is unlimited so all adds are non-blocking
    }

    @Override
    public T peek() {
        Head<T> head = currentHead.get();
        return getFirstWithAction(head, null, GET_FIRST_ITEM);
    }

    @Override
    public T poll() {
        while (size.tryAcquire()) {
            Head<T> head = currentHead.get();
            T first = getFirstWithAction(head, null, REMOVE_FIRST_ITEM);
            if (first != null) {
                return first;
            }
            size.release(); // another thread is removing, put the permit back
            Thread.yield();
        }
        return null;
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        for (long start = System.nanoTime(), remain = timeout = unit.toNanos(timeout); //
                        remain >= 0 && size.tryAcquire(remain, TimeUnit.NANOSECONDS); //
                        remain = timeout - (System.nanoTime() - start)) {
            Head<T> head = currentHead.get();
            T first = getFirstWithAction(head, null, REMOVE_FIRST_ITEM);
            if (first != null) {
                return first;
            }
            size.release(); // another thread is removing, put the permit back
            Thread.yield();
        }
        return null;
    }

    @Override
    public void put(T item) throws InterruptedException {
        offer(item);
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Remove the provided Object from the queue.
     *
     * If the Object is a QueueItem and it is marked expedited, remove it from the expedited part of the queue,
     * otherwise remove it from the non-expedited part of the queue.
     *
     * @param item Object to be removed from the queue
     */
    @Override
    public boolean remove(Object item) {
        if (item == null) {
            return false;
        }

        Head<T> head;
        Node<T> end;

        if (item instanceof QueueItem && ((QueueItem) item).isExpedited()) {
            // If it is an expedited item, but no expedited items have been added, return false
            if (currentHead.get() == nonExpeditedHead) {
                return false;
            }
            head = expeditedHead;
            end = nonExpeditedHead;
        } else {
            head = nonExpeditedHead;
            end = null;
        }

        Node<T> prev = null;
        Node<T> next = null;
        Node<T> current = getFirstWithAction(head, end, GET_FIRST_NODE);
        for (; current != null; prev = current, current = next) {
            boolean removed = false;
            T element = current.get();
            if (element != null) {
                if (!item.equals(element)) {
                    next = getNext(current, head, end);
                    continue;
                }
                removed = current.compareAndSet(element, null);
            }

            next = getNext(current, head, end);
            if (prev != null && next != null) {
                prev.next.compareAndSet(current, next);
            }
            if (removed) {
                size.reducePermits(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public final int size() {
        int s = size.availablePermits();
        return s < 0 ? 0 : s;
    }

    @Override
    public T take() throws InterruptedException {
        while (true) {
            size.acquire();

            Head<T> head = currentHead.get();
            T first = getFirstWithAction(head, null, REMOVE_FIRST_ITEM);
            if (first != null) {
                return first;
            }
            size.release(); // another thread is removing, put the permit back
            Thread.yield();
        }
    }

    <F> F getFirstWithAction(Head<T> head, Node<T> end, FirstAction<T, F> firstAction) {
        Node<T> first = head.getFirst();
        Node<T> current = first;
        Node<T> firstActionEnd = head == expeditedHead ? nonExpeditedHead : null;
        while (true) {
            F returnVal = firstAction.apply(current, head, firstActionEnd, first);
            // can be null if the first element was removed and the head point wasn't updated
            // or if we lost the race and another thread got it
            if (returnVal != null) {
                return returnVal;
            }
            Node<T> next = current.next.get();

            if (next == firstActionEnd) {
                // Update first if there were removed elements in this section of the queue
                firstAction.updateFirst(head, first, current);

                // If we got to the planned end, return null
                if (firstActionEnd == end) {
                    return null;
                }

                // if we are doing a full scan and the next element is the non expedited head
                // we skip it and move onto first element of the non expedited head
                head = nonExpeditedHead;
                current = first = head.getFirst();
                firstActionEnd = null;

                // if we hit an element that was removed from the queue, start over from the beginning
            } else if (current == next) {
                current = first = head.getFirst();
            } else {
                current = next;
            }
        }
    }

    /**
     * Instances of the class do an action on the current Node and if the return value is not null
     * it also uses the head, current first to determine if the first needs to be reset.
     *
     * @param <T>
     * @param <R>
     */
    private static abstract class FirstAction<T, R> {

        abstract R apply(Node<T> current, Head<T> head, Node<T> end, Node<T> currentFirst);

        final void updateFirst(Head<T> head, Node<T> currentFirst, Node<T> newFirst) {
            if (currentFirst != newFirst && head.compareAndSetFirst(currentFirst, newFirst)) {
                // update first to point to itself to indicate that it has been removed from the queue
                currentFirst.next.set(currentFirst);
            }
        }
    }

    private static class RemoveFirstItem<T> extends FirstAction<T, T> {
        @SuppressWarnings("rawtypes")
        static final RemoveFirstItem INSTANCE = new RemoveFirstItem();

        @Override
        public T apply(Node<T> current, Head<T> head, Node<T> end, Node<T> currentFirst) {
            T item = current.get();
            if (item == null || !current.compareAndSet(item, null)) {
                return null;
            }

            if (current != currentFirst) {
                Node<T> next = current.next.get();
                // Must leave a valid Node for first and and tail to point to, so if next is the end
                // of the queue, use current instead so that we don't end up causing things to get out of sync
                Node<T> newFirst = (next == end) ? current : next;
                updateFirst(head, currentFirst, newFirst);
            }
            return item;
        }
    }

    private static class GetFirstNode<T> extends FirstAction<T, Node<T>> {
        @SuppressWarnings("rawtypes")
        static final GetFirstNode INSTANCE = new GetFirstNode();

        @Override
        public Node<T> apply(Node<T> current, Head<T> head, Node<T> end, Node<T> currentFirst) {
            T item = current.get();
            if (item == null) {
                return null;
            }

            updateFirst(head, currentFirst, current);
            return current;
        }
    }

    private static class GetFirstItem<T> extends FirstAction<T, T> {
        @SuppressWarnings("rawtypes")
        static GetFirstItem INSTANCE = new GetFirstItem();

        @Override
        public T apply(Node<T> current, Head<T> head, Node<T> end, Node<T> currentFirst) {
            T item = current.get();
            if (item != null) {
                updateFirst(head, currentFirst, current);
            }

            return item;
        }
    }

    /**
     * Represents the queue in the form:
     *
     * <pre>
     * SIZE [A, B, C, D, E]
     * </pre>
     *
     * If the size is > 100, only the first 100 are shown.
     *
     * <p>The string value generated by this method is only meaningful when no modifications are being made for
     * the duration of the method.</p>
     *
     * @return string representing this data structure.
     */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append(size.availablePermits()).append(' ');
        Iterator<T> it = iterator();
        if (!it.hasNext()) {
            b.append("[]");
        } else {
            b.append('[');
            int i = 0;
            while (true) {
                i++;
                b.append(it.next());
                if (!it.hasNext()) {
                    b.append(']');
                    break;
                }
                if (i == 100) {
                    b.append(", ...]");
                    break;
                }
                b.append(", ");
            }
        }

        return b.toString();
    }
}