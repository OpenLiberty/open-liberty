/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.util;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.JavaInfo;

/**
 * Helper to access thread context variables. Callers should ensure that they
 * do not allow unprivileged code to have access to an implementation of this
 * class. Callers should use {@link #getPrivilegedAction} to obtain an
 * instance of this class and then store it in a static variable:
 *
 * <pre>
 * private static final ThreadContextAccessor threadContextAccessor = AccessController.doPrivileged(ThreadContextAccessor.getPrivilegedAction());
 * </pre>
 *
 * <p>If the only privileged operation that a caller needs to perform within a
 * code path is a single get or set of a context class loader, then the caller
 * should use the "ForUnprivileged" methods. Otherwise, if {@link #isPrivileged} returns false, the caller should create their own
 * <code>PrivilegedAction</code> and use the non-"ForUnprivileged" methods as
 * needed within a single <code>AccessController.doPrivileged</code>.
 */
public abstract class ThreadContextAccessor {
    private static final TraceComponent tc = Tr.register(ThreadContextAccessor.class, "Runtime", null);

    /**
     * If this flag is set, the thread pool Worker will print the stack
     * trace when somebody calls setContextClassLoader(ClassLoader) on a
     * thread pool thread. When this is enabled, we must use the
     * ThreadContextAccessorImpl (ignoring the performance gains of the
     * ReflectionThreadContextAccessorImpl) to ensure that we print the
     * stack for callers of this class.
     * <br/>
     * Default visibility so that ThreadPool can check it.
     */
    //D658409
    final static boolean PRINT_STACK_ON_SET_CTX_CLASSLOADER = Boolean.getBoolean("com.ibm.ws.util.threadpool.PrintStackOnSetContextClassLoader");

    private static final PrivilegedAction<ThreadContextAccessor> PRIVILEGED_ACTION = new PrivilegedAction<ThreadContextAccessor>() {
        @Override
        public ThreadContextAccessor run() {
            return getThreadContextAccessor();
        }
    };

    private static class Unchanged {
        // Named class for debugging.
    }

    /**
     * The object returned by {@link #pushContextClassLoader} or {@link #repushContextClassLoader} when the current context class loader
     * is the same as the new context class loader. This value must be an
     * object that will never be returned from
     * <code>Thread.getContextClassLoader</code>, so it must be non-null.
     */
    public static final Object UNCHANGED = new Unchanged();

    /**
     * Checks whether the implementation is privileged. If an implementation
     * is privileged, then calls to other methods do not need to be wrapped in
     * a call to <code>AccessController.doPrivileged</code>.
     *
     * @return <code>true</code> if the implementation is privileged
     */
    public abstract boolean isPrivileged();

    /**
     * Gets the thread context class loader. This method is equivalent to
     * <code>Thread.getContextClassLoader</code> except that it might be
     * implemented without security checks. The calling stack must have
     * <code>RuntimePermission("getClassLoader")</code> permission unless {@link #isPrivileged} returns true.
     *
     * @param thread the thread to check
     * @return the context class loader
     * @see #getContextClassLoaderForUnprivileged
     */
    public abstract ClassLoader getContextClassLoader(Thread thread);

    /**
     * Gets the thread context class loader. This method is equivalent to
     * calling <code>Thread.getContextClassLoader</code> with
     * <code>RuntimePermission("getClassLoader")</code> permission.
     *
     * @param thread the thread to check
     * @return the context class loader
     */
    public abstract ClassLoader getContextClassLoaderForUnprivileged(Thread thread);

    /**
     * Sets the thread context class loader. This method is equivalent to
     * <code>Thread.setContextClassLoader</code> except that it might be
     * implemented without security checks. The calling stack must have
     * <code>RuntimePermission("setContextClassLoader")</code> permission unless {@link #isPrivileged} returns true.
     *
     * @param thread the thread to modify
     * @param loader the new context class loader
     * @see #setContextClassLoaderForUnprivileged
     */
    public abstract void setContextClassLoader(Thread thread, ClassLoader loader);

