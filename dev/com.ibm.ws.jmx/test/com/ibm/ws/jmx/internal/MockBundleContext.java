/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jmx.internal;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceObjects;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Implement just enough of the BundleContext interface to support the unit tests.
 */
public class MockBundleContext implements BundleContext {
    private final Map<ServiceReference<?>, Object> _services = new HashMap<ServiceReference<?>, Object>();
    private ServiceListener sl;
    private Filter f;

    public void addService(ServiceReference<?> ref, Object serviceObject) {
        _services.put(ref, serviceObject);
        if (sl != null && f.match(ref)) {
            sl.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref));
        }
    }

    @Override
    public void addBundleListener(BundleListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addFrameworkListener(FrameworkListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addServiceListener(ServiceListener listener) {
        sl = listener;
    }

    @Override
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        sl = listener;
        f = FrameworkUtil.createFilter(filter);
    }

    @Override
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return FrameworkUtil.createFilter(filter);
    }

    @Override
    public ServiceReference<?>[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        List<ServiceReference<?>> refs = new ArrayList<ServiceReference<?>>();
        Filter f = createFilter(filter);
        for (ServiceReference<?> ref : _services.keySet()) {
            if (f.match(ref)) {
                refs.add(ref);
            }
        }

        return refs.toArray(new ServiceReference<?>[refs.size()]);
    }

    @Override
    public Bundle getBundle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle[] getBundles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getDataFile(String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProperty(String key) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S> S getService(ServiceReference<S> reference) {
        return (S) _services.get(reference);
    }

    @Override
    public ServiceReference<?> getServiceReference(String clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> ServiceReference<S> getServiceReference(Class<S> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceReference<?>[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> Collection<ServiceReference<S>> getServiceReferences(Class<S> clazz, String filter) throws InvalidSyntaxException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(String location) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceRegistration<?> registerService(String[] clazzes, Object service, Dictionary<String, ?> properties) {
        return new MockServiceRegistration<Object>();
    }

    @Override
    public ServiceRegistration<?> registerService(String clazz, Object service, Dictionary<String, ?> properties) {
        return new MockServiceRegistration<Object>();
    }

    @Override
    public void removeBundleListener(BundleListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFrameworkListener(FrameworkListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeServiceListener(ServiceListener listener) {
        if (sl == listener) {
            sl = null;
        }
    }

    @Override
    public boolean ungetService(ServiceReference<?> reference) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, S service, Dictionary<String, ?> properties) {
        return new MockServiceRegistration<S>();
    }

    /** {@inheritDoc} */
    @Override
    public Bundle getBundle(String location) {
        throw new UnsupportedOperationException();
    }

    /**
     * @param sr
     */
    public void removeService(ServiceReference<?> sr) {
        _services.remove(sr);
        if (sl != null && f.match(sr)) {
            sl.serviceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, sr));
        }
    }

    @Override
    public <S> ServiceRegistration<S> registerService(Class<S> clazz, ServiceFactory<S> factory, Dictionary<String, ?> properties) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <S> ServiceObjects<S> getServiceObjects(ServiceReference<S> reference) {
        throw new UnsupportedOperationException();
    }
}
