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

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import com.ibm.ws.classloading.internal.util.BlockingList.Listener;
import com.ibm.ws.classloading.internal.util.BlockingList.Logger;
import com.ibm.ws.classloading.internal.util.BlockingList.Retriever;
import com.ibm.ws.classloading.internal.util.BlockingList.Slot;

/**
 * A factory to configure and create a {@link BlockingList}.
 * <p>
 * This class provides a fluent interface and convenient methods
 * for configuring and creating a {@link BlockingList}.
 * <p>
 * <strong>N.B. objects of this class are not thread-safe. Either
 * external guards should be used or each thread should start by
 * calling {@link #defineList()} anew.</strong>
 * 
 * 
 * @param <K> the type of the keys associated with the elements
 * @param <E> the type of the elements
 */
public class BlockingListMaker<K, E> {
    private static final Retriever<?, ?> NULL_RETRIEVER = new Retriever<Object, Void>() {
        @Override
        public Void fetch(Object key) throws ElementNotReadyException {
            throw new ElementNotReadyException();
        }
    };

    static final Listener<Object, Void> NULL_LISTENER = new Listener<Object, Void>() {
        @Override
        public void listenFor(Object key, Slot<? super Void> slot) {}
    };

    static final Logger NULL_LOGGER = new Logger() {
        @Override
        public void logTimeoutEvent(BlockingList<?, ?> list) {}
    };

    @SuppressWarnings("unchecked")
    private Retriever<? super K, ? extends E> retriever = (Retriever<K, E>) NULL_RETRIEVER;
    @SuppressWarnings("unchecked")
    private Listener<? super K, ? extends E> listener = (Listener<K, E>) NULL_LISTENER;
    private Logger logger = NULL_LOGGER;
    private long nanoTimeout;
    private Collection<? extends K> keys;

    @SuppressWarnings("unchecked")
    private void setRetriever(Retriever<K, E> retriever) {
        this.retriever = retriever == null ? (Retriever<K, E>) NULL_RETRIEVER : retriever;
    }

    @SuppressWarnings("unchecked")
    private void setListener(Listener<K, E> listener) {
        this.listener = listener == null ? (Listener<K, E>) NULL_LISTENER : listener;
    }

    private BlockingListMaker() {}

    public static <K, E> BlockingListMaker<K, E> defineList() {
        return new BlockingListMaker<K, E>();
    }

    /** Construct a list */
    public <K2 extends K, E2 extends E> BlockingList<K2, E2> make() {
        @SuppressWarnings("unchecked")
        BlockingListMaker<K2, E2> stricterThis = (BlockingListMaker<K2, E2>) this;
        return stricterThis.internalCreateBlockingList();
    }

    private BlockingList<K, E> internalCreateBlockingList() {
        return new BlockingList<K, E>(keys, retriever, listener, logger, nanoTimeout);
    }

    /** Define the {@link Retriever} to use to retrieve elements on demand. */
    public <K2 extends K, E2 extends E> BlockingListMaker<K2, E2> fetchElements(Retriever<K2, E2> retriever) {
        @SuppressWarnings("unchecked")
        BlockingListMaker<K2, E2> stricterThis = (BlockingListMaker<K2, E2>) this;
        stricterThis.setRetriever(retriever);
        return stricterThis;
    }

    /** Define the {@link Listener} to use to get a callback when an element becomes available. */
    public <K2 extends K, E2 extends E> BlockingListMaker<K2, E2> listenForElements(Listener<K2, E2> listener) {
        @SuppressWarnings("unchecked")
        BlockingListMaker<K2, E2> stricterThis = (BlockingListMaker<K2, E2>) this;
        stricterThis.setListener(listener);
        return stricterThis;
    }

    /** Define the logger to use to log interesting events within the list */
    public BlockingListMaker<K, E> log(Logger logger) {
        this.logger = logger == null ? NULL_LOGGER : logger;
        return this;
    }

    /** Specify the total time to wait for the elements of the list to become available */
    public BlockingListMaker<K, E> waitFor(long time, TimeUnit unit) {
        this.nanoTimeout = time == 0 ? 1 : NANOSECONDS.convert(time, unit);
        return this;
    }

    /** Specify the keys associated with the eventual elements of this list */
    public <K2 extends K> BlockingListMaker<K2, E> useKeys(Collection<? extends K2> keys) {
        @SuppressWarnings("unchecked")
        BlockingListMaker<K2, E> stricterThis = (BlockingListMaker<K2, E>) this;
        stricterThis.keys = keys;
        return stricterThis;
    }

    /** Specify the keys associated with the eventual elements of this list */
    public <K2 extends K> BlockingListMaker<K2, E> useKeys(K2... keys) {
        @SuppressWarnings("unchecked")
        BlockingListMaker<K2, E> stricterThis = (BlockingListMaker<K2, E>) this;
        stricterThis.keys = Arrays.asList(keys);
        return stricterThis;
    }

    public BlockingListMaker<K, E> waitIndefinitely() {
        this.nanoTimeout = 0; // special value meaning never time out
        return this;
    }

}