    /**
     * Sets the thread context class loader. This method is equivalent to
     * calling <code>Thread.setContextClassLoader</code> with
     * <code>RuntimePermission("setContextClassLoader")</code> permission.
     *
     * @param thread the thread to modify
     * @param loader the new context class loader
     */
    public abstract void setContextClassLoaderForUnprivileged(Thread thread, ClassLoader loader);

    /**
     * Sets the context class loader of the current thread, and returns either {@link #UNCHANGED} or the original class loader. The calling stack must
     * have <code>RuntimePermission("getClassLoader")</code> and
     * <code>RuntimePermission("setContextClassLoader")</code> permission unless {@link #isPrivileged} returns true.
     *
     * @param loader the new context class loader
     * @return the original context class loader, or {@link #UNCHANGED}
     * @see #pushContextClassLoaderForUnprivileged
     */
    public abstract Object pushContextClassLoader(ClassLoader loader);

    /**
     * Sets the context class loader of the current thread, and returns either {@link #UNCHANGED} or the original context class loader. This method is
     * equivalent to calling {@link #pushContextClassLoader} with
     * <code>RuntimePermission("setContextClassLoader")</code> permission. The
     * suggested pattern of use is:
     *
     * <pre>
     * Object origCL = threadContextAccessor.pushContextClassLoaderForUnprivileged(cl);
     * try {
     * ...
     * } finally {
     * threadContextAccessor.popContextClassLoaderForUnprivileged(origCL);
     * }
     * </pre>
     *
     * @param loader the new context class loader
     * @return the original context class loader, or {@link #UNCHANGED}
     */
    public abstract Object pushContextClassLoaderForUnprivileged(ClassLoader loader);

    /**
     * Updates the context class loader of the current thread between calls to {@link #pushContextClassLoader} and {@link #popContextClassLoader}. If
     * the original class loader is {@link #UNCHANGED}, then this is equivalent
     * to {@link #pushContextClassLoader}). Otherwise, this is equivalent to {@link #setContextClassLoader}, and the passed class loader is returned.
     * The suggested pattern of use is:
     *
     * <pre>
     * Object origCL = ThreadContextAccessor.UNCHANGED;
     * try {
     * for (SomeContext ctx : contexts) {
     * ClassLoader cl = ctx.getClassLoader();
     * origCL = svThreadContextAccessor.repushContextClassLoader(origCL, cl);
     * ...
     * }
     * } finally {
     * svThreadContextAccessor.popContextClassLoader(origCL);
     * }
     * </pre>
     *
     * @param origLoader the result of {@link #pushContextClassLoader} or {@link #repushContextClassLoader}
     * @param loader the new context class loader
     * @return the original context class loader, or {@link #UNCHANGED}
     */
    public Object repushContextClassLoader(Object origLoader, ClassLoader loader) {
        if (origLoader == UNCHANGED) {
            return pushContextClassLoader(loader);
        }

        setContextClassLoader(Thread.currentThread(), loader);
        return origLoader;
    }

    /**
     * Updates the context class loader of the current thread between calls to {@link #pushContextClassLoader} and {@link #popContextClassLoader}. If
     * the original class loader is {@link #UNCHANGED}, then this is equivalent
     * to {@link #pushContextClassLoader}). Otherwise, this is equivalent to {@link #setContextClassLoader}, and the passed class loader is returned.
     * The suggested pattern of use is:
     *
     * <pre>
     * Object origCL = ThreadContextAccessor.UNCHANGED;
     * try {
     * for (SomeContext ctx : contexts) {
     * ClassLoader cl = ctx.getClassLoader();
     * origCL = svThreadContextAccessor.repushContextClassLoaderForUnprivileged(origCL, cl);
     * ...
     * }
     * } finally {
     * svThreadContextAccessor.popContextClassLoaderForUnprivileged(origCL);
     * }
     * </pre>
     *
     * @param origLoader the result of {@link #pushContextClassLoader} or {@link #repushContextClassLoader}
     * @param loader the new context class loader
     * @return the original context class loader, or {@link #UNCHANGED}
     */
    public Object repushContextClassLoaderForUnprivileged(Object origLoader, ClassLoader loader) {
        if (origLoader == UNCHANGED) {
            return pushContextClassLoaderForUnprivileged(loader);
        }

        setContextClassLoaderForUnprivileged(Thread.currentThread(), loader);
        return origLoader;
    }

