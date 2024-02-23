/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Alex Blewitt - initial API and implementation
 *     Alexander Fedorov (ArSysOp) - documentation improvements
 *******************************************************************************/

// Copied from https://github.com/eclipse-equinox/equinox/blob/master/bundles/org.eclipse.equinox.common/src/org/eclipse/core/runtime/ServiceCaller.java

// This is copied because: The upstream package name is the horrid org.eclipse.core.runtime and the Eclipse project has moved that bundle to be compiled to Java 17 byte code.

// Local modifications are the call and callOnce methods that take a Function, and the package name.

package com.ibm.ws.kernel.service.util;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServicePermission;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@code ServiceCaller} provides functional methods for invoking OSGi services
 * in two different ways
 * <ul>
 * <li>Single invocations which happen only once or very rarely. In this case,
 * maintaining a cache of the service is not worth the overhead.</li>
 * <li>Multiple invocations that happen often and rapidly. In this case,
 * maintaining a cache of the service is worth the overhead.</li>
 * </ul>
 * <p>
 * For single invocations of a service the static method
 * {@link ServiceCaller#callOnce(Class, Class, Consumer)} can be used. This
 * method will wrap a call to the consumer of the service with the necessary
 * OSGi service registry calls to ensure the service exists and will do the
 * proper get and release service operations surround the calls to the service.
 * By wrapping a call around the service we can ensure that it is correctly
 * released after use.
 * </p>
 * <p>
 * Single invocation example:
 * </p>
 *
 * <pre>
 * ServiceCaller.callOnce(MyClass.class, ILog.class, (logger) -&gt; logger.info("All systems go!"));
 * </pre>
 *
 * <p>
 * Note that it is generally more efficient to use a long-running service
 * utility, such as {@link ServiceTracker} or declarative services, but there
 * are cases where a single one-shot lookup is preferable, especially if the
 * service is not required after use. Examples might include logging unlikely
 * conditions or processing debug options that are only read once.
 * </p>
 * <p>
 * This allows boilerplate code to be reduced at call sites, which would
 * otherwise have to do something like:
 * </p>
 *
 * <pre>
 * Bundle bundle = FrameworkUtil.getBundle(BadExample.class);
 * BundleContext context = bundle == null ? null : bundle.getBundleContext();
 * ServiceReference&lt;Service&gt; reference = context == null ? null : context.getServiceReference(serviceType);
 * try {
 *     Service service = reference == null ? null : context.getService(reference);
 *     if (service != null)
 *         consumer.accept(service);
 * } finally {
 *     context.ungetService(reference);
 * }
 * </pre>
 *
 * <p>
 * For cases where a service is used much more often a {@code ServiceCaller}
 * instance can be used to cache and track the available service. This may be
 * useful for cases that cannot use declarative services and that want to avoid
 * using something like a {@link ServiceTracker} that does not easily allow for
 * lazy instantiation of the service instance. For example, if logging is used
 * more often then something like the following could be used:
 * </p>
 *
 * <pre>
 * static final ServiceCaller&lt;ILog&gt; log = new ServiceCaller(MyClass.class, ILog.class);
 *
 * static void info(String msg) {
 *     log.call(logger -&gt; logger.info(msg));
 * }
 * </pre>
 *
 * <p>
 * Note that this class is intended for simple service usage patterns only. More
 * advanced cases should use other mechanisms such as the {@link ServiceTracker}
 * or declarative services.
 * </p>
 *
 * @param <S> the service type for this caller
 * @since 3.13
 */
public class ServiceCaller<S> {

    /**
     * Calls an OSGi service by dynamically looking it up and passing it to the
     * given consumer.
     * <p>
     * If not running under OSGi, the caller bundle is not active or the service is
     * not available, return false. If the service is found, call the service and
     * return true.
     * </p>
     * <p>
     * Any runtime exception thrown by the consumer is rethrown by this method. If
     * the consumer throws a checked exception, it can be propagated using a
     * <em>sneakyThrow</em> inside a try/catch block:
     * </p>
     *
     * <pre>
     * callOnce(MyClass.class, Callable.class, (callable) -&gt; {
     *   try {
     *     callable.call();
     *   } catch (Exception e) {
     *     sneakyThrow(e);
     *   }
     * });
     * ...
     * {@literal @}SuppressWarnings("unchecked")
     * static &lt;E extends Throwable&gt; void sneakyThrow(Throwable e) throws E {
     *   throw (E) e;
     * }
     * </pre>
     *
     * @param caller      a class from the bundle that will use service
     * @param serviceType the OSGi service type to look up
     * @param consumer    the consumer of the OSGi service
     * @param <S>         the OSGi service type to look up
     * @return true if the OSGi service was located and called successfully, false
     *         otherwise
     * @throws NullPointerException  if any of the parameters are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public static <S> boolean callOnce(Class<?> caller, Class<S> serviceType, Consumer<S> consumer) {
        return callOnce(caller, serviceType, null, consumer);
    }

    /**
     * As {@link #callOnce(Class, Class, Consumer)} with an additional OSGi filter.
     *
     * @param caller      a class from the bundle that will use service
     * @param serviceType the OSGi service type to look up
     * @param consumer    the consumer of the OSGi service
     * @param filter      an OSGi filter to restrict the services found
     * @param <S>         the OSGi service type to look up
     * @return true if the OSGi service was located and called successfully, false
     *         otherwise
     * @throws NullPointerException  if any of the parameters except filter are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public static <S> boolean callOnce(Class<?> caller, Class<S> serviceType, String filter, Consumer<S> consumer) {
        return new ServiceCaller<>(caller, serviceType, filter).getCurrent().map(r -> {
            try {
                consumer.accept(r.instance);
                return Boolean.TRUE;
            } finally {
                r.unget();
            }
        }).orElse(Boolean.FALSE);
    }

    /**
     * As {@link #callOnce(Class, Class, Consumer)} but replaces the Consumer with a Function that returns a value.
     *
     * @param caller      a class from the bundle that will use service
     * @param serviceType the OSGi service type to look up
     * @param function    a function that consumes the OSGi service and retrns a value
     * @param <S>         the OSGi service type to look up
     * @param <R>         the type returned by calling the method on the service
     * @return an Optional containing the object returned by function, or null if it failed to return a value
     * @throws NullPointerException  if any of the parameters are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public static <S, R> Optional<R> callOnce(Class<?> caller, Class<S> serviceType, Function<S, R> function) {
        ServiceCaller<S> sc = new ServiceCaller<>(caller, serviceType);
        try {
            return sc.call(function);
        } finally {
            sc.unget();
        }
    }

    /**
     * As {@link #callOnce(Class, Class, Consumer)} but with an additional OSGi filter.
     *
     * @param caller      a class from the bundle that will use service
     * @param serviceType the OSGi service type to look up
     * @param function    a function that consumes the OSGi service and retrns a value
     * @param filter      an OSGi filter to restrict the services found
     * @param <S>         the OSGi service type to look up
     * @param <R>         the type returned by calling the method on the service
     * @return an Optional containing the object returned by function, or null if it failed to return a value
     * @throws NullPointerException  if any of the parameters except filter are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public static <S, R> Optional<R> callOnce(Class<?> caller, Class<S> serviceType, String filter, Function<S, R> function) {
        ServiceCaller<S> sc = new ServiceCaller<>(caller, serviceType, filter);
        try {
            return sc.call(function);
        } finally {
            sc.unget();
        }
    }

    private static int getRank(ServiceReference<?> ref) {
        Object rank = ref.getProperty(Constants.SERVICE_RANKING);
        if (rank instanceof Integer) {
            return ((Integer) rank).intValue();
        }
        return 0;
    }

    private class ReferenceAndService implements SynchronousBundleListener, ServiceListener {
        final BundleContext context;
        final ServiceReference<S> ref;
        final S instance;
        final int rank;

        public ReferenceAndService(final BundleContext context, ServiceReference<S> ref, S instance) {
            this.context = context;
            this.ref = ref;
            this.instance = instance;
            this.rank = getRank(ref);
        }

        void unget() {
            untrack();
            try {
                context.ungetService(ref);
            } catch (IllegalStateException e) {
                // ignore; just trying to cleanup but context is not valid now
            }
        }

        @Override
        public void bundleChanged(BundleEvent e) {
            if (bundle.equals(e.getBundle()) && e.getType() == BundleEvent.STOPPING) {
                unget();
            }
        }

        @Override
        public void serviceChanged(ServiceEvent e) {
            if (requiresUnget(e)) {
                unget();
            }
        }

        private boolean requiresUnget(ServiceEvent e) {
            if (e.getServiceReference().equals(ref)) {
                return (e.getType() == ServiceEvent.UNREGISTERING)
                       || (filter != null && e.getType() == ServiceEvent.MODIFIED_ENDMATCH)
                       || (e.getType() == ServiceEvent.MODIFIED && getRank(ref) != rank);
                // if rank changed: untrack to force a new ReferenceAndService with new rank
            }
            return e.getType() == ServiceEvent.MODIFIED && getRank(e.getServiceReference()) > rank;
        }

        // must hold monitor on ServiceCaller.this when calling track
        Optional<ReferenceAndService> track() {
            try {
                ServiceCaller.this.service = this;
                context.addServiceListener(this, "(&" //$NON-NLS-1$
                                                 + "(objectClass=" + serviceType.getName() + ")" // //$NON-NLS-1$ //$NON-NLS-2$
                                                 + (filter == null ? "" : filter) // //$NON-NLS-1$
                                                 + ")"); //$NON-NLS-1$

                if (System.getSecurityManager() != null) {
                    AccessController.doPrivileged(
                                                  (PrivilegedAction<Void>) () -> {
                                                      context.addBundleListener(this);
                                                      return null;
                                                  });
                } else {
                    context.addBundleListener(this);
                }
                if ((ref.getBundle() == null || context.getBundle() == null) && ServiceCaller.this.service == this) {
                    // service should have been untracked but we may have missed the event
                    // before we could added the listeners
                    unget();
                }
                if (getRank(ref) != rank) {
                    // ranking has changed; unget to force reget in case the ranking is not the
                    // highest
                    unget();
                }
            } catch (InvalidSyntaxException e) {
                // really should never happen with our own filter above.
                ServiceCaller.this.service = null;
                throw new IllegalStateException(e);
            } catch (IllegalStateException e) {
                // bundle was stopped before we could get listeners added/removed
                ServiceCaller.this.service = null;
            }
            // Note that we always return this ReferenceAndService
            // even for cases where the instance was unget
            // It is way complicated to try again and
            // even if we did the returned value can become
            // stale right after return.
            return Optional.of(this);
        }

        void untrack() {
            synchronized (ServiceCaller.this) {
                if (ServiceCaller.this.service == this) {
                    ServiceCaller.this.service = null;
                }
                try {
                    context.removeServiceListener(this);
                    if (System.getSecurityManager() != null) {
                        AccessController.doPrivileged(
                                                      (PrivilegedAction<Void>) () -> {
                                                          context.removeBundleListener(this);
                                                          return null;
                                                      });
                    } else {
                        context.removeBundleListener(this);
                    }
                } catch (IllegalStateException e) {
                    // context is invalid;
                    // ignore - the listeners already got cleaned up
                }
            }
        }
    }

    private final Bundle bundle;
    private final Class<S> serviceType;
    private final String filter;
    private final ServicePermission servicePermission;
    private volatile ReferenceAndService service = null;

    /**
     * Creates a {@code ServiceCaller} instance for invoking an OSGi service many
     * times with a consumer function.
     *
     * @param caller      a class from the bundle that will consume the service
     * @param serviceType the OSGi service type to look up
     * @throws NullPointerException  if any of the parameters are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public ServiceCaller(Class<?> caller, Class<S> serviceType) {
        this(caller, serviceType, null);
    }

    /**
     * Creates a {@code ServiceCaller} instance for invoking an OSGi service many
     * times with a consumer function.
     *
     * @param caller      a class from the bundle that will consume the service
     * @param serviceType the OSGi service type to look up
     * @param filter      the service filter used to look up the service. May be
     *                        {@code null}.
     * @throws NullPointerException  if any of the parameters are {@code null}
     * @throws IllegalStateException if the bundle associated with the caller class
     *                                   cannot be determined
     */
    public ServiceCaller(Class<?> caller, Class<S> serviceType, String filter) {
        this.serviceType = Objects.requireNonNull(serviceType);
        this.bundle = Optional.of(Objects.requireNonNull(caller)).map(FrameworkUtil::getBundle).orElseThrow(IllegalStateException::new);
        this.filter = filter;
        this.servicePermission = new ServicePermission(serviceType.getName(), ServicePermission.GET);

        if (filter != null) {
            try {
                FrameworkUtil.createFilter(filter);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private BundleContext getContext() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> bundle.getBundleContext());
        }
        return bundle.getBundleContext();
    }

    /**
     * Calls an OSGi service by dynamically looking it up and passing it to the
     * given consumer. If not running under OSGi, the caller bundle is not active or
     * the service is not available, return false. Any runtime exception thrown by
     * the consumer is rethrown by this method. (For handling checked exceptions,
     * see {@link #callOnce(Class, Class, Consumer)} for a solution.) Subsequent
     * calls to this method will attempt to reuse the previously acquired service
     * instance until one of the following occurs:
     * <ul>
     * <li>The {@link #unget()} method is called.</li>
     * <li>The service is unregistered.</li>
     * <li>The service properties change such that this {@code ServiceCaller} filter
     * no longer matches.
     * <li>The caller bundle is stopped.</li>
     * <li>The service rankings have changed.</li>
     * </ul>
     *
     * After one of these conditions occur subsequent calls to this method will try
     * to acquire the another service instance.
     *
     * @param consumer the consumer of the OSGi service
     * @return true if the OSGi service was located and called successfully, false
     *         otherwise
     */
    public boolean call(Consumer<S> consumer) {
        return trackCurrent().map(r -> {
            consumer.accept(r.instance);
            return Boolean.TRUE;
        }).orElse(Boolean.FALSE);
    }

    /**
     * As {@link #call(call)} but replaces the Consumer with a Function that returns a value.
     *
     * @param <R>      the type returned by calling the method on the service
     * @param function A function that calls a method on the OSGi service and returns its output
     * @return an Optional containing the object returned by function, or null if it failed to return a value
     */
    public <R> Optional<R> call(Function<S, R> function) {
        return Optional.ofNullable(trackCurrent().map(r -> {
            return function.apply(r.instance);
        }).orElse(null));
    }

    /**
     * Return the currently available service.
     *
     * @return the currently available service or empty if the service cannot be
     *         found.
     */
    public Optional<S> current() {
        return trackCurrent().map(r -> r.instance);
    }

    private Optional<ReferenceAndService> trackCurrent() {
        ReferenceAndService current = service;
        if (current != null) {
            return Optional.of(current);
        }
        return getCurrent().flatMap(r -> {
            synchronized (ServiceCaller.this) {
                if (service != null) {
                    // another thread beat us
                    // unget this instance and return existing
                    r.unget();
                    return Optional.of(service);
                }
                return r.track();
            }
        });

    }

    private Optional<ReferenceAndService> getCurrent() {
        BundleContext context = getContext();
        //If we do not have the permission the else block will throw the necessary SecurityException
        if (System.getSecurityManager() != null && bundle.hasPermission(servicePermission)) {
            return AccessController.doPrivileged(
                                                 (PrivilegedAction<Optional<ReferenceAndService>>) () -> {
                                                     return getCurrentInternal(context);
                                                 });
        } else {
            return getCurrentInternal(context);
        }
    }

    private Optional<ReferenceAndService> getCurrentInternal(BundleContext context) {
        return getServiceReference(context).map(r -> {
            S current = context.getService(r);
            return current == null ? null : new ReferenceAndService(context, r, current);
        });
    }

    private Optional<ServiceReference<S>> getServiceReference(BundleContext context) {
        if (context == null) {
            return Optional.empty();
        }

        if (filter == null) {
            return Optional.ofNullable(context.getServiceReference(serviceType));
        }
        try {
            return context.getServiceReferences(serviceType, filter).stream().findFirst();
        } catch (InvalidSyntaxException e) {
            // should not happen; filter was checked at construction
            return Optional.empty();
        }
    }

    /**
     * Releases the cached service object, if it exists. Another invocation of
     * {@link #call(Consumer)} will lazily get the service instance again and cache
     * the new instance if found.
     */
    public void unget() {
        ReferenceAndService current = service;
        if (current != null) {
            current.unget();
        }
    }
}
