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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

//@formatter:off
/**
 * Generic cache of failable supplier results.
 *
 * A failable supplier ({@link CaptureCache.FailableSupplier}
 * is the same as a {@link java.util.function.Supplier} except that
 * {@link CaptureCache.FailableSupplier#get()} may throw an exception.
 *
 * Main APIs:
 *
 * <ul>
 * <li>{@link #capture(String, FailableSupplier<T>)</li>
 * <li>{@link #release(String)}</li>
 * </ul>
 *
 * Each invocation of {@link #capture} stores the supplier and
 * increases the key use count.  Duplicate capture requests do
 * not replace the stored supplier, but do increase the use count.
 *
 * Each invocation of {@link #release(String)} reduces the key use
 * count. If the use count is reduced to zero, the associated supplier
 * is removed. (Release requests which match no supplier are ignored.)
 *
 * The invocation of {@link #capture} does not invoke the supplier.
 * The returned supplier is expected to independently invoked by the
 * caller.
 *
 * The state of any particular capture supplier is completely independent
 * of the state of the capture cache itself.  Synchronization of of the
 * capture cache itself is independent of synchronization within the
 * capture supplier.  This is important because supplier invocations are
 * expected to be expensive, and cache operations should not be impacted
 * by supplier overhead.
 *
 * A capture cache stores both a successful (non-exception) and
 * failed (exception) results.  Unless a supplier is released, multiple
 * invocations of the supplier returned by a capture cache will obtain
 * the same result, including null results, non-exception results, and
 * exception results.
 *
 * Callers must reliable release suppliers to avoid memory leaks.
 */
public class CaptureCache<T> {
    // Trace with the application manager.
    // TODO: Move DeferredCache to a more common location.
    private static final TraceComponent tc =
        Tr.register(CaptureCache.class,
                    "app.manager",
                    "com.ibm.ws.app.manager.module.internal.resources.Messages");

    protected static void debug(String tag, String methodName, String text) {
        String debugText = "[" + tag + "]" + "." + methodName + ": " + text;
        Tr.debug(tc, debugText);
    }

    //

    /**
     * Simple data structure for cache keys.  The structure holds a
     * unique, internal, key value, and holds the use count of that key.
     *
     * Synchronization of key data count updates is performed at the same
     * level as synchronization of the cache.  Count updates are always
     * associated with updates of the cache state.
     */
    protected static class KeyData {
        public KeyData(String key) {
            if ( key == null ) {
                throw new IllegalArgumentException("Disallowed null key");
            }
            this.key = key;
            this.count = 0;
        }

        private final String key;

        public String getKey() {
            return key;
        }

        private int count;

        public int getCount() {
            return count;
        }

        protected int increment() {
            return ++count;
        }

        protected int decrement() {
            return --count;
        }
    }

    //

    public CaptureCache(String tag) {
        this.tag = tag;

        this.cacheLock = new Object();
        this.keys = new HashMap<>();
        this.storage = new IdentityHashMap<>();
    }

    //

    /** A simple descriptive tag used for debugging. */
    private final String tag;

    public String getTag() {
        return tag;
    }

    //

    /**
     * A single lock is used for key and supplier storage updates:
     * Key data and supplier storage have linked state and are
     * satisfied by a single lock.
     */
    private final Object cacheLock;

    //

    /** Table of key data: Keys are the external supplied key values. */
    private final Map<String, KeyData> keys;

    protected KeyData getKey(String key) {
        synchronized ( cacheLock ) {
            return keys.get(key);
        }
    }

    /**
     * Acquire a key.
     *
     * If necessary, create a key data structure, with a unique,
     * internal copy of the key string and with a count of zero.
     *
     * Increment the key count.
     *
     * Answer the unique key from the key data structure.
     *
     * @param key A key which is to be acquired.
     *
     * @return A unique, internal, copy of the key.
     */
    protected String acquire(String key) {
        KeyData keyData =
            keys.computeIfAbsent(key,
                                 (String missingKey) -> new KeyData( new String(missingKey) ) );

        String uniqueKey = keyData.getKey();
        int newCount = keyData.increment();

        if ( tc.isDebugEnabled() ) {
            debug( getTag(), "acquire", "[ " + key + " ] [ " + newCount + " ]" );
        }
        return uniqueKey;
    }

