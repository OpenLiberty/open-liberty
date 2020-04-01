/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Caches looked up values for a period of time.
 * <p>
 * Allows {@code null} to be used as a value, but not as a key
 * <p>
 * Uses a scheduled task to invalidate the cache after the configured period of time.
 *
 * @param K the lookup key type
 * @param V the value type
 */
public class TimedCache<K, V> {

    private static final TraceComponent tc = Tr.register(TimedCache.class);

    private static class CachedNullValue {
    };

    /**
     * Constant to represent {@code null} when stored inside the cache map
     * <p>
     * Needed because ConcurrentHashMap can't hold null values and we need to distinguish between a cached {@code null} and a missing value.
     */
    private static final Object NULL_VALUE = new CachedNullValue();

    private final ScheduledExecutorService executor;
    private final boolean localExecutor;
    private final long delay;
    private final TimeUnit unit;

    private volatile ConcurrentHashMap<K, Object> cache = new ConcurrentHashMap<>();
    private final AtomicBoolean invalidationPending = new AtomicBoolean(false);
    private volatile Future<?> invalidationFuture = null;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * @param executor
     * @param delay
     * @param unit
     */
    public TimedCache(ScheduledExecutorService executor, long delay, TimeUnit unit) {
        if (executor == null) {
            // For unit testing only
            this.executor = Executors.newSingleThreadScheduledExecutor();
            this.localExecutor = true;
        } else {
            this.executor = executor;
            this.localExecutor = false;
        }
        this.delay = delay;
        this.unit = unit;
    }

    /**
     * Get the value associated with {@code key}. If the value is in the cache, it will be returned from the cache. Otherwise the value will be retrieved using
     * {@code lookupFunction} and stored in the cache.
     *
     * @param key            the key to use for the lookup
     * @param lookupFunction a function which returns the value associated with the given {@code key}. Only used if the value is not in the cache.
     * @return the value
     */
    public V get(K key, Function<K, V> lookupFunction) {

        Object cacheValue = cache.get(key);

        if (cacheValue == null) {
            // If the object is not in the cache, find it with the lookup function and store it in the cache
            cacheValue = toCacheValue(lookupFunction.apply(key));
            cache.put(key, cacheValue);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(this, tc, "Item added to timed cache", key, cacheValue);
            }

            requestInvalidation();
        }

        return fromCacheValue(cacheValue);
    }

    /**
     * Prepares a value to be put into the cache by converting {@code null} to {@link #NULL_VALUE}.
     *
     * @param value the value to be cached
     * @return the object to store in the cache map
     */
    @Trivial
    private Object toCacheValue(V value) {
        return value == null ? NULL_VALUE : value;
    }

    /**
     * Extracts the value from the object stored in the cache by converting {@link #NULL_VALUE} to {@code null}
     *
     * @param cacheValue the object from the cache map
     * @return the value stored in the cache
     */
    @SuppressWarnings("unchecked") // Safe because we ensure that all values in the map are of type V except NULL_VALUE
    @Trivial
    private V fromCacheValue(Object cacheValue) {
        return cacheValue == NULL_VALUE ? null : (V) cacheValue;
    }

    @FFDCIgnore(RejectedExecutionException.class)
    private void requestInvalidation() {
        if (closed.get()) {
            return;
        }

        if (invalidationPending.compareAndSet(false, true)) {
            try {
                invalidationFuture = executor.schedule(this::invalidate, delay, unit);

                if (closed.get()) {
                    // If the cache was closed while we were scheduling an invalidation, we should now cancel it
                    invalidationFuture.cancel(false);
                    invalidationPending.compareAndSet(true, false);
                }

            } catch (RejectedExecutionException e) {
                // Didn't actually start task
                invalidationPending.compareAndSet(true, false);

                if (!com.ibm.wsspi.kernel.service.utils.FrameworkState.isStopping()) {
                    // This shouldn't happen unless the server is stopping
                    Tr.error(tc, "failed.to.schedule.cache.invalidation.CWMCG0301E", e);
                    String sourceId = TimedCache.class.getName();
                    FFDCFilter.processException(e, sourceId, "requestInvalidation.rejectedExecution", this);
                }

            } catch (Throwable t) {
                // Probably didn't actually start task
                invalidationPending.compareAndSet(true, false);
                Tr.error(tc, "failed.to.schedule.cache.invalidation.CWMCG0301E", t);
                throw t;
            }
        }

    }

    private void invalidate() {
        invalidationPending.compareAndSet(true, false);
        cache = new ConcurrentHashMap<>();
    }

    public void close() {
        closed.set(true);
        if (invalidationFuture != null) {
            invalidationFuture.cancel(false);
        }
        if (localExecutor) {
            executor.shutdown();
        }
    }

}
