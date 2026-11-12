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
package com.ibm.ws.classloading.internal;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Dedicated Declarative Services component that listens for runtime feature update
 * events and recycles affected applications when dynamic features are added or removed.
 *
 * <p>Separated from {@link ClassLoadingServiceImpl} so that feature-update handling lives in
 * its own component. The runtime update and threading packages are provided by the kernel and
 * are always present, so this component is active in every server. The application recycle
 * coordinator (package {@code com.ibm.wsspi.application.lifecycle}, which may be absent) is
 * deliberately not referenced here; recycling is delegated to
 * {@link ClassLoadingServiceImpl#recycleApplications(Set)}.</p>
 */
@Component(service = { RuntimeUpdateListener.class },
           immediate = true,
           configurationPolicy = ConfigurationPolicy.IGNORE,
           property = "service.vendor=IBM")
public class ClassLoadingUpdateRecycleListener implements RuntimeUpdateListener {
    private static final TraceComponent tc = Tr.register(ClassLoadingUpdateRecycleListener.class);

    @Reference
    private ClassLoadingServiceImpl classLoadingService;

    /**
     * State captured while a FEATURE_BUNDLES_RESOLVED notification is outstanding.
     * Immutable reference, concurrent contents; readers get a consistent view with one read.
     */
    private static final class Window {
        /** ACLs whose gateway went UNRESOLVED; used only to drive aclStore.remove(). */
        final Set<AppClassLoader> acls = ConcurrentHashMap.<AppClassLoader> newKeySet();

        /** App names to recycle, collected from GatewayClassLoader.getOwnerAppNames(). */
        final Set<String> appNames = ConcurrentHashMap.<String> newKeySet();
    }

    /** The open capture window for the current UPDATE, or null. */
    private final AtomicReference<Window> window = new AtomicReference<Window>();

    /**
     * Captures the AppClassLoader whose gateway bundle went UNRESOLVED during a
     * ProvisioningMode.UPDATE operation. Fires only while a capture window is open.
     */
    private final SynchronousBundleListener gatewayUnresolvedListener = new SynchronousBundleListener() {
        @Override
        public void bundleChanged(BundleEvent event) {
            if (event.getType() != BundleEvent.UNRESOLVED) {
                return;
            }
            Window w = window.get();
            if (w == null) {
                return; // window not open
            }
            Bundle bundle = event.getBundle();
            Map<Bundle, Set<GatewayClassLoader>> classloaders = classLoadingService.getClassloaders();
            if (!classloaders.containsKey(bundle)
                || !"true".equalsIgnoreCase(bundle.getHeaders("").get(GatewayBundleFactory.GATEWAY_BUNDLE_MARKER))) {
                return; // not a gateway bundle
            }
            synchronized (classloaders) {
                Set<GatewayClassLoader> gcls = classloaders.get(bundle);
                if (gcls != null) {
                    for (GatewayClassLoader gcl : gcls) {
                        w.appNames.addAll(gcl.getOwnerAppNames());
                    }
                }
                Set<AppClassLoader> appClassLoaders = classLoadingService.getAppClassLoaders();
                synchronized (appClassLoaders) {
                    for (AppClassLoader acl : appClassLoaders) {
                        if (bundle.equals(acl.getBundle())) {
                            w.acls.add(acl);
                        }
                    }
                }
            }
        }
    };

    @Activate
    protected void activate() {
        BundleContext bundleContext = classLoadingService.getBundleContext();
        if (bundleContext != null) {
            Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (systemBundle != null) {
                BundleContext systemContext = systemBundle.getBundleContext();
                if (systemContext != null) {
                    systemContext.addBundleListener(gatewayUnresolvedListener);
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        BundleContext bundleContext = classLoadingService.getBundleContext();
        if (bundleContext != null) {
            Bundle systemBundle = bundleContext.getBundle(Constants.SYSTEM_BUNDLE_LOCATION);
            if (systemBundle != null) {
                BundleContext systemContext = systemBundle.getBundleContext();
                if (systemContext != null) {
                    systemContext.removeBundleListener(gatewayUnresolvedListener);
                }
            }
        }
        window.set(null);
    }

    @Override
    public void notificationCreated(RuntimeUpdateManager updateManager, RuntimeUpdateNotification notification) {
        if (!RuntimeUpdateNotification.FEATURE_BUNDLES_RESOLVED.equals(notification.getName())) {
            return; // only interested in FEATURE_BUNDLES_RESOLVED
        }

        // Confirm this is a ProvisioningMode.UPDATE, not a REFRESH.
        // FEATURE_UPDATES_COMPLETED is only created for UPDATE operations.
        RuntimeUpdateNotification fuc = updateManager.getNotification(RuntimeUpdateNotification.FEATURE_UPDATES_COMPLETED);
        if (fuc == null || fuc.isDone()) {
            return; // not an UPDATE operation, or already completed — nothing to do
        }

        // Open the capture window for this notification.
        final Window w = new Window();
        window.set(w);

        // Register the recycle listener; it fires synchronously on the FeatureManager thread
        // when featureBundlesResolved.setResult(true) is called at the end of provisioning.
        // Each listener acts on its own window object and only closes the shared slot if it
        // is still that window, so an overlapping notification's window is never cleared or
        // consumed by this one.
        notification.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                runRecycleListener(w);
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                // Close the window without recycling on failure path
                window.compareAndSet(w, null);
            }
        });
    }

    private void runRecycleListener(Window w) {
        try {
            // Step 1: close the window, only if it is still ours. If a newer window replaced
            // it, our captured data is still valid and is processed below.
            window.compareAndSet(w, null);

            if (w.acls.isEmpty() && w.appNames.isEmpty()) {
                return; // nothing to do
            }

            if (FrameworkState.isStopping()) {
                return; // server is going down; skip recycling
            }

            // Step 2: evict captured ACLs from aclStore.
            for (AppClassLoader acl : w.acls) {
                if (!classLoadingService.evictFromAclStore(acl) && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Loader not removed from aclStore (already evicted or replaced): " + acl.getKey());
                }
            }

            // Step 3: recycle applications.
            classLoadingService.recycleApplications(w.appNames);
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.classloading.internal.ClassLoadingUpdateRecycleListener", "runRecycleListener", this);
        }
    }
}
