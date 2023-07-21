/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.app.manager.module.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//@formatter:off
/**
 * Simple cache of adaptable Ts.
 *
 * This cache is write-only, and is only suitable for use when no updates are made to
 * application shared libraries or their usage within a single server start.
 */
public class DeferredCache<T> {
    // Trace with the application manager.
    // TODO: Move DeferredCache to a more common location.
    private static final TraceComponent tc = Tr.register(DeferredCache.class,
                                                         "app.manager",
                                                         "com.ibm.ws.app.manager.module.internal.resources.Messages");

    //

    /**
     * Create a new deferred cache.  The cache is initially empty.
     */
    public DeferredCache() {
        this.storage = new HashMap<>();
    }

    // Store a caching producer.  That moves production outside of management of
    // storage.  The production call is expected to require a large amount of time.
    //
    // TODO: An additional context specific value might be added.  See the TODO in
    //       DeployedAppInfoBase in regards to the use of the library PID.

    private final Map<String, CaptureProducer<T>> storage;

    /**
     * Clear (empty) storage.
     *
     * The storage is guaranteed to be empty only for a moment.
     *
     * This operation is thread safe.
     */
    public void clear() {
        synchronized(storage) {
            storage.clear();
        }
    }

    /**
     * Answer the current size of the cache.
     *
     * The size is not valid after it is returned: The size is
     * valid only at the moment at which it is return.
     *
     * This operation is thread safe.
     *
     * @return A size of the cache.
     */
    public int size() {
        synchronized(storage) {
            return storage.size();
        }
    }

    /**
     * Create and return a snapshot of the cache.  This is created
     * as a table of snapshots of the current stored capturing producers.
     *
     * The snapshot is not valid after it is returned: The snapshot
     * has the state of the cache at the moment the
     * snapshot was made.
     *
     * This operation is thread safe.
     *
     * @return A snapshot of the cache.
     */
    public Map<String, CaptureProducer.SnapShot<T>> snapshot() {
        synchronized(storage) {
            Map<String, CaptureProducer.SnapShot<T>> snapshot = new HashMap<>( storage.size() );
            BiConsumer<String, CaptureProducer<T>> snapper =
                (String key, DeferredCache.CaptureProducer<T> captureProducer) ->
                    { snapshot.put(key,  captureProducer.snapshot()); };
            storage.forEach(snapper);
            return snapshot;
        }
    }

    /**
     * Defer invocation of a failable producer.  Cache the deferred product as
     * a capturing failable producer.
     *
     * Only the first invocation for a specific key will be placed into storage.
     * Subsequent invocations for the same key will obtain the previously placed
     * capturing producer, which uses the producer which was provided on the first
     * invocation.
     *
     * As a consequence, differences in the producer will be lost on second and
     * subsequent invocations.  The expectation is that the producers provided
     * for the second and subsequent invocations are the equivalent of the initial
     * producer.
     *
     * This operation is thread safe.
     *
     * @param key The key associated with the product.
     * @param baseProducer A producer to associate with the key.
     *
     * @return A capturing failable producer.
     */
    public CaptureProducer<T> defer(String key, FailableProducer<T> baseProducer) {
        Function<String, CaptureProducer<T>> producerProducer =
            (String useKey) -> new CaptureProducer<T>(useKey, baseProducer);
        synchronized(storage) {
            return storage.computeIfAbsent(key, producerProducer);
        }
    }

    //

    /**
     * Type for failable producers.
     *
     * @param <T> The type of the product.
     */
    @FunctionalInterface
    public static interface FailableProducer<T> {
        T produce() throws Exception;
    }

    /**
     * Concrete capturing failable producer.
     *
     * Wraps a failable producer and caches the product.
     *
     * @param <T> The type of the product.
     */
    public static class CaptureProducer<T> implements FailableProducer<T> {
        public CaptureProducer(String tag, FailableProducer<T> baseProducer) {
            this.tag = tag;

            this.baseProducer = baseProducer;

            this.capturedProduct = null;
            this.capturedException = null;
        }

        private final String tag; // A tag for debugging.

        public String getTag() {
            return tag;
        }

        private volatile FailableProducer<T> baseProducer;
        private T capturedProduct;
        private Exception capturedException;

        /**
         * Type used to answer snapshots of a capturing producer.
         *
         * This enables the state of the capturing producer to be
         * examined without forcing production.
         *
         * The snapshot is not valid after it is returned: The snapshot
         * has the state of the capturing producer at the moment the
         * snapshot was made.
         *
         * This operation is thread safe.
         *
         * @param <T> The type which is being produced.
         */
        public static class SnapShot<T> {
            public final FailableProducer<T> baseProducer;
            public final T capturedProduct;
            public final Exception capturedException;

            protected SnapShot(FailableProducer<T> baseProducer, T capturedProduct, Exception capturedException) {
                this.baseProducer = baseProducer;
                this.capturedProduct = capturedProduct;
                this.capturedException = capturedException;
            }
        }

        /**
         * Obtain a snapshot of the current state of this capturing producer:
         *
         * If production has not occurred, answer the base producer and null product and exception.
         *
         * If production has occurred, answer a null base producer and the captured product and
         * exception.
         *
         * This operation is thread safe.
         *
         * @return A snapshot of the current state of this capturing producer.
         */
        public SnapShot<T> snapshot() {
            if ( baseProducer == null ) {
                return new SnapShot<T>(baseProducer, capturedProduct, capturedException);
            } else {
                synchronized(this) {
                    return new SnapShot<T>(baseProducer, capturedProduct, capturedException);
                }
            }
        }

        // Production synchronization is separate from the
        // store synchronization.

        /**
         * Obtain the result from the producer.
         *
         * Either, answer the prior result, or invoke the producer and return the new result.
         *
         * The result (or thrown exception) is cached: The producer is invoked at most once.
         *
         * The reference to the producer is cleared following its first invocation.
         *
         * This operation is thread safe.
         *
         * @return The result from the producer.
         */
        @Override
        public T produce() throws Exception {
            String productTime = "prior";
            if ( baseProducer != null ) {
                synchronized (this) {
                    if ( baseProducer != null ) {
                        productTime = "new";
                        try {
                            capturedProduct = baseProducer.produce();
                        } catch ( Exception baseException ) {
                            capturedException = baseException;
                        }
                        baseProducer = null;
                    }
                }
            }

            // Check the exception first: This allows for a null product.

            if (tc.isDebugEnabled()) {
                Object product;
                String productType;
                if ( capturedException != null ) {
                    product = capturedException;
                    productType = "exception";
                } else {
                    product = capturedProduct;
                    productType = "product";
                }
                Tr.debug(tc, "Product [ " + tag + " ] [ " + product + " ] (" + productTime + " " + productType + ")");
                System.out.println("Product [ " + tag + " ] [ " + product + " ] (" + productTime + " " + productType + ")");
            }

            if ( capturedException != null ) {
                throw capturedException;
            } else {
                return capturedProduct;
            }
        }
    }
}
//@formatter:on
