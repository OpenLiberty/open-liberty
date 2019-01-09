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
package com.ibm.ws.classloading.internal;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class MockServiceReference<S> implements ServiceReference<S> {

    private static final AtomicInteger rankingCounter = new AtomicInteger(0);
    private final int serviceRanking;
    final S s;

    private MockServiceReference(S s) {
        this.s = s;
        serviceRanking = rankingCounter.getAndIncrement();
    }

    static <S> MockServiceReference<S> wrap(S s) {
        return new MockServiceReference<S>(s);
    }

    S unwrap() {
        return s;
    }

    @Override
    public Object getProperty(String key) {
        if ("service.ranking".equals(key)) {
            return serviceRanking;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public String[] getPropertyKeys() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle getBundle() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle[] getUsingBundles() {
        throw new UnsupportedOperationException();

    }

    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(Object reference) {
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.osgi.framework.ServiceReference#getProperties()
     */
    @Override
    public Dictionary<String, Object> getProperties() {
        return new Hashtable<>();
    }

}
