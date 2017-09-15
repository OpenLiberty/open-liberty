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

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

/**
 * Implement just enough of the ComponentContext interface to support the unit tests.
 */
public class MockComponentContext implements ComponentContext {

    private final Map<String, Map<ServiceReference<?>, Object>> services =
                    new HashMap<String, Map<ServiceReference<?>, Object>>();

    private final BundleContext bundleContext = new MockBundleContext();

    public void addService(String name, ServiceReference<?> ref, Object serviceObject) {
        Map<ServiceReference<?>, Object> _services = services.get(name);
        if (_services == null) {
            _services = new HashMap<ServiceReference<?>, Object>();
            services.put(name, _services);
        }
        _services.put(ref, serviceObject);
    }

    @Override
    public void disableComponent(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableComponent(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    @Override
    public ComponentInstance getComponentInstance() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Dictionary getProperties() {
        throw new UnsupportedOperationException();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ServiceReference getServiceReference() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getUsingBundle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object locateService(String name) {
        Map<ServiceReference<?>, Object> _services = services.get(name);
        if (_services != null) {
            Iterator<Object> i = _services.values().iterator();
            if (i.hasNext()) {
                return i.next();
            }
        }
        return null;
    }

    @Override
    public Object locateService(String name,
                                @SuppressWarnings("rawtypes") ServiceReference reference) {
        Map<ServiceReference<?>, Object> _services = services.get(name);
        if (_services != null) {
            return _services.get(reference);
        }
        return null;
    }

    @Override
    public Object[] locateServices(String name) {
        Map<ServiceReference<?>, Object> _services = services.get(name);
        if (_services != null) {
            Collection<Object> serviceObjects = _services.values();
            return serviceObjects.toArray(new Object[serviceObjects.size()]);
        }
        return null;
    }

}
