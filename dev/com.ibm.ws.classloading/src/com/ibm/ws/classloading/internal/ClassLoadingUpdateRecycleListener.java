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

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.runtime.update.RuntimeUpdateListener;
import com.ibm.ws.runtime.update.RuntimeUpdateManager;
import com.ibm.ws.runtime.update.RuntimeUpdateNotification;
import com.ibm.ws.threading.listeners.CompletionListener;
import com.ibm.wsspi.application.lifecycle.ApplicationRecycleCoordinator;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

/**
 * Dedicated Declarative Services component that listens for runtime feature update
 * events and recycles affected applications when dynamic features are added or removed.
 *
 * <p>Separated from {@link ClassLoadingServiceImpl} to allow the core classloading service
 * to resolve and function in minimal runtime environments where the runtime update service
 * and application lifecycle packages are absent.</p>
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
     * Optional reference to the application recycle coordinator; used to recycle applications
     * when a feature bundle is uninstalled. OPTIONAL/DYNAMIC to avoid an OSGi dependency cycle
     * (com.ibm.ws.app.manager.lifecycle must not depend on com.ibm.ws.classloading).
     */
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected volatile ApplicationRecycleCoordinator recycleCoordinator;

    /**
     * Capture window: the FEATURE_BUNDLES_RESOLVED notification for the current UPDATE, or null.
     */
    private volatile RuntimeUpdateNotification captureNotification = null;

    /**
     * ACLs whose gateway went UNRESOLVED during the current capture window.
     * Used only to drive aclStore.remove(); app names come from capturedAppNames.
     */
    private volatile Set<AppClassLoader> capturedAcls = null;

    /**
     * App names to recycle, collected directly from GCL.getOwnerAppNames() during
     * the capture window.
     */
    private volatile Set<String> capturedAppNames = null;

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
            if (captureNotification == null) {
                return; // window not open
            }
            Bundle bundle = event.getBundle();
            Map<Bundle, Set<GatewayClassLoader>> classloaders = classLoadingService.getClassloaders();
            if (!classloaders.containsKey(bundle)
                || !"true".equalsIgnoreCase(bundle.getHeaders("").get(GatewayBundleFactory.GATEWAY_BUNDLE_MARKER))) {
                return; // not a gateway bundle
            }
            Set<AppClassLoader> aclsToEvict = capturedAcls;
            Set<String> appNames = capturedAppNames;
            if (aclsToEvict == null || appNames == null) {
                return; // window closed
            }
            synchronized (classloaders) {
                Set<GatewayClassLoader> gcls = classloaders.get(bundle);
                if (gcls != null) {
                    for (GatewayClassLoader gcl : gcls) {
                        appNames.addAll(gcl.getOwnerAppNames());
                    }
                }
                Set<AppClassLoader> appClassLoaders = classLoadingService.getAppClassLoaders();
                synchronized (appClassLoaders) {
                    for (AppClassLoader acl : appClassLoaders) {
                        if (bundle.equals(acl.getBundle())) {
                            aclsToEvict.add(acl);
                        }
                    }
                }
            }
        }
    };

    @Activate
    protected void activate(ComponentContext cc) {
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
    protected void deactivate(ComponentContext cc) {
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
        captureNotification = null;
        capturedAcls = null;
        capturedAppNames = null;
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

        // Open the capture window bound to this notification object.
        capturedAcls = ConcurrentHashMap.<AppClassLoader> newKeySet();
        capturedAppNames = ConcurrentHashMap.<String> newKeySet();
        captureNotification = notification;

        // Register the recycle listener; it fires synchronously on the FeatureManager thread
        // when featureBundlesResolved.setResult(true) is called at the end of provisioning.
        notification.onCompletion(new CompletionListener<Boolean>() {
            @Override
            public void successfulCompletion(Future<Boolean> future, Boolean result) {
                runRecycleListener();
            }

            @Override
            public void failedCompletion(Future<Boolean> future, Throwable t) {
                // Close the window without recycling on failure path
                captureNotification = null;
                capturedAcls = null;
                capturedAppNames = null;
            }
        });
    }

    private void runRecycleListener() {
        try {
            // Step 1: close window.
            captureNotification = null;

            // Step 2: take the captured ACLs and app names.
            Set<AppClassLoader> aclsToEvict = capturedAcls;
            capturedAcls = null;
            Set<String> appsToRecycle = capturedAppNames;
            capturedAppNames = null;

            if ((aclsToEvict == null || aclsToEvict.isEmpty()) && (appsToRecycle == null || appsToRecycle.isEmpty())) {
                return; // nothing to do
            }

            if (FrameworkState.isStopping()) {
                return; // server is going down; skip recycling
            }

            // Step 3: evict captured ACLs from aclStore.
            if (aclsToEvict != null) {
                for (AppClassLoader acl : aclsToEvict) {
                    if (!classLoadingService.evictFromAclStore(acl) && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Loader not removed from aclStore (already evicted or replaced): " + acl.getKey());
                    }
                }
            }

            // Step 4: recycle applications.
            if (appsToRecycle != null && !appsToRecycle.isEmpty()) {
                ApplicationRecycleCoordinator coord = recycleCoordinator;
                if (coord != null) {
                    coord.recycleApplications(appsToRecycle);
                } else if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "recycleCoordinator is absent; cannot recycle apps after feature update: " + appsToRecycle);
                }
            }
        } catch (Throwable t) {
            FFDCFilter.processException(t, "com.ibm.ws.classloading.internal.ClassLoadingUpdateRecycleListener", "runRecycleListener", this);
        }
    }
}
