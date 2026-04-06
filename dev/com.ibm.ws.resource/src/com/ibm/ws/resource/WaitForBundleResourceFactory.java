/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.resource;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;

import com.ibm.wsspi.resource.ResourceInfo;

/**
 * A convenience class that integrates a ResourceFactory and SynchronousBundleListener when
 * the resource factory requires that a specific bundle be available at runtime.
 */
public class WaitForBundleResourceFactory implements SynchronousBundleListener, ResourceFactory {

    private final BundleContext bundleContext;
    private final String targetBundleLocation;
    private final int requiredStateMask;

    private final Function<Bundle, ResourceFactory> constructor;
    private final Supplier<? extends RuntimeException> notReadyException;

    private final AtomicReference<ResourceFactory> delegate = new AtomicReference<>();
    private final AtomicReference<Exception> createException = new AtomicReference<>();

    public WaitForBundleResourceFactory(BundleContext bundleContext, String targetBundleLocation,
                                        Function<Bundle, ResourceFactory> constructor,
                                        Supplier<? extends RuntimeException> notReadyException) {
        this(bundleContext, targetBundleLocation, constructor, notReadyException, Bundle.STARTING | Bundle.ACTIVE);
    }

    public WaitForBundleResourceFactory(BundleContext bundleContext,
                                        String targetBundleLocation,
                                        Function<Bundle, ResourceFactory> constructor,
                                        Supplier<? extends RuntimeException> notReadyException,
                                        int requiredStateMask) {
        this.bundleContext = bundleContext;
        this.targetBundleLocation = targetBundleLocation;
        this.constructor = constructor;
        this.notReadyException = notReadyException;
        this.requiredStateMask = requiredStateMask;
    }

    public final void listenForBundle() {
        bundleContext.addBundleListener(this);

        // If it already exists, we might have missed the event.
        Bundle b = bundleContext.getBundle(targetBundleLocation);
        if (b != null) {
            tryCreateDelegate(b);
        }
    }

    @Override
    public final void bundleChanged(BundleEvent event) {
        tryCreateDelegate(event.getBundle());
    }

    private void tryCreateDelegate(Bundle b) {
        delegate.getAndUpdate(existing -> {
            if (existing != null) {
                return existing;
            }
            if (!matchesTargetBundle(b)) {
                return null;
            }
            if ((b.getState() & requiredStateMask) == 0) {
                return null;
            }

            try {
                ResourceFactory rf = constructor.apply(b);
                return rf;
            } catch (Exception ex) {
                createException.compareAndSet(null, ex);
                return null;
            } finally {
                // Stop listening once we've either created successfully or failed creation.
                // If you want to keep listening after failures, move this into the success path only.
                cleanupListener();
            }
        });
    }

    private boolean matchesTargetBundle(Bundle b) {
        return targetBundleLocation.equals(b.getLocation());
    }

    private void cleanupListener() {
        try {
            bundleContext.removeBundleListener(this);
        } catch (IllegalStateException ignored) {
            // BundleContext might already be stopping.
        }
    }

    private final ResourceFactory getDelegate() throws Exception {
        ResourceFactory rf = delegate.get();
        if (rf != null) {
            return rf;
        }
        Exception ex = createException.get();
        if (ex != null) {
            throw ex;
        }
        throw notReadyException.get();
    }

    @Override
    public final Object createResource(ResourceInfo info) throws Exception {
        return getDelegate().createResource(info);
    }

    @Override
    public final Object createResource(ResourceRefInfo info) throws Exception {
        return getDelegate().createResource(info);
    }

    @Override
    public void destroy() throws Exception {
        // The resource factory is being destroyed which indicates the application is being
        // uninstalled or modified, stop any bundle listener still waiting.
        cleanupListener();

        ResourceFactory rf = delegate.get();
        if (rf != null) {
            rf.destroy();
        }
    }

    @Override
    public void modify(Map<String, Object> props) throws Exception {
        getDelegate().modify(props);
    }
}
