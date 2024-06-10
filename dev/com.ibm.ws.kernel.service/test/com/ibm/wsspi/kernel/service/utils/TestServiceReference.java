/*******************************************************************************
 * Copyright (c) 2012, 2024 IBM Corporation and others.
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
package com.ibm.wsspi.kernel.service.utils;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

class TestServiceReference implements ServiceReference<String> {

    private static final AtomicLong nextId = new AtomicLong();

    final Long id = nextId.getAndIncrement();
    final String name;
    Object ranking;

    TestServiceReference(String name) {
        this.name = name;
    }

    TestServiceReference(String name, Integer ranking) {
        this.name = name;
        this.ranking = ranking;
    }

    @Override
    public <A> A adapt(Class<A> arg0) {
        return null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '[' + name + ", id=" + id + ", ranking=" + ranking + ']';
    }

    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public Object getProperty(String key) {
        if (key.equals(Constants.SERVICE_ID)) {
            return id;
        }
        if (key.equals(Constants.SERVICE_RANKING)) {
            return ranking;
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getPropertyKeys() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Bundle getBundle() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Bundle[] getUsingBundles() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAssignableTo(Bundle bundle, String className) {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Object reference) {
        TestServiceReference other = (TestServiceReference) reference;
        final int thisRanking = !(this.ranking instanceof Integer) ? 0 : ((Integer) this.ranking).intValue();
        final int otherRanking = !(other.ranking instanceof Integer) ? 0 : ((Integer) other.ranking).intValue();
        if (thisRanking != otherRanking) {
            if (thisRanking < otherRanking) {
                return -1;
            }
            return 1;
        }

        if (this.id == other.id) {
            return name.compareTo(other.name);
        }
        if (this.id < other.id) {
            return 1;
        }
        return -1;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TestServiceReference other = (TestServiceReference) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public Dictionary<String, Object> getProperties() {
        return new Hashtable<>();
    }
}
