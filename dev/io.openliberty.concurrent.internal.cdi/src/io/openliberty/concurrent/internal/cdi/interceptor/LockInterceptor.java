/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.concurrent.internal.cdi.interceptor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Priority;
import jakarta.enterprise.concurrent.ManagedExecutors;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

/**
 * Interceptor for the Lock annotation.
 */
// @Lock should annotate this interceptor, but won't compile against Java 17.
// Instead, ConcurrencyExtension.beforeBeanDiscovery adds it dynamically.
@Interceptor
@Priority(Interceptor.Priority.PLATFORM_BEFORE + 100)
public class LockInterceptor implements Serializable {
    private static final long serialVersionUID = 757018268808860939L;

    private static final TraceComponent tc = Tr.register(LockInterceptor.class);

    /**
     * jakarta.enterprise.concurrent.Lock if Jakarta Concurrency 3.2+
     * and otherwise null.
     */
    public static final Class<? extends Annotation> ANNO_CLASS;

    /**
     * Methods to access fields of jakarta.enterprise.concurrent.Lock
     * if Jakarta Concurrency 3.2+ and otherwise null.
     */
    private static final Method ACCESS_TIMEOUT, TYPE, UNIT;

    /**
     * jakarta.enterprise.concurrent.Lock.Literal.INSTANCE
     * if Jakarta Concurrency 3.2+ and otherwise null.
     */
    public static final Annotation LOCK_ANNO;

    static {
        String specVersion = ManagedExecutors.class.getPackage() //
                        .getSpecificationVersion();
        if (specVersion == null || "3.1".equals(specVersion)) {
            ANNO_CLASS = null;
            ACCESS_TIMEOUT = null;
            TYPE = null;
            UNIT = null;
            LOCK_ANNO = null;
        } else {
            ClassLoader loader = ManagedExecutors.class.getClassLoader();
            try {
                @SuppressWarnings("unchecked")
                Class<? extends Annotation> c = //
                                (Class<? extends Annotation>) //
                                loader.loadClass("jakarta.enterprise.concurrent.Lock");
                ANNO_CLASS = c;

                ACCESS_TIMEOUT = c.getMethod("accessTimeout");
                TYPE = c.getMethod("type");
                UNIT = c.getMethod("unit");

                LOCK_ANNO = (Annotation) loader //
                                .loadClass("jakarta.enterprise.concurrent.Lock$Literal") //
                                .getField("INSTANCE") //
                                .get(null);
            } catch (ClassNotFoundException | //
                            IllegalAccessException | //
                            NoSuchFieldException | //
                            NoSuchMethodException x) {
                throw (ExceptionInInitializerError) // should never occur
                new ExceptionInInitializerError //
                ("Incorrect definition of @Lock in Concurrency " + specVersion) //
                                .initCause(x);
            }
        }
    }

    /**
     * Mapping of key that enforces instance equality for bean instances
     * to the lock for the bean instance.
     */
    private final ConcurrentHashMap<InstanceEqualityKey, ReentrantReadWriteLock> //
    locks = new ConcurrentHashMap<>();

    @AroundInvoke
    @FFDCIgnore({
                  Error.class, Exception.class, // raised by user's @Lock method
                  InterruptedException.class, // interrupted while awaiting Lock
    })
    @Trivial
    public Object intercept(InvocationContext invocation) throws Exception {
        Method method = invocation.getMethod();
        Annotation anno = invocation.getInterceptorBinding(ANNO_CLASS);

        boolean read = "READ".equals(TYPE.invoke(anno).toString());
        long timeout = (Long) ACCESS_TIMEOUT.invoke(anno);
        TimeUnit unit = (TimeUnit) UNIT.invoke(anno);

        Object beanInstance = invocation.getTarget();

        ReentrantReadWriteLock reentrantLock = locks //
                        .computeIfAbsent(new InstanceEqualityKey(beanInstance),
                                         key -> new ReentrantReadWriteLock());

        Lock lock = read ? reentrantLock.readLock() : reentrantLock.writeLock();

        final boolean trace = TraceComponent.isAnyTracingEnabled();
        if (trace && tc.isEntryEnabled())
            Tr.entry(this, tc,
                     "invoke " + method.getName(),
                     "on " + beanInstance,
                     (read ? "READ " : "WRITE ") + (timeout == 0 ? "IMMEDIATE" : //
                                     timeout == -1 ? "UNLIMITED" : //
                                                     timeout + " " + unit),
                     lock,
                     invocation);

        boolean acquired = false;
        try {
            // reject unsupported escalation from READ to WRITE
            // (which would otherwise time out or deadlock self)
            if (!read && reentrantLock.getReadHoldCount() > 0)
                throw new IllegalStateException("Cannot upgrade from READ to WRITE lock"); // TODO NLS

            if (timeout > 0) {
                acquired = lock.tryLock(timeout, unit);
            } else if (timeout == 0) { // IMMEDIATE
                acquired = lock.tryLock();
            } else if (timeout == -1) { // UNLIMITED
                lock.lockInterruptibly();
                acquired = true;
            } else { // negative accessTimeout value
                throw new CompletionException(new UnsupportedOperationException//
                ("accessTimeout=" + timeout)); // TODO NLS
            }

            if (!acquired)
                throw new CompletionException(new TimeoutException()); // TODO NLS

            if (trace && tc.isDebugEnabled())
                Tr.debug(this, tc,
                         (read ? "READ" : "WRITE") + " allowed for " + beanInstance);

            Object result = invocation.proceed();

            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), result);
            return result;
        } catch (InterruptedException x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), x);
            Thread.currentThread().interrupt(); // restore interrupted status
            for (Class<?> c : invocation.getMethod().getExceptionTypes())
                if (c.isInstance(x))
                    throw x;
            String message = acquired //
                            ? x.toString() //
                            : "Interrupted while waiting for lock"; // TODO NLS
            throw new IllegalStateException(message, x);
        } catch (Exception x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), x);
            throw x;
        } catch (Error x) {
            if (trace && tc.isEntryEnabled())
                Tr.exit(this, tc, "invoke " + method.getName(), x);
            throw x;
        } finally {
            if (acquired) {
                int lockCount = 0;
                if (trace && tc.isDebugEnabled())
                    lockCount = reentrantLock.getReadLockCount() +
                                reentrantLock.getWriteHoldCount() -
                                1; // -1 for subsequent unlock
                lock.unlock();
                if (trace && tc.isDebugEnabled())
                    Tr.debug(this, tc,
                             (read ? "READ" : "WRITE") + " completed, with " +
                                       lockCount + " Lock methods still accessing, " +
                                       reentrantLock.getQueueLength() +
                                       " Lock methods waiting");
            }
        }
    }

    /**
     * Receives notification that a bean instance with intercepted methods is
     * being destroyed. Removes references to the bean instance from the map
     * of locks.
     *
     * @param invocation
     * @throws Exception if an error occurs
     */
    @PreDestroy
    @Trivial
    public void preDestroyBean(InvocationContext invocation) throws Exception {
        Object beanInstance = invocation.getTarget();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(this, tc, "preDestroyBean", invocation, beanInstance);
        locks.remove(new InstanceEqualityKey(beanInstance));
        invocation.proceed();
    }

    /**
     * Hash map key that enforces instance equality.
     */
    @Trivial
    private static class InstanceEqualityKey {
        private final Object instance;

        private InstanceEqualityKey(Object instance) {
            this.instance = instance;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof InstanceEqualityKey key &&
                   instance == key.instance;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(instance);
        }

        @Override
        public String toString() {
            return instance.toString();
        }
    }
}