    /**
     * Resets the context class loader of the current thread after a call to {@link #pushContextClassLoader} or {@link #repushContextClassLoader}.
     * This method does nothing if the specified class loader is {@link #UNCHANGED}. Otherwise, this method is equivalent to calling {@link #setContextClassLoader}.
     *
     * @param origLoader the result of {@link #pushContextClassLoader} or {@link #repushContextClassLoader}
     */
    public void popContextClassLoader(Object origLoader) {
        if (origLoader != UNCHANGED) {
            setContextClassLoader(Thread.currentThread(), (ClassLoader) origLoader);
        }
    }

    /**
     * Resets the context class loader of the current thread after a call to {@link #pushContextClassLoaderForUnprivileged} or {@link #repushContextClassLoaderForUnprivileged}.
     * This method does
     * nothing if the specified class loader is {@link #UNCHANGED}. Otherwise,
     * this method is equivalent to calling {@link #setContextClassLoaderForUnprivileged}
     *
     * @param origLoader the result of {@link #pushContextClassLoaderForUnprivileged} or {@link #repushContextClassLoaderForUnprivileged}
     */
    public void popContextClassLoaderForUnprivileged(Object origLoader) {
        if (origLoader != UNCHANGED) {
            setContextClassLoaderForUnprivileged(Thread.currentThread(), (ClassLoader) origLoader);
        }
    }

    private final static ThreadContextAccessor threadContextAccessor;

