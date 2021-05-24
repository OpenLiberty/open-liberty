/*
 * =============================================================================
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 * =============================================================================
 */
package com.ibm.ws.transport.iiop.internal;

import java.util.Optional;
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
    private interface Ref<T> extends Supplier<T> {
        default boolean isTransitional() { return false; }
    }

    private static class TransitionalRef<T> implements Ref<T> {
        private final CountDownLatch latch = new CountDownLatch(1);
        public boolean isTransitional() {
            try {
                latch.await();
            } catch (InterruptedException ignored) {}
            return true;
        }
        public void markTransitioned() { latch.countDown(); }
        public T get() { throw new UnsupportedOperationException();  }
    }

    private final AtomicReference<Ref<T>> atomicReference;

    public AcidReference(T initial) { this.atomicReference = new AtomicReference<>(() -> initial); }
    public AcidReference() { this(null); }

    /**
     * Observe the <em>consistent</em>>, <em>durable</em>> value held by this reference.
     */
    public T get() { return getWithWait().get(); }

    private Ref<T> getWithWait() {
	int contention = -1;
        for (;;) {
            Ref<T> ref = atomicReference.get();
            contention++;
            if (ref.isTransitional()) continue;
            if (contention > 0) System.out.println("### contention: " + contention);
            return ref;
        }
    }

    /**
     * Invokes the supplied function to determine if an update is required.
     * If an update is required, this is performed <em>atomically</em> and <em>in isolation</em>.
     * <br>
     * <strong>N.B. If the update computation throws an exception, the reference will not be updated.</strong>
     *
     * @param updaterSupplier This function should be side-effect free and inexpensive.
     *                        It should return {@link Optional#empty()} if no update is required.
     *                        The resulting updater, if present, will be invoked at most once.
     *
     * @return true if an update was performed as a result of this invocation and false otherwise
     */
    public boolean update(Function<T, Supplier<T>> updaterSupplier) {
        Ref<T> ref;
        TransitionalRef<T> tranRef;
        Supplier<T> neededUpdate;
        do {
            ref = getWithWait();
            neededUpdate = updaterSupplier.apply(ref.get());
            if (null == neededUpdate) return false;
            tranRef = new TransitionalRef<>();
        } while (false == atomicReference.compareAndSet(ref, tranRef));
        try {
            T newT = neededUpdate.get();
            ref = () -> newT;
            return true;
        } finally {
            atomicReference.set(ref);
            tranRef.markTransitioned();
        }
    }
}
