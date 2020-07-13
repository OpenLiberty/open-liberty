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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * The BlockingQueue implementation takes the concepts from the DoubleQueue and ConcurrentLinkedQueue
 * and combines them together. The logic in this class is largely from both of those classes
 * with the appropriate updates needed to support having both a queue of priority elements
 * and non priority elements.
 *
 * Whereas ConcurrentLinkedQueue uses Unsafe to set fields, this implementation makes use of
 * AtomicReferences to give concurrency.
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

    final Node<T> expeditedHead;

    final AtomicReference<Node<T>> expeditedTail = new AtomicReference<>();

    final Node<T> nonExpeditedHead;

    final AtomicReference<Node<T>> nonExpeditedTail = new AtomicReference<>();

    /**
     * Count of items available for poll/removal.
     */
    private final ReduceableSemaphore size = new ReduceableSemaphore(0, false);

    @SuppressWarnings("unchecked")
    private final FirstAction<T, Node<T>> GET_FIRST_NODE = GetFirstNode.INSTANCE;
    @SuppressWarnings("unchecked")
    private final FirstAction<T, T> GET_FIRST_ITEM = GetFirstItem.INSTANCE;
    @SuppressWarnings("unchecked")
    private final FirstAction<T, T> REMOVE_FIRST_ITEM = RemoveFirstItem.INSTANCE;

    ConcurrentPriorityBlockingQueue() {
        Node<T> expeditedHead = new Node<>(null);
        Node<T> nonExpeditedHead = new Node<>(null);
        expeditedHead.next.set(nonExpeditedHead);
        this.expeditedHead = expeditedHead;
        this.expeditedTail.set(expeditedHead);
        this.nonExpeditedHead = nonExpeditedHead;
        this.nonExpeditedTail.set(nonExpeditedHead);
    }

    @Override
    public boolean add(T e) {
        return offer(e);
    }

    @Override
    public boolean contains(Object item) {
        if (item == null) {
            return false;
        }
        boolean isExpeditedItem = item instanceof QueueItem && ((QueueItem) item).isExpedited();

        Node<T> head = isExpeditedItem ? expeditedHead : nonExpeditedHead;
        Node<T> end = isExpeditedItem ? nonExpeditedHead : null;

        Node<T> e = getFirstWithAction(head, end, false, GET_FIRST_NODE);
        for (; e != null; e = getNext(e, head, end)) {
            T element = e.get();
            if (element != null && item.equals(element))
                return true;
        }
        return false;
    }

    private Node<T> getNext(Node<T> current, Node<T> head, Node<T> end) {
        Node<T> next = current.next.get();
        return (current == next) ? head.next.get() : (next != end ? next : null);
    }

    @Override
    public int drainTo(Collection<? super T> col) {
        int count = 0;
        for (T item; (item = poll()) != null; count++)
            col.add(item);
        return count;
    }

    @Override
    public int drainTo(Collection<? super T> col, int maxElements) {
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
        Node<T> current = null;
        Node<T> next = null;
        T nextItem = null;

        QueueIterator() {
            calculateNext();
        }

        private T calculateNext() {
            current = next;
            Node<T> prevNext = next;
            Node<T> nextCandidate = next == null ? getFirstWithAction(expeditedHead, null, true, GET_FIRST_NODE) : getNext(next, expeditedHead, null);

            while (true) {
                if (nextCandidate == nonExpeditedHead) {
                    nextCandidate = getNext(nextCandidate, nonExpeditedHead, null);
                    prevNext = null;
                }
                if (nextCandidate == null) {
                    T returnVal = nextItem;
                    next = null;
                    nextItem = null;
                    return returnVal;
                }

                T item = nextCandidate.get();
                if (item != null) {
                    T returnVal = nextItem;
                    next = nextCandidate;
                    nextItem = item;
                    return returnVal;
                }

                Node<T> prevNextCandidate = nextCandidate;
                nextCandidate = getNext(prevNextCandidate, expeditedHead, null);
                if (prevNext != null && nextCandidate != null && nextCandidate != nonExpeditedHead) {
                    prevNext.next.compareAndSet(prevNextCandidate, nextCandidate);
                }
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
            if (current == null)
                throw new IllegalStateException();

            T prevItem = current.get();
            if (prevItem != null && current.compareAndSet(prevItem, null)) {
                size.reducePermits(1);
            }
            current = null;
        }
    }

    @Override
    public boolean offer(T item) {
        if (item == null) {
            throw new NullPointerException();
        }
        boolean isExpeditedItem = item instanceof QueueItem && ((QueueItem) item).isExpedited();
        AtomicReference<Node<T>> tail = isExpeditedItem ? expeditedTail : nonExpeditedTail;
        Node<T> head = isExpeditedItem ? expeditedHead : nonExpeditedHead;
        Node<T> end = isExpeditedItem ? nonExpeditedHead : null;

        Node<T> newNode = new Node<T>(item);
        if (isExpeditedItem) {
            newNode.next.set(nonExpeditedHead);
        }

        Node<T> startingTail = tail.get();
        Node<T> currentTail = startingTail;
        while (true) {
            Node<T> tailNext = currentTail.next.get();
            if (tailNext == end) {
                if (currentTail.next.compareAndSet(end, newNode)) {
                    if (currentTail != startingTail) {
                        tail.compareAndSet(startingTail, newNode);
                    }
                    size.release();
                    return true;
                }
                continue;
            }
            if (currentTail == tailNext) {
                Node<T> possibleNewTail = tail.get();
                currentTail = startingTail != possibleNewTail ? possibleNewTail : head.next.get();
                startingTail = possibleNewTail;
            } else {
                if (currentTail == startingTail) {
                    currentTail = tailNext;
                } else {
                    Node<T> possibleNewTail = tail.get();
                    currentTail = startingTail != possibleNewTail ? possibleNewTail : tailNext;
                    startingTail = possibleNewTail;
                }
            }
        }
    }

    @Override
    public boolean offer(T item, long time, TimeUnit timeout) throws InterruptedException {
        return offer(item); // size is unlimited so all adds are non-blocking
    }

    @Override
    public T peek() {
        return getFirstWithAction(expeditedHead, null, true, GET_FIRST_ITEM);
    }

    @Override
    public T poll() {
        while (size.tryAcquire()) {
            T first = getFirstWithAction(expeditedHead, null, true, REMOVE_FIRST_ITEM);
            if (first != null) {
                //System.out.println("JHA: " + first);
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
            T first = getFirstWithAction(expeditedHead, null, true, REMOVE_FIRST_ITEM);
            if (first != null) {
                //System.out.println("JHA2: " + first);
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

    @Override
    public boolean remove(Object item) {
        if (item == null) {
            return false;
        }
        boolean isExpeditedItem = item instanceof QueueItem && ((QueueItem) item).isExpedited();

        Node<T> head = isExpeditedItem ? expeditedHead : nonExpeditedHead;
        Node<T> end = isExpeditedItem ? nonExpeditedHead : null;

        Node<T> prev = null;
        Node<T> next = null;
        Node<T> e = getFirstWithAction(head, end, false, GET_FIRST_NODE);
        for (; e != null; prev = e, e = next) {
            boolean removed = false;
            T element = e.get();
            if (element != null) {
                if (!item.equals(element)) {
                    next = getNext(e, head, end);
                    continue;
                }
                removed = e.compareAndSet(element, null);
            }

            next = getNext(e, head, end);
            if (prev != null && next != null) {
                prev.next.compareAndSet(e, next);
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

            T first = getFirstWithAction(expeditedHead, null, true, REMOVE_FIRST_ITEM);
            if (first != null) {
                return first;
            }
            size.release(); // another thread is removing, put the permit back
            Thread.yield();
        }
    }

    private <F> F getFirstWithAction(Node<T> head, Node<T> end, boolean fullScan,
                                     FirstAction<T, F> firstAction) {
        Node<T> first = head.next.get();
        Node<T> current = first;
        while (true) {
            Node<T> next;
            if (fullScan && current == nonExpeditedHead) {
                head = nonExpeditedHead;
                next = first = head.next.get();
            } else {
                F returnVal = firstAction.apply(current);
                if (returnVal != null) {
                    if (current != first) {
                        next = current.next.get();
                        Node<T> newNext = firstAction.getNewFirst(current, next, nonExpeditedHead);
                        if (head.next.compareAndSet(first, newNext)) {
                            first.next.set(first);
                        }
                    }
                    return returnVal;
                }
                next = current.next.get();
            }

            if (next == end) {
                if (head != current && first != current &&
                    head.next.compareAndSet(first, current)) {
                    first.next.set(first);
                }
                return null;
            } else if (current == next) {
                current = first = head.next.get();
            } else {
                current = next;
            }
        }
    }

    private interface FirstAction<T, R> extends Function<Node<T>, R> {
        Node<T> getNewFirst(Node<T> current, Node<T> next, Node<T> expeditedEnd);
    }

    private static class RemoveFirstItem<T> implements FirstAction<T, T> {
        @SuppressWarnings("rawtypes")
        static final RemoveFirstItem INSTANCE = new RemoveFirstItem();

        @Override
        public T apply(Node<T> node) {
            T item = node.get();
            return item != null && node.compareAndSet(item, null) ? item : null;
        }

        @Override
        public Node<T> getNewFirst(Node<T> current, Node<T> next, Node<T> expeditedEnd) {
            return next == expeditedEnd || next == null ? current : next;
        }
    }

    private static class GetFirstNode<T> implements FirstAction<T, Node<T>> {
        @SuppressWarnings("rawtypes")
        static final GetFirstNode INSTANCE = new GetFirstNode();

        @Override
        public Node<T> apply(Node<T> node) {
            T item = node.get();
            return item != null ? node : null;
        }

        @Override
        public Node<T> getNewFirst(Node<T> current, Node<T> next, Node<T> expeditedEnd) {
            return current;
        }
    }

    private static class GetFirstItem<T> implements FirstAction<T, T> {
        @SuppressWarnings("rawtypes")
        static GetFirstItem INSTANCE = new GetFirstItem();

        @Override
        public T apply(Node<T> node) {
            T item = node.get();
            return item != null ? item : null;
        }

        @Override
        public Node<T> getNewFirst(Node<T> current, Node<T> next, Node<T> expeditedEnd) {
            return current;
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
                if (!it.hasNext() || i == 100) {
                    b.append(']').toString();
                    break;
                }
                b.append(',').append(' ');
            }
        }

        return b.toString();
    }
}