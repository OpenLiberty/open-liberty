/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.telemetry.internal.rest;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores a cache of {@code (class, method) -> route}
 * <p>
 * Conceptually this is just a thread-safe {@code WeakHashMap<Pair<Class<?>, Method>, String>} but it's more complicated because we need a weak reference to the class and the
 * method, not to the pair object.
 */
public class RestRouteCache {

    private final ConcurrentHashMap<RestRouteKey, String> routes = new ConcurrentHashMap<>();
    private final ReferenceQueue<Class<?>> referenceQueue = new ReferenceQueue<>();

    /**
     * Retrieve the cached route for the specified REST Class and Method
     *
     * @param restClass  the class
     * @param restMethod the method
     * @return the route, or {@code null} if no route is in the cache
     */
    public String getRoute(Class<?> restClass, Method restMethod) {
        evictGarbageCollectedEntries();
        return routes.get(new RestRouteKey(restClass, restMethod));
    }

    /**
     * Add a new route for the specified REST Class and Method
     *
     * @param restClass
     * @param restMethod
     * @param route
     */
    public void putRoute(Class<?> restClass, Method restMethod, String route) {
        evictGarbageCollectedEntries();
        routes.put(new RestRouteKey(referenceQueue, restClass, restMethod), route);
    }

    private void evictGarbageCollectedEntries() {
        RestRouteKeyWeakReference<?> key;
        while ((key = (RestRouteKeyWeakReference<?>) referenceQueue.poll()) != null) {
            routes.remove(key.getOwningKey());
        }
    }

    private static class RestRouteKey {
        private final RestRouteKeyWeakReference<Class<?>> restClassRef;
        private final RestRouteKeyWeakReference<Method> restMethodRef;
        private final int hash;

        RestRouteKey(Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestRouteKeyWeakReference<>(restClass, this);
            this.restMethodRef = new RestRouteKeyWeakReference<>(restMethod, this);
            hash = Objects.hash(restClass, restMethod);
        }

        RestRouteKey(ReferenceQueue<Class<?>> referenceQueue, Class<?> restClass, Method restMethod) {
            this.restClassRef = new RestRouteKeyWeakReference<>(restClass, this, referenceQueue);
            this.restMethodRef = new RestRouteKeyWeakReference<>(restMethod, this);
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
            RestRouteKey other = (RestRouteKey) obj;
            if (!restClassRef.equals(other.restClassRef)) {
                return false;
            }
            if (!restMethodRef.equals(other.restMethodRef)) {
                return false;
            }
            return true;
        }
    }

    private static class RestRouteKeyWeakReference<T> extends WeakReference<T> {
        private final RestRouteKey owningKey;

        RestRouteKeyWeakReference(T referent, RestRouteKey owningKey) {
            super(referent);
            this.owningKey = owningKey;
        }

        RestRouteKeyWeakReference(T referent, RestRouteKey owningKey,
                                  ReferenceQueue<T> referenceQueue) {
            super(referent, referenceQueue);
            this.owningKey = owningKey;
        }

        RestRouteKey getOwningKey() {
            return owningKey;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof RestRouteKeyWeakReference) {
                return get() == ((RestRouteKeyWeakReference) obj).get();
            }

            return false;
        }

        @Override
        public String toString() {
            T referent = get();
            return new StringBuilder("RestRouteKeyWeakReference: ").append(referent).toString();
        }
    }

}
