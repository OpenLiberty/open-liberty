/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.jaxrs.monitor;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Stores a cache of {@code (class, method) -> monitor key data}
 * <p>
 * Conceptually this is just a thread-safe {@code WeakHashMap<Pair<Class<?>, Method>, MonitorKey>} but it's more complicated because we need a weak reference to the class and the
 * method, not to the pair object.
 */
@Trivial
public class RestMonitorKeyCache {

    private final ConcurrentHashMap<RestResourceMethodKey, MonitorKey> routes = new ConcurrentHashMap<>();
    private final ReferenceQueue<Class<?>> referenceQueue = new ReferenceQueue<>();

    static {
    	/*
    	 * Eagerly load the inner classes so that they are not loaded while calculating the amount of time a method took.
    	 * The first request coming through the filter() logic will end up being way off due to the loading of the inner classes. 
    	 */
    	MonitorKey.init();
    	RestResourceMethodKey.init();
    	RestMonitorKeyWeakReference.init();
    }

    @Trivial
    static class MonitorKey {
    	static void init() {}

    	final String statsKey;
        final String statsKeyPrefix;
        final String statsMethodName;
        MonitorKey(String statsKey, String statsKeyPrefix, String statsMethodName) {
            this.statsKey = statsKey;
            this.statsKeyPrefix = statsKeyPrefix;
            this.statsMethodName = statsMethodName;
        }

        @Override
        public String toString() {
            return "MonitorKey [statsKey=" + statsKey + ", statsKeyPrefix=" + statsKeyPrefix + ", statsMethodName="
                    + statsMethodName + "]";
        }
    }

    /**
     * Retrieve the cached monitor key for the specified REST Class and Method
     *
     * @param restClass  the class
     * @param restMethod the method
     * @return the monitor key, or {@code null} if no route is in the cache
     */
    public MonitorKey getMonitorKey(Class<?> restClass, Method restMethod) {
        evictGarbageCollectedEntries();
        return routes.get(new RestResourceMethodKey(restClass, restMethod));
    }

    /**
     * Add a new monitor key for the specified REST Class and Method
     *
     * @param restClass
     * @param restMethod
     * @param monitorKey
     */
    public void putMonitorKey(Class<?> restClass, Method restMethod, MonitorKey monitorKey) {
        evictGarbageCollectedEntries();
        routes.put(new RestResourceMethodKey(referenceQueue, restClass, restMethod), monitorKey);
    }

    private void evictGarbageCollectedEntries() {
        RestMonitorKeyWeakReference<?> key;
        while ((key = (RestMonitorKeyWeakReference<?>) referenceQueue.poll()) != null) {
            routes.remove(key.getOwningKey());
        }
    }

    @Trivial
    private static class RestResourceMethodKey {
    	static void init() {}

    	private final RestMonitorKeyWeakReference<Class<?>> restClassRef;
        private final RestMonitorKeyWeakReference<Method> restMethodRef;
        private final int hash;

        RestResourceMethodKey(Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestMonitorKeyWeakReference<>(restClass, this);
            this.restMethodRef = new RestMonitorKeyWeakReference<>(restMethod, this);
            hash = Objects.hash(restClass, restMethod);
        }

        RestResourceMethodKey(ReferenceQueue<Class<?>> referenceQueue, Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestMonitorKeyWeakReference<>(restClass, this, referenceQueue);
            this.restMethodRef = new RestMonitorKeyWeakReference<>(restMethod, this);
            hash = Objects.hash(restClass, restMethod);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RestResourceMethodKey other = (RestResourceMethodKey) obj;
            if (!restClassRef.equals(other.restClassRef)) {
                return false;
            }
            if (!restMethodRef.equals(other.restMethodRef)) {
                return false;
            }
            return true;
        }
    }

    @Trivial
    private static class RestMonitorKeyWeakReference<T> extends WeakReference<T> {
    	static void init() {}

    	private final RestResourceMethodKey owningKey;

        RestMonitorKeyWeakReference(T referent, RestResourceMethodKey owningKey) {
            super(referent);
            this.owningKey = owningKey;
        }

        RestMonitorKeyWeakReference(T referent, RestResourceMethodKey owningKey,
                                  ReferenceQueue<T> referenceQueue) {
            super(referent, referenceQueue);
            this.owningKey = owningKey;
        }

        RestResourceMethodKey getOwningKey() {
            return owningKey;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof RestMonitorKeyWeakReference) {
                return get() == ((RestMonitorKeyWeakReference) obj).get();
            }

            return false;
        }

        @Override
        public String toString() {
            T referent = get();
            return new StringBuilder("RestMonitorKeyWeakReference: ").append(referent).toString();
        }
    }

}
