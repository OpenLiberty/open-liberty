/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.service.util;

import java.util.ArrayDeque;
import java.util.Hashtable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A helper class for modifying a service registration properties dynamically. This helper
 * may perform the service registration update asynchronously if another thread
 * is already in the middle of updating the service properties.
 */
public class ServiceRegistrationModifier<S> {

    /**
     * A supplier of service properties. Implementations of this
     * interface must be thread safe.
     */
    public static interface ServicePropertySupplier {
        /**
         * Returns the current service properties for a service
         * registration. This method will get called during calls to
         * {@link ServiceRegistrationModifier#update()} and
         * {@link ServiceRegistrationModifier#registerOrUpdate(BundleContext)}.
         *
         * @return the current service properties. A Hashtable is
         *         used to ease the copying of the Dictionary for the service
         *         registration.
         */
        Hashtable<String, Object> getServiceProperties();
    }

    private final ArrayDeque<Boolean> modifyQueue = new ArrayDeque<>();
    private final Lock modifyLock = new ReentrantLock();
    private final Class<S> serviceClass;
    private final ServicePropertySupplier supplier;
    private final S service;

    private BundleContext context;
    private Hashtable<String, Object> serviceProperties;
    private ServiceRegistration<S> registration;
    private boolean unregistered = false;

    /**
     * Constructs a modifier for a service registration.
     *
     * @param serviceClass the class to register the service under.
     * @param supplier     a supplier of the service registration properties
     * @param service      the service object to register
     */
    public ServiceRegistrationModifier(Class<S> serviceClass, ServicePropertySupplier supplier, S service) {
        this.serviceClass = serviceClass;
        this.supplier = supplier;
        this.service = service;
    }

    /**
     * If the service is not already registered this method will register the
     * service. If the service is already registered then this method will
     * update the service properties of the existing registration. The service
     * properties are obtained by calling {@link ServicePropertySupplier#getServiceProperties()}.
     * This method may perform the update to the service properties asynchronously.
     *
     * @param bc the bundle context to use to register the service with. May be {@code null}.
     *               if {@code null} is used and the service is not already registered then this method
     *               exists without updating anything.
     */
    public void registerOrUpdate(BundleContext bc) {
        updateService(false, bc);
    }

    /**
     * If the service is registered then this method will unregister it. If the
     * service is not registered then this method exists without updating anything.
     * This method may unregister the service asynchronously.
     */
    public void unregister() {
        updateService(true, null);
    }

    /**
     * If the service is registered then this method will update the service properties
     * of the existing registration. The service properties are obtained by calling
     * {@link ServicePropertySupplier#getServiceProperties()}.
     * This method may perform the update to the service properties asynchronously.
     */
    public void update() {
        updateService(false, null);
    }

    @FFDCIgnore(IllegalStateException.class)
    private void updateService(boolean unregister, BundleContext bc) {
        modifyLock.lock();
        try {
            if (bc != null) {
                context = bc;
            }
            boolean doWorkNow = modifyQueue.isEmpty();
            modifyQueue.add(unregister);
            if (doWorkNow) {
                do {
                    boolean doUnregister = modifyQueue.peek();
                    boolean isUnregistered = unregistered;

                    BundleContext currentContext = context;
                    ServiceRegistration<S> currentReg = registration;
                    Hashtable<String, Object> currentProps = serviceProperties;

                    ServiceRegistration<S> newReg = null;
                    Hashtable<String, Object> newProps = null;
                    modifyLock.unlock();
                    try {
                        if (isUnregistered) {
                            return; // nothing to do if already unregistered
                        }
                        if (doUnregister) {
                            if (currentReg != null) {
                                currentReg.unregister();
                                return; // we are done after unregistering
                            }
                        } else {
                            if (currentReg != null || currentContext != null) {
                                newProps = supplier.getServiceProperties();
                                if (newProps != null && (currentProps == null || !currentProps.equals(newProps))) {
                                    // make a copy of the new props to protect ourselves of external changes to the results
                                    newProps = new Hashtable<String, Object>(newProps);
                                    if (currentReg != null) {
                                        currentReg.setProperties(newProps);
                                    } else {
                                        newReg = currentContext.registerService(serviceClass, service, newProps);
                                        currentReg = newReg;
                                    }
                                    currentProps = newProps;
                                }
                            }
                        }
                    } catch (IllegalStateException e) {
                        // happens if already unregistered
                        return;
                    } finally {
                        modifyLock.lock();
                        modifyQueue.poll();
                        if (doUnregister) {
                            unregistered = true;
                            context = null;
                            registration = null;
                            serviceProperties = null;
                        } else {
                            serviceProperties = currentProps;
                            registration = currentReg;
                        }
                    }
                } while (!modifyQueue.isEmpty());
            }
        } finally {
            modifyLock.unlock();
        }
    }
}
