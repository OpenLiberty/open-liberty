/*
 * =============================================================================
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.transport.iiop.internal;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Allow an expensive update computation to happen on a single thread even when contended.
 * Contending threads wait for the updating thread to complete before continuing.
 * <p>
 * The Java util classes including {@link AtomicReference} and {@link ClassValue}
 * allow threads to contend to update a value while selecting one clear winner.
 * However, they do NOT guarantee that the value computation will only happen once.
 */
public class AcidReference<T> {
    @FunctionalInterface
    private interface Ref<T> extends Supplier<T> {
        default boolean isReady() { return true; }
    }

    /** A placeholder ref to allow contending threads to wait for an in-flight update. */
    private static class TransitionalRef<T> implements Ref<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        private final Thread updaterThread = Thread.currentThread();
        public boolean isReady() {
            // It is not valid to try to retrieve the value from the updating thread while it is still updating
            if (Thread.currentThread() == updaterThread) throw new IllegalStateException("Attempt to retrieve value during update process");
            try {
                latch.await();
            } catch (InterruptedException ignored) {}
            return false;
        }
        void markAsReady() { latch.countDown(); }
        public T get() { throw new UnsupportedOperationException();  }
    }

    private final AtomicReference<Ref<T>> atomicReference;

    public AcidReference(T initial) { this.atomicReference = new AtomicReference<>(() -> initial); }

    public AcidReference() { this(null); }

    private Ref<T> getWithWait() {
        for (;;) {
            Ref<T> ref = atomicReference.get();
            if (ref.isReady()) return ref;
        }
    }

    /** Observe the <em>consistent</em>, <em>durable</em> value held by this reference. */
    public T get() { return getWithWait().get(); }

    /**
     * Invokes the supplied function to determine if an update is required.
     * If an update is required, this is performed <em>atomically</em> and <em>in isolation</em>.
     * <br>
     * <strong>N.B. If the update computation throws an exception, the reference will not be updated.</strong>
     *
     * @param updaterSupplier This function should be side-effect free and inexpensive.
     *                        It should return <code>null</code> if no update is required.
     *                        The resulting updater, if present, will be invoked at most once.
     *
     * @return true if an update was performed as a result of this invocation and false otherwise
     */
    public boolean update(Function<T, Supplier<T>> updaterSupplier) {
        Ref<T> ref = getWithWait();
        Supplier<T> neededUpdate = updaterSupplier.apply(ref.get()); // check what update is needed
        if (null == neededUpdate) return false; // early return: no update was needed
        final TransitionalRef<T> tranRef = new TransitionalRef<>();

        // bid for the work with an atomic compare-and-swap
        while (false == atomicReference.compareAndSet(ref, tranRef)) {
            // If the swap fails, some other thread has been allowed to perform the update.
            // This thread must wait for the other update to complete, and repeat the update logic above.
            // NOTE: original do-while loop unrolled to allow tranRef to be final
            ref = getWithWait();
            neededUpdate = updaterSupplier.apply(ref.get()); // check what update is needed
            if (null == neededUpdate) return false; // early return: no update was needed
        }
        // The compare-and-swap succeeded!
        // This thread must now update the value.
        try {
            // Compute the new value and hold it in a new (non-transitional) ref.
            final T newValue = neededUpdate.get();
            ref = () -> newValue;
            return true; // the update succeeded
        } finally { // handle commit or rollback
            atomicReference.set(ref); // ref holds the new value or the old one if an exception was thrown
            tranRef.markAsReady(); // wake up any waiting threads
        }
    }
}
