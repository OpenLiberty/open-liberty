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
import com.ibm.websphere.ras.annotation.Trivial;

//@formatter:off
/**
 * Generic cache of supplier results.
 *
 * Main APIs:
 *
 * <ul>
 * <li>{@link #capture(String, BaseCaptureSupplier<T>)</li>
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
 * Callers must reliable release suppliers to avoid memory leaks.
 */
public class CaptureCache<T> {
    private static final TraceComponent tc =
        Tr.register(CaptureCache.class,
                    "app.manager",
                    "com.ibm.ws.app.manager.module.internal.resources.Messages");

    // This format is used by the FAT tests.  Code in the tests which locates
    // and parses the cache trace will need to be updated if this debugging format
    // is updated.  See:
    // componenttest.application.manager.test.SharedLibTestUtils.ContainerAction.ContainerAction(String)

    @Trivial
    protected static void debug(String tag, String methodName, String text) {
        String debugText = "[" + tag + "]" + "." + methodName + ": " + text;
        Tr.debug(tc, debugText);
    }

    @Trivial
    protected static void warning(String tag, String methodName, String text) {
        String warningText = "[" + tag + "]" + "." + methodName + ": " + text;
        Tr.warning(tc, warningText);
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

    @Trivial
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

    private static Function <String, KeyData> keySource =
        (String missingKey) -> new KeyData( new String(missingKey) );

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
    private KeyData acquire(String key) {
        KeyData keyData = keys.computeIfAbsent(key, keySource);
        keyData.increment();
        return keyData;
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

        // The count must be retrieved within the cache lock:
        // Other calls to acquire or release. can happen after
        // releasing the lock.

        String uniqueKey;
        int newCount;
        CaptureSupplier<T> supplier;

        boolean isDebugEnabled = tc.isDebugEnabled();

        synchronized( cacheLock ) {
            KeyData keyData = keys.get(key);
            if ( keyData == null ) {
                uniqueKey = null;
                newCount = -1;
                supplier = null;
            } else {
                uniqueKey = keyData.getKey();
                newCount = keyData.decrement();
                // Guard against corruption of the key data.
                // The count should never go below zero.  But if it
                // does, still remove the key from storage.
                if ( newCount <= 0 ) {
                    keys.remove(key);
                    supplier = storage.remove(uniqueKey);
                } else {
                    if ( isDebugEnabled ) {
                        supplier = storage.get(uniqueKey);
                    } else {
                        supplier = null;
                    }
                }
            }
        }

        // 'release' entries must be logged in this format.
        // The FAT "com.ibm.ws.app.manager_fat/fat/src/"
        // "componenttest/application/manager/test/SharedLibTest.java"
        // scans for and parses these log entries.

        if ( isDebugEnabled || (newCount < 0) ) {
            String baseText = "[ " + key + " ] [ " + newCount + " ]: [ " + supplier + " ]";
            if ( isDebugEnabled ) {
                debug( getTag(), "release", baseText);
            }
            if ( newCount < 0 ) {
                // This should happen only if there is an update to key data which
                // breaks internal access rules.
                warning( getTag(), "release", "Unexpected: " + baseText);
            }
        }
    }

    // Consideration for the future: An additional context specific value might be added.
    // See comments in DeployedAppInfoBase in regards to the use of the library PID,
    // on or around line 370.

    /**
     * Supplier storage.  Keys are the internal, unique, string values.
     * Supplier storage uses an {@link IdentityHashMap}.
     *
     * Two levels of data storage are provided:
     *
     * A capture cache stores suppliers.
     *
     * Each supplier stores the value that it supplies.
     *
     * The capture retains suppliers using a reference count mechanism.
     *
     * Suppliers store valued based on logic specific to the supplier.  In the
     * simplest cases, a supplier might have a pre-computed value.  More typically,
     * a supplier obtains a target value on the first request for that value.
     *
     * The supplier may include logic which causes the refresh of the supplied
     * value.  That is done by the implementation provided by DeployedAppInfoBase,
     * which records the size and the time-stamp of the file which was used to
     * create a supplied container.  Subsequent requests for the container check
     * the current file values against the values stamp which were present when
     * the container was created.  If the size or time-stamp changed, the container
     * is replaced with a new, up to date container.
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
     * Capture invocation of supplier.
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
     * @return A capturing supplier.
     */
    public CaptureSupplier<T> capture(String key, BaseCaptureSupplier<T> baseSupplier) {
        Function<String, CaptureSupplier<T>> supplierSource =
            (String uniqueKey) -> new CaptureSupplier<T>(uniqueKey, baseSupplier);

        KeyData keyData;
        CaptureSupplier<T> captureSupplier;

        boolean isDebugEnabled = tc.isDebugEnabled();
        int count = 0;

        synchronized( cacheLock ) {
            keyData = acquire(key);

            // The count must be retrieved within the cache lock:
            // Other calls to acquire or release can happen after
            // releasing the lock.
            if ( isDebugEnabled ) {
                count = keyData.count;
            } else {
                count = 0;
            }

            captureSupplier = storage.computeIfAbsent( keyData.getKey(), supplierSource );
        }

        // 'capture' entries must be logged in this format.
        // The FAT "com.ibm.ws.app.manager_fat/fat/src/"
        // "componenttest/application/manager/test/SharedLibTest.java"
        // scans for and parses these log entries.

        if ( isDebugEnabled ) {
            debug( getTag(), "capture",
                   "[ " + key + " ] [ " + count + " ] [ " + captureSupplier + " ]" );
        }

        return captureSupplier;
    }

    //

    @FunctionalInterface
    public static interface BaseCaptureSupplier<T> {
        T get(T priorCapture);
    }

    public static class CaptureSupplier<T> {
        public static final String INNER_CLASS_NAME = CaptureSupplier.class.getSimpleName();

        /**
         * Create a new capture supplier which wraps a base supplier.
         *
         * @param tag A tag used to identify the supplier.
         * @param baseSupplier The supplier which is to be captured.
         */
        public CaptureSupplier(String tag, BaseCaptureSupplier<T> baseSupplier) {
            this.tag = tag;
            this.baseSupplier = baseSupplier;
            this.captured = null;
        }

        //

        /** A simple descriptive tag used for debugging. */
        private final String tag;

        @Trivial
        public String getTag() {
            return tag;
        }

        //

        private final BaseCaptureSupplier<T> baseSupplier;
        private T captured;

        /**
         * Type used to answer snapshots of a capturing supplier.
         *
         * The state of the supplier is retrieved without triggering
         * the supply operation.
         *
         * @param <T> The type which is being supplied.
         */
        public static class SupplierSnapShot<T> {
            public final BaseCaptureSupplier<T> baseSupplier;
            public final T captured;

            protected SupplierSnapShot(BaseCaptureSupplier<T> baseSupplier, T captured) {
                this.baseSupplier = baseSupplier;
                this.captured = captured;
            }
        }

        /**
         * Obtain a snapshot of the current state of this capturing Supplier:
         *
         * @return A snapshot of the current state of this capturing Supplier.
         */
        public SupplierSnapShot<T> snapshot() {
            synchronized( this ) {
                return new SupplierSnapShot<T>(baseSupplier, captured);
            }
        }

        // Production synchronization is separate from the
        // storage synchronization.

        /**
         * Obtain the result from the Supplier.
         *
         * @return The result from the supplier.
         */
        public T get() {
            T priorProduct;
            synchronized ( this ) {
                captured = baseSupplier.get( priorProduct = captured );
            }
            if ( tc.isDebugEnabled() ) {
                if ( captured == priorProduct ) {
                    debug("get", getTag(), "[ " + priorProduct + " ]");
                } else {
                    debug("get", getTag(), "[ " + priorProduct + " ] [ " + captured + " ]");
                }
            }
            return captured;
        }
    }
}
//@formatter:on
