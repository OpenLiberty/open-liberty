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

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Implement just enough of the ServiceReference interface to support the unit tests.
 */
public class MockServiceReference<S> implements ServiceReference<S> {

    private final Map<String, Object> properties;

    @SuppressWarnings("unchecked")
    public MockServiceReference() {
        this(Collections.EMPTY_MAP);
    }

    public MockServiceReference(String... interfaceNames) {
        properties = new HashMap<String, Object>();
        properties.put(Constants.OBJECTCLASS, interfaceNames);
    }

    public MockServiceReference(Map<String, Object> map) {
        properties = map;
    }

    @Override
    public int compareTo(Object reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public String[] getPropertyKeys() {
        Set<String> propertyKeys = properties.keySet();
        return propertyKeys.toArray(new String[propertyKeys.size()]);
    }

    @Override
    public Bundle[] getUsingBundles() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.ServiceReference#getProperties()
     */
    @Override
    public Dictionary<String, Object> getProperties() {
        return new Hashtable<>(properties);
    }

}
