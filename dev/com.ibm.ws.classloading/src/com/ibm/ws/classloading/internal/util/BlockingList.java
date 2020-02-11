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

import static com.ibm.ws.classloading.internal.util.BlockingList.PlaceHolder.FAILED;
import static com.ibm.ws.classloading.internal.util.BlockingList.PlaceHolder.PENDING;
import static com.ibm.ws.classloading.internal.util.BlockingList.PlaceHolder.UNAVAILABLE;
import static com.ibm.ws.classloading.internal.util.BlockingList.PlaceHolder.couldStillTurnUp;
import static com.ibm.ws.classloading.internal.util.BlockingList.State.BLOCKING;
import static com.ibm.ws.classloading.internal.util.BlockingList.State.COMPLETE;
import static com.ibm.ws.classloading.internal.util.BlockingList.State.COMPLETE_WITH_FAILURES;
import static com.ibm.ws.classloading.internal.util.BlockingList.State.TIMED_OUT;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.Traceable;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A fixed size list that initially has all its elements marked as unavailable.
 * Elements are represented initially by a collection or array of keys, and these
 * keys must be used to {@link #put(Object, Object) put} the elements into the list.
 * Elements can be set no more than once in each slot. Any attempt to get an
 * element that has not yet been set will block until the element is available.
 * <p>
 * If a timeout value is provided, an attempt to get an unavailable element will
 * time out after the specified interval has elapsed. After this point, the list
 * will behave as if the unavailable elements do not exist and no further attempts
 * to retrieve elements will block.
 * <p>
 * Note that the timeout value provided specifies the <strong>total</strong> wait
 * time for this list. This mechanism is provided so that the maximum delay caused
 * by calls to this list can be specified, regardless of the number of accesses.
 * Since blocking calls can come from multiple threads concurrently, a best effort
 * attempt will be made to track the elapsed wait time allowing for concurrency.
 * <p>
 * This class does not log anything directly, but does provide a {@link Logger} interface
 * 
 * @param <K> the type of the keys provided as placeholders for the elements
 * @param <E> the type of the elements
 */

public class BlockingList<K, E> extends AbstractList<E> implements List<E>, Traceable {
    static final TraceComponent tc = Tr.register(BlockingList.class);

    @com.ibm.websphere.ras.annotation.Trivial
    enum PlaceHolder {
        /** A place holder for an element that is not yet available. */
        UNAVAILABLE,
        /** A place holder for an element that this collection has created a listener for */
        PENDING,
        /** A place holder for an element that is never going to be available. */
        FAILED;
        static boolean couldStillTurnUp(Object o) {
            return o == UNAVAILABLE || o == PENDING;
        }
    }

    /**
     * The behavioural state of the {@link BlockingList} is encoded
     * in this enumeration.
     */
    @com.ibm.websphere.ras.annotation.Trivial
    enum State {
        BLOCKING,
        TIMED_OUT,
        COMPLETE_WITH_FAILURES,
        COMPLETE
    }

    /** An interface to retrieve an element for a particular key. Not expected to block */
    public interface Retriever<K, E> {
        E fetch(K key) throws ElementNotReadyException, ElementNotValidException;
    }

    /** An object representing a slot in a {@link BlockingList} */
    public interface Slot<E> {
        /** Provide a value for an awaited element. */
        void fill(E e);

        /** Permanently fail to provide a value for an awaited element. */
        void delete();
    }

    private final class SlotImpl implements Slot<E> {
        private final K k;

        private SlotImpl(K k) {
            this.k = k;
        }

        @Override
        public void fill(E e) {
            putIfAbsent(k, e);
        }

        @Override
        public void delete() {
            fail(k);
        }
    }

    /** An interface to start listening for a callback, used when the {@link Retriever} fails */
    public interface Listener<K, E> {
        /**
         * Start listening for the element relating to <code>key</code> to arrive.
         * Once the element is available <code>slot.fill(element)</code> should be invoked.
         */
        void listenFor(K key, Slot<? super E> slot);
    }

    /** A plugin interface to log events that occur in the list. */
    public interface Logger {
        void logTimeoutEvent(BlockingList<?, ?> list);
    }

    /**
     * Read and write locks to guard state to allow concurrent readers.
     * Also provides wait-and-notify semantics while avoiding deadlock.
     * If a timeout is specified, this object will also manage the timeout.
     */
    private final EventReadWriteLock stateLock;

    /** The elements, stored in the position of their corresponding keys */
    private final Object[] elements;
    /** A map from keys to indices */
    private final Map<K, Integer> actualIndices = new LinkedHashMap<K, Integer>();
    /** A map from effective indices to actual indices */
    private final int[] effectiveIndex;
    /** The keys in their original positions */
    private final Object[] keys;
    /** Failed keys */
    private Collection<K> failedKeys;
    /** Used to retrieve the value from the key if it is available. */
    private final Retriever<? super K, ? extends E> retriever;
    /** Used to start listening for an element to arrive if the {@link Retriever} fails. */
    private final Listener<? super K, ? extends E> listener;
    /**  */
    private final Logger logger;
    /** The object's behavioural state */
    private volatile State state;

    /**
     * Create a list that blocks until a specified timeout when retrieving objects that are not yet available.
     * 
     * @param retriever The {@link Retriever} to use to retrieve the elements from the keys. May NOT be <code>null</code>.
     * @param listener The {@link Listener} to use to start listening for elements if they cannot be fetched using the retriever. May NOT be null.
     * @param logger The logger plugin to use to log timeout events within the list. May NOT be <code>null</code>.
     * @param nanoTimeout The total number of milliseconds to wait for all elements to become available.
     *            0 is a special value used to indicate that the list should block indefinitely.
     * @param keys The keys to use. May be <code>null</code>.
     *            Duplicate keys will be ignored.
     *            Order of iteration will be used to
     *            define the order of elements in the list
     *            and if a specific order is required,
     *            a {@link List} should be provided.
     */
    BlockingList(Collection<? extends K> keys, Retriever<? super K, ? extends E> retriever, Listener<? super K, ? extends E> listener, Logger logger, long nanoTimeout) {
        assert retriever != null;
        assert listener != null;
        assert logger != null;
        this.retriever = retriever;
        this.listener = listener;
        this.logger = logger;
        this.stateLock = new EventReadWriteLock(nanoTimeout);
        // need to use locking here because the Java memory model does not guarantee that non-final, 
        // non-volatile fields of an object will be initialised before another thread can see the constructed object
        stateLock.writeLock().lock();
        try { // don't really need try/finally in this case but let's preserve the idiom everywhere
            if (keys == null)
                keys = Collections.emptyList();
            this.elements = new Object[keys.size()];
            Arrays.fill(this.elements, UNAVAILABLE);
            this.state = keys.isEmpty() ? COMPLETE : BLOCKING;
            // create an array filled with a too-high index so sorted inserts fill up from slot 0
            this.effectiveIndex = new int[keys.size()];
            Arrays.fill(effectiveIndex, Integer.MAX_VALUE);
            int i = 0;
            for (K k : keys) {
                if (actualIndices.containsKey(k))
                    continue;
                actualIndices.put(k, i++);
            }
            this.keys = keys.toArray();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Insert <code>index</code> into the {@link #effectiveIndex} array,
     * preserving the natural ordering, and shifting successive elements to the right
     */
    private void recordIndex(int index) {
        stateLock.writeLock().lock();
        try {
            int insertionPoint = -Arrays.binarySearch(effectiveIndex, index) - 1;
            System.arraycopy(effectiveIndex, insertionPoint, effectiveIndex, insertionPoint + 1, effectiveIndex.length - insertionPoint - 1);
            effectiveIndex[insertionPoint] = index;
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /**
     * Block until the element at <code>index</code> becomes available or the
     * remaining timeout is exceeded.
     * <p>
     * If the timeout is exceeded, this list will behave as a smaller list
     * with only the available elements in it. This method converts the
     * supplied index into the appropriate one to use if the timed-out behaviour
     * is in effect.
     * <p>
     * If the timeout is reached this method will also modify the state of the list
     * to {@link State#TIMED_OUT}.
     * 
     * @return true if the element arrived, false otherwise
     */
    @SuppressWarnings("unchecked")
    @Override
    public E get(int index) {
        stateLock.readLock().lock();
        try {
            switch (state) {
                case BLOCKING:
                    if (waitForIndex(index)) {
                        return (E) elements[index];
                    }
                    // FALL THROUGH because waitForIndex() returned false and this list is now timed out
                case TIMED_OUT:
                    // FALL THROUGH because TIMED_OUT and COMPLETE_WITH_FAILURES both involve missing elements
                case COMPLETE_WITH_FAILURES:
                    return (E) elements[effectiveIndex[index]];
                case COMPLETE:
                    return (E) elements[index];
                default:
                    throw new IllegalStateException();
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Attempt to retrieve the element at the specified index, using the provided {@link Retriever}.
     * This method will release all read locks held by this thread, then acquire a write lock.
     * It will then re-acquire the same number of read locks before exiting the method.
     * 
     * @param index the index of the key whose associated element is required
     * @return true if the element was found or has failed permanently, false otherwise
     */
    private boolean tryToFetchElement(int index) {
        // must release read locks before acquiring write lock
        final int readLocks = stateLock.releaseReadLocksAndAcquireWriteLock();
        try {
            return couldStillTurnUp(elements[index]) ? reallyTryToFetchElement(index) : true;
        } finally {
            stateLock.downgradeWriteLockToReadLocks(readLocks);
        }
    }

    /** @return true if the element was found or has failed permanently, false otherwise */
    @FFDCIgnore({ ElementNotReadyException.class, ElementNotValidException.class })
    private boolean reallyTryToFetchElement(int index) {
        final String methodName = "reallyTryToFetchElement(): ";
        @SuppressWarnings("unchecked")
        final K k = (K) keys[index];
        boolean result;
        try {
            try {
                // try to fetch synchronously
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "retriever fetching");
                put(k, retriever.fetch(k));
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "retriever fetched");
                result = true;
            } catch (ElementNotReadyException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "retriever threw NotReadyException");
                // only listen if we haven't already started listening
                if (elements[index] == UNAVAILABLE) {
                    // Mark element as already being listened for,
                    // avoiding concurrent threads entering 
                    // (or this thread re-entering) this if block.
                    elements[index] = PENDING;
                    // Make sure we aren't holding any locks while we execute the external code.
                    // This should avoid deadlock conditions between this lock and locks in the external code.
                    final int writeLocks = stateLock.releaseWriteLocks();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "AFTER releaseWriteLocks");
                    try {
                        listener.listenFor(k, new SlotImpl(k));
                    } finally {
                        // restore write-lock state
                        stateLock.acquireWriteLocks(writeLocks);
                    }
                    // EVIL TIMING WINDOW: listener may miss element if it JUST arrived.
                    // Try fetching again anyway, just in case.
                    if (couldStillTurnUp(elements[index])) {
                        try {
                            putIfAbsent(k, retriever.fetch(k));
                            result = true;
                        } catch (ElementNotReadyException ignored) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, methodName + "Caught NotReadyException while putting a retriever");
                        }
                    }
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "Not creating listener: since elements[" + index + "] != UNAVAILABLE");
                }
            }
        } catch (ElementNotValidException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "retriever reported element as invalid for key " + k, e);
            fail(k);
        }
        result = !!!couldStillTurnUp(elements[index]);
        return result;
    }

    /**
     * Block until the element at <code>index</code> becomes available.
     * 
     * @return true if the element is available, false otherwise
     */
    @FFDCIgnore(InterruptedException.class)
    private boolean waitForIndex(int index) {
        final String methodName = "waitForIndex(): ";
        while (!!!tryToFetchElement(index)) {
            try {
                if (stateLock.hasTimedOut()) {
                    markTimedOut();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, methodName + "TIMED OUT!");
                    return false;
                }
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "waiting ...");
                boolean result = stateLock.waitForEvent();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "waitForEvent result is [" + result + "] will Loop again to check timeout");
            } catch (InterruptedException ignored) {
                // NO FFDC
                if (tc.isDebugEnabled())
                    Tr.debug(tc, methodName + "interrupted");
            }
        }
        return true;
    }

    @FFDCIgnore({ ElementNotReadyException.class, ElementNotValidException.class })
    private void markTimedOut() {
        final String methodName = "markTimedOut(): ";
        int lockCount = stateLock.releaseReadLocksAndAcquireWriteLock();
        try {
            state = TIMED_OUT;
            if (retriever != null)
                for (K k : getUnmatchedKeys())
                    try {
                        E e = retriever.fetch(k);
                        put(k, e);
                    } catch (ElementNotReadyException ignored) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + "retriever failed to retrieve element at timeout for key " + k);
                    } catch (ElementNotValidException e) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, methodName + "retriever reported element as invalid for key " + k);
                        fail(k);
                    }
            if (logger != null)
                logger.logTimeoutEvent(this);
        } finally {
            stateLock.downgradeWriteLockToReadLocks(lockCount);
        }
    }

    @FFDCIgnore(IndexOutOfBoundsException.class)
    private void preFetchAll() {
        // ok to have concurrent threads in here,
        // and grabbing a write lock would prevent 
        // us from receiving asynchronous updates

        // 1) try a non-blocking pre-fetch on all elements
        for (int i = 0; i < elements.length; i++) {
            tryToFetchElement(i);
        }
        // 2) now wait for any missing elements, up to the specified timeout 
        try {
            for (int i = 0; i < elements.length; i++) {
                get(i);
            }
        } catch (IndexOutOfBoundsException ignored) {
            // some stuff was missing - carry on without it
        }
    }

    /**
     * Add the element associated with the given key into the list at the established position for that key.
     * 
     * @param key the key to use to add the element, which must match one of the keys provided to the constructor
     * @param element the element to add
     * @throws IllegalArgumentException if the key does not match one of the keys provided on construction, or if an element has already been provided for that key
     */
    public void put(K key, E element) {
        final String methodName = "put(): ";
        stateLock.writeLock().lock();
        try {
            Integer index = actualIndices.remove(key);
            if (index == null)
                throw new IllegalArgumentException("unknown key: " + key);
            elements[index] = element;
            recordIndex(index);
            checkForCompletion();
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "element found for key: " + key);
            stateLock.postEvent();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /** Permanently fail to retrieve the element associated with the key, independently of any timeout. */
    private void fail(final K key) {
        final String methodName = "fail(): ";
        stateLock.writeLock().lock();
        try {
            Integer index = actualIndices.remove(key);
            if (index == null)
                throw new IllegalArgumentException("unknown key: " + key);
            elements[index] = FAILED;
            // register that this key failed
            (failedKeys == null ? failedKeys = new ArrayList<K>(actualIndices.size() + 1) : failedKeys).add(key);
            checkForCompletion();
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName, "permanent fail for key " + key);
            stateLock.postEvent();
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    /** if all the keys have been satisfied or permanently failed, mark this list as being complete */
    private void checkForCompletion() {
        final String methodName = "checkForCompletion(): ";
        // mark the list complete if that was the last awaited key
        if (actualIndices.isEmpty()) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, methodName + "setting state=COMPLETE");
            state = failedKeys == null
                            ? COMPLETE
                            : COMPLETE_WITH_FAILURES; // state change must be last action
        }
    }

    /**
     * Add the element associated with the given key into the list at the established position for that key.
     * 
     * @param key the key to use to add the element, which must match one of the keys provided to the constructor
     * @param element the element to add
     * @return <code>true</code> if the element was added and <code>false</code> otherwise
     */
    @FFDCIgnore(IllegalArgumentException.class)
    public boolean putIfAbsent(K key, E element) {
        try {
            put(key, element);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Find the size of this list, attempting to retrieve all elements if necessary.
     */
    @Override
    public int size() {
        stateLock.readLock().lock();
        try {
            switch (state) {
                default:
                    // This list might be empty or full or somewhere in between:
                    // we don't know until we look!
                    preFetchAll();
                    // The list is now either complete or timed out - recurse to find the result.
                    return size();
                case TIMED_OUT:
                    return currentSize();
                case COMPLETE_WITH_FAILURES:
                    return elements.length - failedKeys.size();
                case COMPLETE:
                    return elements.length;
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    private int currentSize() {
        stateLock.readLock().lock();
        try {
            return failedKeys == null
                            ? elements.length - actualIndices.size()
                            : elements.length - actualIndices.size() - failedKeys.size();
        } finally {
            stateLock.readLock().unlock();
        }
    }

    /**
     * Get a list of only those elements that have already been set.
     * <p>
     * <em>N.B. this method is thread-safe but does not guarantee a consistent view of the list contents in the face of concurrent updates.</em>
     * 
     * @return a dynamically updated list showing only the currently available elements
     */
    public List<E> getCondensedList() {
        return new AbstractList<E>() {

            @SuppressWarnings("unchecked")
            @Override
            public E get(int arg0) {
                stateLock.readLock().lock();
                try {
                    return (E) elements[effectiveIndex[arg0]];
                } finally {
                    stateLock.readLock().unlock();
                }
            }

            @Override
            public int size() {
                return currentSize();
            }

        };
    }

    /**
     * Blocking call to ensure an element is available
     * 
     * @return false if the element is unavailable and the list has timed out
     */
    @FFDCIgnore(IndexOutOfBoundsException.class)
    private boolean ensureElement(int index) {
        try {
            get(index);
            return true;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
    }

    @Override
    public Iterator<E> iterator() {
        return listIterator();
    }

    @Override
    public ListIterator<E> listIterator(final int index) {
        return new ListIterator<E>() {
            ListIterator<E> internal = BlockingList.super.listIterator(index);

            @Override
            public void add(E e) {
                internal.add(e);
            }

            @Override
            public boolean hasNext() {
                // must ensure next element is retrievable
                return internal.hasNext() && BlockingList.this.ensureElement(nextIndex());
            }

            @Override
            public boolean hasPrevious() {
                return internal.hasPrevious() && BlockingList.this.ensureElement(previousIndex());
            }

            @Override
            public E next() {
                return internal.next();
            }

            @Override
            public int nextIndex() {
                return internal.nextIndex();
            }

            @Override
            public E previous() {
                return internal.previous();
            }

            @Override
            public int previousIndex() {
                return internal.previousIndex();
            }

            @Override
            public void remove() {
                internal.remove();
            }

            @Override
            public void set(E e) {
                internal.set(e);
            }
        };
    }

    public boolean isTimedOut() {
        return state == TIMED_OUT;
    }

    @Override
    public String toString() {
        stateLock.readLock().lock();
        try {
            String elems = Arrays.toString(elements);
            return actualIndices.isEmpty()
                            ? failedKeys == null
                                            ? elems
                                            : "elements:" + elems + " failed keys: " + failedKeys
                            : failedKeys == null
                                            ? "elements:" + elems + " awaited keys:" + actualIndices.keySet()
                                            : "elements:" + elems + " awaited keys:" + actualIndices.keySet() + " failed keys: " + failedKeys;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public String toTraceString() {
        return toString();
    }

    /**
     * Get a point-in-time view of the unmatched keys.
     * This may be immediately out of date unless additional
     * synchronization is performed to prevent concurrent updates.
     */
    public Set<K> getUnmatchedKeys() {
        stateLock.readLock().lock();
        try {
            return new HashSet<K>(this.actualIndices.keySet());
        } finally {
            stateLock.readLock().unlock();
        }
    }
}
