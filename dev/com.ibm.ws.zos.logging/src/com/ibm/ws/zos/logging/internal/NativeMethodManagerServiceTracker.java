/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.logging.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.ibm.ws.zos.jni.NativeMethodManager;

/**
 * Keeps an eye out for the NativeMethodManager and forwards registrations/
 * deregistrations to ZosLoggingBundleActivator.
 */
public class NativeMethodManagerServiceTracker {

    /**
     * When the NativeMethodManager becomes available it is injected into this guy.
     */
    private final ZosLoggingBundleActivator zosLoggingBundleActivator;

    /**
     * ServiceTracker for NativeMethodManager.
     */
    private volatile ServiceTracker<NativeMethodManager, NativeMethodManager> serviceTracker;

    /**
     * CTOR.
     */
    public NativeMethodManagerServiceTracker(ZosLoggingBundleActivator zosLoggingBundleActivator) {
        this.zosLoggingBundleActivator = zosLoggingBundleActivator;
    }

    /**
     * Open the ServiceTracker.
     *
     * @return this
     */
    public synchronized NativeMethodManagerServiceTracker open(final BundleContext bundleContext) {

        ServiceTrackerCustomizer<NativeMethodManager, NativeMethodManager> stc = new ServiceTrackerCustomizer<NativeMethodManager, NativeMethodManager>() {

            @Override
            public void modifiedService(ServiceReference<NativeMethodManager> reference, NativeMethodManager service) {
            }

            @Override
            public void removedService(ServiceReference<NativeMethodManager> reference, NativeMethodManager service) {
                zosLoggingBundleActivator.unsetNativeMethodManager(service);
            }

            @Override
            public NativeMethodManager addingService(ServiceReference<NativeMethodManager> reference) {
                NativeMethodManager retMe = bundleContext.getService(reference);
                zosLoggingBundleActivator.setNativeMethodManager(retMe);
                return retMe;
            }
        };

        serviceTracker = new ServiceTracker<NativeMethodManager, NativeMethodManager>(bundleContext, NativeMethodManager.class, stc);
        serviceTracker.open();

        return this;
    }

    /**
     * Close the ServiceTracker.
     */
    public synchronized void close() {
        if (serviceTracker != null) {
            serviceTracker.close();
            serviceTracker = null;
        }
    }

}