    /**
     * Release a key.
     *
     * This is a public operation, with three possible outcomes:
     *
     * Do nothing if the key is not currently in use by the cache.
     *
     * Decrement the key count, if the key is in use by the cache.
     *
     * Remove the key data and any associated supplier if the key
     * count is decreased to zero.
     *
     * @param key The key which is to be released.
     */
    public void release(String key) {
        String uniqueKey;
        int newCount;
        CaptureSupplier<T> supplier;

        synchronized( cacheLock ) {
            KeyData keyData = keys.get(key);
            if ( keyData == null ) {
                uniqueKey = null;
                newCount = 0;
                supplier = null;
            } else {
                uniqueKey = keyData.getKey();
                newCount = keyData.decrement();
                if ( newCount == 0 ) {
                    keys.remove(key);
                    supplier = storage.remove(uniqueKey);
                } else {
                    supplier = storage.get(uniqueKey);
                }
            }
        }

        if ( tc.isDebugEnabled() ) {
            debug( getTag(),
                   "release",
                   "[ " + key + " ] [ " + newCount + " ]: [ " + supplier + " ]" );
        }
    }

    // Store a caching Supplier.  That moves production outside of management of
    // storage.  The production call is expected to require a large amount of time.
    //
    // TODO: An additional context specific value might be added.  See the TODO in
    //       DeployedAppInfoBase in regards to the use of the library PID.

    /**
     * Supplier storage.  Keys are the internal, unique, string values.
     * Supplier storage uses an {@link IdentityHashMap}.
     */
    private final Map<String, CaptureSupplier<T>> storage;

    /**
     * Clear all stored data.  That includes both key data and suppliers.
     */
    public void clear() {
        synchronized( cacheLock ) {
            keys.clear();
            storage.clear();
        }
    }

    /**
     * Tell how many suppliers are currently stored.
     *
     * Key use is not taken into account.  Alternatively,
     * a total of key use counts could be obtained.
     *
     * @return The count of stored suppliers.
     */
    public int size() {
        synchronized( cacheLock ) {
            return storage.size();
        }
    }

    // Introspection APIs ...

    /**
     * Create and return a snapshot of the cache.  This is created
     * as a table of snapshots of the current stored capturing Suppliers.
     *
     * @return A snapshot of the cache.
     */
    public Map<String, CaptureSupplier.SupplierSnapShot<T>> snapshot() {
        synchronized( cacheLock ) {
            Map<String, CaptureSupplier.SupplierSnapShot<T>> snapshot = new HashMap<>( storage.size() );
            BiConsumer<String, CaptureSupplier<T>> snapper =
                (String key, CaptureCache.CaptureSupplier<T> captureSupplier) ->
                    { snapshot.put(key,  captureSupplier.snapshot()); };
            storage.forEach(snapper);
            return snapshot;
        }
    }

    /**
     * Defer invocation of a failable supplier.  Cache the deferred product as
     * a capturing failable supplier.
     *
     * Only the first invocation for a specific key will be placed into storage.
     * subsequent invocations for the same key will obtain the previously placed
     * capturing supplier, which uses the supplier which was provided on the first
     * invocation.
     *
     * As a consequence, differences in the supplier will be lost on second and
     * subsequent invocations.  The expectation is that the suppliers provided
     * for the second and subsequent invocations are the equivalent of the initial
     * supplier.
     *
     * @param key The key associated with the product.
     * @param baseSupplier A supplier to associate with the key.
     *
     * @return A capturing failable supplier.
     */
    public CaptureSupplier<T> capture(String key, FailableSupplier<T> baseSupplier) {
        Function<String, CaptureSupplier<T>> supplierSupplier =
            (String useKey) -> new CaptureSupplier<T>(useKey, baseSupplier);

        synchronized( cacheLock ) {
            // 'acquire' does two or three things:
            //
            // If necessary, a data structure is created and stored.
            // The key is updated to an internal, unique, string instance.
            // The count associated with the key is incremented.

            return storage.computeIfAbsent( acquire(key), supplierSupplier );
        }
    }

    //

    /**
     * Type for failable Suppliers.  See {@link java.util.function.Supplier}.
     *
     * Modified to enable {@link java.util.function.Supplier#get} to throw
     * an exception.
     *
     * @param <T> The type which is supplied.
     */
    @FunctionalInterface
    public static interface FailableSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Concrete capturing failable Supplier.
     *
     * Wraps a failable Supplier and caches the product.
     *
     * @param <T> The type of the product.
     */
    public static class CaptureSupplier<T> implements FailableSupplier<T> {
        public static final String INNER_CLASS_NAME = CaptureSupplier.class.getSimpleName();

