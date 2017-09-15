/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.launch.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import com.ibm.ws.kernel.security.thread.J2CIdentityService;
import com.ibm.ws.kernel.security.thread.ThreadIdentityManager;
import com.ibm.wsspi.kernel.security.thread.ThreadIdentityService;

/**
 * Injects OSGI configuration into the ThreadIdentityManager. The ThreadIdentityManager
 * itself sits on the non-OSGI side of the logging component, and thus cannot access
 * the OSGI framework.
 * 
 * Basically this guy sets up a ServiceTracker for the ThreadIdentityService.
 * When a ThreadIdentityService is added to the OSGI framework, this guy sets a
 * ref to it in ThreadIdentityManager. When a ThreadIdentityService is removed
 * from the OSGI framework, this guy un-sets the ref in ThreadIdentityManager.
 */
public class ThreadIdentityManagerConfigurator {

    ThreadIdentityServiceTracker threadIdentityServiceTracker = null;
    J2CIdentityServiceTracker j2cIdentityServiceTracker = null;

    /**
     * @param context The BundleContext.
     */
    public ThreadIdentityManagerConfigurator(BundleContext context) {
        threadIdentityServiceTracker = new ThreadIdentityServiceTracker(context);
        j2cIdentityServiceTracker = new J2CIdentityServiceTracker(context);
    }

    /**
     * In addition to opening the trackers, check if a ThreadIdentityService and J2CIdentityService services have
     * already been registered with OSGI and set them into the ThreadIdentityManager.
     */
    public void open() {
        threadIdentityServiceTracker.open();
        j2cIdentityServiceTracker.open();
    }

    private class ThreadIdentityServiceTracker extends ServiceTracker<ThreadIdentityService, ThreadIdentityService> {

        private BundleContext bundleContext = null;

        public ThreadIdentityServiceTracker(BundleContext context) {
            super(context, ThreadIdentityService.class.getName(), null);
            bundleContext = context;
        }

        /**
         * {@inheritDoc}
         * 
         * In addition to opening the tracker, check if ThreadIdentityService services have
         * already been registered with OSGI and set them into the ThreadIdentityManager.
         */
        @Override
        public void open() {
            super.open();
            ThreadIdentityManager.addThreadIdentityService(getService());
        }

        /**
         * {@inheritDoc}
         * 
         * In addition to closing the tracker, remove all the ThreadIdentityService instances
         * from the ThreadIdentityManager.
         */
        @Override
        public void close() {
            super.close();
            ThreadIdentityManager.removeAllThreadIdentityServices();
        }

        /**
         * Set the ThreadIdentityService into the ThreadIdentityManager.
         * 
         * @param reference
         * @return The ThreadIdentityService
         */
        @Override
        public ThreadIdentityService addingService(ServiceReference<ThreadIdentityService> reference) {
            ThreadIdentityService tis = bundleContext.getService(reference);
            ThreadIdentityManager.addThreadIdentityService(tis);
            return tis;
        }

        /**
         * Un-set the ThreadIdentityService from the ThreadIdentityManager.
         * 
         * @param reference
         * @param service
         */
        @Override
        public void removedService(ServiceReference<ThreadIdentityService> reference, ThreadIdentityService service) {
            ThreadIdentityManager.removeThreadIdentityService(service);
            bundleContext.ungetService(reference);
        }
    }

    private class J2CIdentityServiceTracker extends ServiceTracker<J2CIdentityService, J2CIdentityService> {

        private BundleContext bundleContext = null;

        public J2CIdentityServiceTracker(BundleContext context) {
            super(context, J2CIdentityService.class.getName(), null);
            bundleContext = context;
        }

        /**
         * {@inheritDoc}
         * 
         * In addition to opening the tracker, check if J2CIdentityService services have
         * already been registered with OSGI and set them into the ThreadIdentityManager.
         */
        @Override
        public void open() {
            super.open();
            ThreadIdentityManager.addJ2CIdentityService(getService());
        }

        /**
         * {@inheritDoc}
         * 
         * In addition to closing the tracker, remove all the J2CIdentityService instances
         * from the ThreadIdentityManager.
         */
        @Override
        public void close() {
            super.close();
            ThreadIdentityManager.removeAllJ2CIdentityServices();
        }

        /**
         * Set the J2CIdentityService into the ThreadIdentityManager.
         * 
         * @param reference
         * @return The ThreadIdentityService
         */
        @Override
        public J2CIdentityService addingService(ServiceReference<J2CIdentityService> reference) {
            J2CIdentityService j2cIdentityService = bundleContext.getService(reference);
            ThreadIdentityManager.addJ2CIdentityService(j2cIdentityService);
            return j2cIdentityService;
        }

        /**
         * Un-set the J2CIdentityService from the ThreadIdentityManager.
         * 
         * @param reference
         * @param service
         */
        @Override
        public void removedService(ServiceReference<J2CIdentityService> reference, J2CIdentityService j2cIdentityService) {
            ThreadIdentityManager.removeJ2CIdentityService(j2cIdentityService);
            bundleContext.ungetService(reference);
        }
    }

}
