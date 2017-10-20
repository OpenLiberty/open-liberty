/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.staticvalue;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Callable;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A StaticValue object is intended to be used for static field values
 * which hold state for a specific group of threads when multiplexing
 * is enabled.  When multiplexing is not enabled accessing the value
 * with {@link #get()} should be a simple read on a final field which
 * requires no additional locks to read.
 * <p>
 * If a static field is final then you can use the {@link #createStaticValue(Callable)}
 * with a callable which will assign the final value value for the
 * StaticValue.  If a static field is not final then you should
 * use the {@link #createStaticValue(Callable)} method but the
 * callable should return null for the initial value. If the
 * static field needs updated with a new value then the method
 * {@link #mutateStaticValue(StaticValue, Callable)}
 * is called with the callable to initialize the value.
 * 
 *
 * @param <T> the type of value
 */
public abstract class StaticValue<T> {
    /**
     * Indicates if values are multiplexed according to thread group.
     * Note that to enable this a resource is searched for called
     * <q>com.ibm.ws.kernel.boot.multiplex</q>.  This is to ensure
     * the multiplex flag is set appropriately as soon as possible.
     */
    private static final boolean multiplex = StaticValue.class.getResource("/com.ibm.ws.kernel.boot.multiplex") != null;

    /**
     * Creates a new StaticValue and uses the initializer callable to obtain the
     * initial value when {@link #get()} is called.  Note that when not multiplexing
     * the initializer is called immediately.
     * @param initializer the callable to obtain the initial value.
     * @return the new static value
     */
    @FFDCIgnore(Exception.class)
    public static <T> StaticValue<T> createStaticValue(Callable<T> initializer) {
        if (multiplex) {
            return new Multiplexed<T>(initializer);
        }
        try {
            return new FinalSingleton<T>(initializer == null ? null : initializer.call());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Mutates a static value.  Note that if not multiplexig then this method returns a
     * new StaticValue object with the value as obtained by the initializer.
     * @param staticValue the mutated static value using the specified initializer
     * @param initializer the initializer to use to get the new value for the static value
     * @return the static value.  This may be a new static value or the one passed in depending on
     * if the static value can be mutated.
     */
    public static <T> StaticValue<T> mutateStaticValue(StaticValue<T> staticValue, Callable<T> initializer) {
        if (multiplex) {
            // Multiplexing case; must check for existing StaticValue
            if (staticValue == null) {
                // no existing value; create new one with no constructor initializer
                staticValue = StaticValue.createStaticValue(null);
            }
            // initialize this thread only with the initializer
            staticValue.initialize(getThreadGroup(), initializer);
            return staticValue;
        }
        // Final singleton case; just create a new StaticValue
        return StaticValue.createStaticValue(initializer);
    }

    static class Multiplexed<T> extends StaticValue<T> {
        static class GroupHolder<T> {
            final T t;
            final Callable<T> initializer;

            GroupHolder(T t, Callable<T> initializer) {
                this.t = t;
                this.initializer = initializer;
            }
        }

        private final Map<ThreadGroup, GroupHolder<T>> threadGroups = new WeakHashMap<>();
        private final Callable<T> constructorInitializer;
        private GroupHolder<T> singleton;

        Multiplexed(Callable<T> initializer) {
            this.constructorInitializer = initializer;
        }

        @Override
        T initialize(ThreadGroup g, Callable<T> initializer) {
            if (g == null) {
                return getOrInitSingleton(initializer);
            }
            return getOrInitGroup(g, initializer);
        }

        @Override
        public T get() {
            ThreadGroup g = getThreadGroup();
            GroupHolder<T> result;
            boolean initialize = false;
            synchronized (this) {
                result = g == null ? singleton : threadGroups.get(g);
                if (result == null) {
                    if (constructorInitializer != null) {
                        initialize = true;
                    } else {
                        return null;
                    }
                }
            }
            return initialize ? initialize(g, constructorInitializer) : result.t;
        }

        private T getOrInitSingleton(Callable<T> initializer) {
            // Note that we simply override the singleton each time to get the latest
            try {
                synchronized (this) {
                    if (singleton != null && singleton.initializer == initializer) {
                        return singleton.t;
                    }
                }
                T val = initializer == null ? null : initializer.call();
                synchronized (this) {
                    // check to make sure this initializer was not used by another thread
                    if (singleton != null && singleton.initializer == initializer) {
                        // another thread won with the same initializer
                        return singleton.t;
                    }
                    singleton = new GroupHolder<>(val, initializer);
                    return singleton.t;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private T getOrInitGroup(ThreadGroup g, Callable<T> initializer) {
            synchronized (this) {
                GroupHolder<T> result = threadGroups.get(g);
                if (result != null && result.initializer == initializer) {
                    return result.t;
                }
            }

            try {
                T val = initializer == null ? null : initializer.call();
                synchronized (this) {
                    // check to make sure this initializer was not used by another thread
                    GroupHolder<T> result = threadGroups.get(g);
                    if (result != null && result.initializer == initializer) {
                        // another thread won with the same initializer
                        return result.t;
                    }
                    // note the last initializer called wins
                    result = new GroupHolder<>(val, initializer);
                    threadGroups.put(g, result);
                    // note that we overide singleton each time to keep it the latest
                    singleton = result;
                    return result.t;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class FinalSingleton<T> extends StaticValue<T> {
        private final T singleton;

        FinalSingleton(T initial) {
            this.singleton = initial;
        }

        @Override
        public T get() {
            return singleton;
        }
    }

    static ThreadGroup getThreadGroup() {
        ThreadGroup g = Thread.currentThread().getThreadGroup();
        while (g != null && (g.getName() == null || !g.getName().startsWith("osgi-boot-"))) {
            g = g.getParent();
        }
        return g;
    }

    public abstract T get();

    T initialize(ThreadGroup g, Callable<T> initializer) {
        return null;
    }
}