        /**
         * Create a new capture supplier which wraps a base supplier.
         *
         * The capture supplier will "capture" a null result if the base
         * supplier is null.
         *
         * @param tag A tag used to identify the supplier.
         * @param baseSupplier The supplier which is to be captured.
         */
        public CaptureSupplier(String tag, FailableSupplier<T> baseSupplier) {
            this.tag = tag;

            this.baseSupplier = baseSupplier;
            this.capturedProduct = null;
            this.capturedException = null;
        }

        //

        /** A simple descriptive tag used for debugging. */
        private final String tag;

        public String getTag() {
            return tag;
        }

        //

        private volatile FailableSupplier<T> baseSupplier;
        private T capturedProduct;
        private Exception capturedException;

        /**
         * Type used to answer snapshots of a capturing supplier.
         *
         * The state of the supplier is retrieved without triggering
         * the supply operation.
         *
         * @param <T> The type which is being supplied.
         */
        public static class SupplierSnapShot<T> {
            public final FailableSupplier<T> baseSupplier;
            public final T capturedProduct;
            public final Exception capturedException;

            /**
             * Record the state of a capture supplier.
             *
             * @param baseSupplier The base supplier.  Null if the
             *     supplier has been invoked.
             * @param capturedProduct The product.  Null either if
             *     the supplier has not been invoked, or if an exception
             *     occurred, or if a null value was obtained.
             * @param capturedException
             *     Null either if the supplier has not been invoked, or
             *     if the supplier was invoked successfully.
             */
            protected SupplierSnapShot(FailableSupplier<T> baseSupplier,
                                       T capturedProduct, Exception capturedException) {
                this.baseSupplier = baseSupplier;
                this.capturedProduct = capturedProduct;
                this.capturedException = capturedException;
            }
        }

        /**
         * Obtain a snapshot of the current state of this capturing Supplier:
         *
         * If production has not occurred, answer the base Supplier and null product and exception.
         *
         * If production has occurred, answer a null base Supplier and the captured product and
         * exception.
         *
         * This operation is thread safe.
         *
         * @return A snapshot of the current state of this capturing Supplier.
         */
        public SupplierSnapShot<T> snapshot() {
            // The base supplier becomes null when the supplier is invoked,
            // at which point the capture values are fixed.
            if ( baseSupplier == null ) {
                return new SupplierSnapShot<T>(baseSupplier, capturedProduct, capturedException);
            } else {
                synchronized( this ) {
                    return new SupplierSnapShot<T>(baseSupplier, capturedProduct, capturedException);
                }
            }
        }

        // Production synchronization is separate from the
        // storage synchronization.

        /**
         * Obtain the result from the Supplier.
         *
         * Either, answer the prior result, or invoke the supplier and return the result.
         *
         * The result (or thrown exception) is cached: The supplier is invoked at most once.
         *
         * The supplier is cleared following its first invocation.
         *
         * @return The result from the supplier.
         */
        @Override
        public T get() throws Exception {
            // The base supplier is set to null after it is invoked, at which point
            // the capture values are fixed.

            String productTime = "prior";

            if ( baseSupplier != null ) {
                // Synchronize on the capturing supplier, not on the capture cache!
                // The supplier invocation is expected to occasionally be time consuming.
                // The usual operations of the capture cache should be independent of
                // supplier invocations.

                synchronized ( this ) {
                    if ( baseSupplier != null ) {
                        productTime = "new";
                        try {
                            capturedProduct = baseSupplier.get();
                        } catch ( Exception baseException ) {
                            capturedException = baseException;
                        }

                        // Necessary: References held by the base Supplier must
                        // be cleared.  Otherwise, input data references would
                        // be held long after they were needed.
                        baseSupplier = null;
                    }
                }
            }

            // Check the exception first: A null product can occur
            // because the supplier threw an exception, or can occur
            // as the actual supplier result.

            if ( tc.isDebugEnabled() ) {
                Object product;
                String productType;
                if ( capturedException != null ) {
                    product = capturedException;
                    productType = "exception";
                } else {
                    product = capturedProduct;
                    productType = "product";
                }
                debug("get",
                      getTag(),
                      "[ " + product + " ] (" + productTime + " " + productType + ")");
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
