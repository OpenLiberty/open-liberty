/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jaas.common.internal;

import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import com.ibm.ws.classloading.ClassProvider;
import com.ibm.ws.classloading.LibertyClassLoader;

/**
 * This proxy class provider delegates to another class provider that wasn't available
 * at the time of registration.
 */
class ClassProviderProxy implements ServiceListener {
    /**
     * com.ibm.ws.app.manager_# service.pid that is resolved by the classProviderRef
     */
    private final String appMgrPid;

    /**
     * Proxied class provider instance. This is lazily initialized after it becomes available.
     */
    private ClassProvider classProvider;

    /**
     * Service reference to the proxied class provider instance. This is supplied once it becomes available.
     */
    private ServiceReference<ClassProvider> classProviderRef;

    /**
     * Lock for accessing all of the fields of this class.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Registration of this instance, which is a prerequisite of activating a JAASLoginModuleConfigImpl that has a classProviderRef.
     */
    private ServiceRegistration<ClassProviderProxy> registration;

    ClassProviderProxy(String appMgrPid) {
        this.appMgrPid = appMgrPid;
    }

    LibertyClassLoader getDelegateLoader() {
        lock.readLock().lock();
        try {
            if (classProvider == null) {
                // Switch to write lock for initialization
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (classProvider == null) {
                        BundleContext bundleContext = FrameworkUtil.getBundle(ClassProviderProxy.class).getBundleContext();
                        classProvider = bundleContext.getService(classProviderRef);
                    }
                } finally {
                    // Downgrade to read lock for rest of method
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }

            return classProvider.getDelegateLoader();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Register this proxy class provider to load from a resource adapter's class provider, which is now available.
     *
     * @param classProviderRef reference to the proxied ClassProvider that is now available.
     */
    void register(ServiceReference<ClassProvider> classProviderRef) {
        BundleContext bundleContext = FrameworkUtil.getBundle(ClassProviderProxy.class).getBundleContext();

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("provides.classes.for.pid", appMgrPid);

        lock.writeLock().lock();
        try {
            if (registration == null) {
                this.classProviderRef = classProviderRef;
                registration = bundleContext.registerService(ClassProviderProxy.class, this, props);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serviceChanged(ServiceEvent event) {
        switch (event.getType()) {
            case ServiceEvent.REGISTERED:
                register((ServiceReference<ClassProvider>) event.getServiceReference());
                break;
            case ServiceEvent.UNREGISTERING:
            case ServiceEvent.MODIFIED_ENDMATCH:
                unregister();
                break;
            case ServiceEvent.MODIFIED:
                lock.writeLock().lock();
                classProvider = null;
                classProviderRef = (ServiceReference<ClassProvider>) event.getServiceReference();
                lock.writeLock().unlock();
                break;
            default:
                throw new IllegalStateException(event.toString()); // should be unreachable
        }
    }

    /**
     * Unregister this proxy class provider.
     */
    void unregister() {
        lock.writeLock().lock();
        try {
            if (registration != null) {
                registration.unregister();
                registration = null;
                classProvider = null;
                classProviderRef = null;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