    static {
        ThreadContextAccessor myThreadContextAccessor;
        if (PRINT_STACK_ON_SET_CTX_CLASSLOADER || JavaInfo.majorVersion() >= 9) {
            // If we are printing the stack trace when thread pool threads'
            // setContextClassLoader is invoked, then we want to skip the
            // performance optimization in order to ensure that we print
            // the stack -- otherwise we would miss some calls when the
            // web or EJB container uses the performance optimization to
            // directly set the contextClassLoader.
            myThreadContextAccessor = new ThreadContextAccessorImpl();
        } else {
            Field field;
            try {
                field = Thread.class.getDeclaredField("contextClassLoader");
                field.setAccessible(true);
                myThreadContextAccessor = new ReflectionThreadContextAccessorImpl(field);
            } catch (Exception ex) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "exception", ex);
                }
                myThreadContextAccessor = new ThreadContextAccessorImpl();
            }
        }

        threadContextAccessor = myThreadContextAccessor;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "using " + threadContextAccessor);
        }
    }

    /**
     * Gets an implementation of <code>ThreadContextAccessor</code>. Callers
     * should ensure that they do not allow unprivileged code to have access to
     * the object returned by this method.
     *
     * <p>If there is a security manager, then the security manager's
     * <code>checkPermission</code> method is called with a
     * <code>RuntimePermission("getClassLoader")</code> and
     * <code>RuntimePermission("setContextClassLoader")</code> to see if it's
     * ok to get and set the context ClassLoader.
     *
     * @return a <code>ThreadContextAccessor</code> implementation
     * @throws SecurityException if a security manager exists and its
     *             <code>checkPermission</code> method doesn't allow getting or setting the
     *             context ClassLoader.
     */
    public static ThreadContextAccessor getThreadContextAccessor() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // Check for the same permissions that calling
            // getContextClassLoader and setContextClassLoader would require.
            sm.checkPermission(new RuntimePermission("getClassLoader"));
            sm.checkPermission(new RuntimePermission("setContextClassLoader"));
        }

        return threadContextAccessor;
    }

    /**
     * Gets a <code>PrivilegedAction</code> that calls {@link #getThreadContextAccessor}.
     *
     * @return a privileged action to obtain a thread context accessor
     */
    public static PrivilegedAction<ThreadContextAccessor> getPrivilegedAction() {
        return PRIVILEGED_ACTION;
    }

    // An implementation of ThreadContextAccessor that falls back to calling
    // methods on Thread.
    static class ThreadContextAccessorImpl extends ThreadContextAccessor {
        @Override
        public boolean isPrivileged() {
            return System.getSecurityManager() == null;
        }

        @Override
        public ClassLoader getContextClassLoader(Thread thread) {
            return thread.getContextClassLoader();
        }

        @Override
        public ClassLoader getContextClassLoaderForUnprivileged(final Thread thread) {
            if (isPrivileged()) {
                return getContextClassLoader(thread);
            }

            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return thread.getContextClassLoader();
                }
            });
        }

        @Override
        public void setContextClassLoader(Thread thread, ClassLoader loader) {
            thread.setContextClassLoader(loader);
        }

        @Override
        public void setContextClassLoaderForUnprivileged(final Thread thread, final ClassLoader loader) {
            if (isPrivileged()) {
                thread.setContextClassLoader(loader);
            } else {
                AccessController.doPrivileged(new PrivilegedAction<Void>() {
                    @Override
                    public Void run() {
                        thread.setContextClassLoader(loader);
                        return null;
                    }
                });
            }
        }

        @Override
        public Object pushContextClassLoader(ClassLoader loader) {
            Thread thread = Thread.currentThread();
            ClassLoader current = thread.getContextClassLoader();
            if (current == loader) {
                return UNCHANGED;
            }

            thread.setContextClassLoader(loader);
            return current;
        }

        @Override
        public Object pushContextClassLoaderForUnprivileged(final ClassLoader loader) {
            if (isPrivileged()) {
                return pushContextClassLoader(loader);
            }

            return AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    return pushContextClassLoader(loader);
                }
            });
        }
    }

    // An implementation of ThreadContextAccessor that uses reflection to
    // directly set and get the value of a Field.
    static class ReflectionThreadContextAccessorImpl extends ThreadContextAccessor {
        private final Field field;

        public ReflectionThreadContextAccessorImpl(Field field) {
            this.field = field;
        }

        @Override
        public boolean isPrivileged() {
            return true;
        }

        @Override
        public ClassLoader getContextClassLoader(Thread thread) {
            return getContextClassLoaderForUnprivileged(thread);
        }

        @Override
        public ClassLoader getContextClassLoaderForUnprivileged(Thread thread) {
            if (System.getSecurityManager() == null) {
                // 511027 - If security is disabled, then a direct call should be
                // faster than reflection.
                return thread.getContextClassLoader();
            }

            try {
                return (ClassLoader) field.get(thread);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @Override
        public void setContextClassLoader(Thread thread, ClassLoader loader) {
            setContextClassLoaderForUnprivileged(thread, loader);
        }

        @Override
        public void setContextClassLoaderForUnprivileged(Thread thread, ClassLoader loader) {
            if (System.getSecurityManager() == null) {
                // 511027 - If security is disabled, then a direct call should be
                // faster than reflection.
                thread.setContextClassLoader(loader);
            } else {
                try {
                    field.set(thread, loader);
                } catch (IllegalAccessException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }

        @Override
        public Object pushContextClassLoader(ClassLoader loader) {
            return pushContextClassLoaderForUnprivileged(loader);
        }

        @Override
        public Object pushContextClassLoaderForUnprivileged(ClassLoader loader) {
            Thread thread = Thread.currentThread();

            if (System.getSecurityManager() == null) {
                // If security is disabled, then direct calls should be faster than
                // reflection.
                ClassLoader current = thread.getContextClassLoader();
                if (current == loader) {
                    return UNCHANGED;
                }

                thread.setContextClassLoader(loader);
                return current;
            }

            try {
                Object current = field.get(thread);
                if (current == loader) {
                    return UNCHANGED;
                }

                field.set(thread, loader);
                return current;
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
