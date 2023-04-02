/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
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
package io.openliberty.org.jboss.resteasy.common.client;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

class OsgiFacade {
    private static final boolean isSecurityManagerPresent = null != System.getSecurityManager();
    private static final Map<Integer, Tuple<?>> tupleMap = new ConcurrentHashMap<>();
    private static final AtomicInteger counter = new AtomicInteger(0);

    static Optional<OsgiFacade> instance() {
        try {
            Class.forName("org.osgi.framework.Bundle");
            return Optional.of(new OsgiFacade());
        } catch (Throwable t) {
            //expected in non-OSGI, Java SE env
            return Optional.empty();
        }
    }

    <T> Optional<ServiceReference<T>> getServiceRef(Class<T> serviceClass) {
        BundleContext ctx = getBundleContext();
        if (ctx == null) {
            return Optional.empty();
        }
        if (isSecurityManagerPresent) {
            try {
                return Optional.ofNullable(AccessController.doPrivileged((PrivilegedExceptionAction<ServiceReference<T>>) () -> {
                    return ctx.getServiceReference(serviceClass);
                }));
            } catch (PrivilegedActionException e) {
                // Auto FFDC
                return Optional.empty();
            }
        }
        return Optional.ofNullable(ctx.getServiceReference(serviceClass));
    }

    /**
     * 
     * @param <T>
     * @param serviceRef
     * @param serviceClass
     * @throws ClassCastException if anything other than a ServiceReference is passed as serviceRef
     * @return
     */
    @SuppressWarnings("unchecked")
    <T> T getService(Object serviceRef, Class<T> serviceClass) {
        return serviceClass.cast(getService(getBundleContext(), (ServiceReference<T>) serviceRef));
    }

    void ungetService(Object o) {
        if (o instanceof ServiceReference) {
            getBundleContext().ungetService((ServiceReference<?>)o);
        }
    }

    <T> Integer invoke(Class<T> service, Consumer<T> consumer) {
        BundleContext ctx = getBundleContext();
        Tuple<T> tuple = new Tuple<>(ctx, getServiceRefs(service, ctx).orElse(Collections.emptyList()));
        tuple.serviceRefs.stream().map(sr -> getService(tuple.bundleCtx, sr)).forEach(consumer);
        Integer key = counter.incrementAndGet();
        tupleMap.put(key, tuple);
        return key;
    }

    @SuppressWarnings("unchecked")
    <T> void invoke(Integer key, Class<T> clz, Consumer<T> consumer) {
        Tuple<T> tuple = (Tuple<T>) tupleMap.remove(key);
        tuple.serviceRefs.stream().map(sr -> getService(tuple.bundleCtx, sr)).forEach(consumer);
    }

    private BundleContext getBundleContext() {
        if (isSecurityManagerPresent) {
            return AccessController.doPrivileged((PrivilegedAction<BundleContext>) () -> {
                Bundle b = FrameworkUtil.getBundle(getClass());
                return b == null ? null : b.getBundleContext(); 
            });
        }
        Bundle b = FrameworkUtil.getBundle(getClass());
        return b == null ? null : b.getBundleContext();
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<List<ServiceReference<T>>> getServiceRefs(Class<T> serviceClass, BundleContext ctx) {
        if (ctx == null) {
            return Optional.empty();
        }
        try {
            ServiceReference<?>[] serviceRefs;
            if (isSecurityManagerPresent) {
                serviceRefs = AccessController.doPrivileged((PrivilegedExceptionAction<ServiceReference<?>[]>) () -> 
                    ctx.getServiceReferences(serviceClass.getName(), null));
            } else {
                serviceRefs = ctx.getServiceReferences(serviceClass.getName(), null);
            }
            return Optional.ofNullable(serviceRefs == null ? null : Arrays.asList((ServiceReference<T>[]) serviceRefs));
        } catch (PrivilegedActionException pae) {
            throw new RuntimeException(pae.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T getService(BundleContext ctx, ServiceReference<T> ref) {
        if (isSecurityManagerPresent) {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> ctx.getService(ref));
        }
        return ctx.getService(ref);
    }

    class Tuple<T> {
        final BundleContext bundleCtx;
        final List<ServiceReference<T>> serviceRefs;

        Tuple(BundleContext bundleCtx, List<ServiceReference<T>> serviceRefs) {
            this.bundleCtx = bundleCtx;
            this.serviceRefs = serviceRefs;
        }
    }
}
