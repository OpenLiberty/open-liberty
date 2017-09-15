/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jca.internal;

import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * Provide access to the class loader ID for the classloader for the RAR file.
 * RAR bundle may want to retrieve the class loader ID in order to directly
 * access files in the RAR file.
 */
public abstract class DeferredService {
    private static final TraceComponent tc = Tr.register(DeferredService.class);

    /**
     * Resource adapter domain for class loader identity
     */
    public final static String RESOURCE_ADAPTER_DOMAIN = "Resource Adapter";

    private final AtomicReference<Object> serviceReg = new AtomicReference<Object>();

    /**
     * Register information available after class loader is created
     * for use by RAR bundle
     */
    public void registerDeferredService(BundleContext bundleContext, Class<?> providedService, Dictionary dict) {
        Object obj = serviceReg.get();
        if (obj instanceof ServiceRegistration<?>) {
            // already registered - nothing to do here
            return;
        }
        if (obj instanceof CountDownLatch) {
            // another thread is in the process of (de)registering - wait for it to finish
            try {
                ((CountDownLatch) obj).await();

                if (serviceReg.get() instanceof ServiceRegistration<?>) {
                    // Another thread has successfully registered to return out (so we don't go
                    // into recursive loop).
                    return;
                }
            } catch (InterruptedException swallowed) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Count down interrrupted", swallowed);
                }
            }
        } else {
            // This is probably the first thread to register.
            // Claim the right to register by setting a latch for other threads to wait on.
            CountDownLatch latch = new CountDownLatch(1);
            if (serviceReg.compareAndSet(null, latch)) {
                // This thread won the right to register the service
                try {
                    serviceReg.set(bundleContext.registerService(providedService.getName(), this, dict));
                    // successfully registered - nothing more to do
                    return;
                } finally {
                    // if the serviceReg was not updated for any reason, we need to set it back to null
                    serviceReg.compareAndSet(latch, null);
                    // in any case we need to allow any blocked threads to proceed
                    latch.countDown();
                }
            }
        }
        // If we get to here we have not successfully registered 
        // nor seen another thread successfully register, so just recurse.
        registerDeferredService(bundleContext, providedService, dict);
    }

    /**
     * Unregister information provided after class loader was created
     */
    public void deregisterDeferredService() {
        Object obj = serviceReg.get();
        if (obj == null) {
            // already deregistered so there is nothing to be done
            return;
        }
        if (obj instanceof CountDownLatch) {
            // If someone else has the latch, then let them do whatever they are doing and we pretend
            // we've already done the deregister.
            return;
        } else if (obj instanceof ServiceRegistration<?>) {
            CountDownLatch latch = new CountDownLatch(1);
            if (serviceReg.compareAndSet(obj, latch)) {
                // This thread won the right to deregister the service
                try {
                    ((ServiceRegistration<?>) obj).unregister();
                    // successfully deregistered - nothing more to do
                    return;
                } finally {
                    // if the serviceReg was not updated for any reason, we need to restore the previous value
                    serviceReg.compareAndSet(latch, obj);
                    // in any case we need to allow any blocked threads to proceed
                    latch.countDown();
                }
            }
        }

    }

}