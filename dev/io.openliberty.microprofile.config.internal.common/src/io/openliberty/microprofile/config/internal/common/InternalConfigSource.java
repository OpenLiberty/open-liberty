/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.common;

import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;

/**
 *
 */
public abstract class InternalConfigSource implements ConfigSource {

    private final int ordinal;
    private final String id;

    @Trivial
    public InternalConfigSource(int ordinal, String id) {
        this.ordinal = ordinal;
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        String theValue = null;
        Map<String, String> theValueMap = getProperties();
        theValue = theValueMap.get(propertyName);
        return theValue;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return id;
    }

    @Override
    public String toString() {
        return getName() + "(" + getOrdinal() + ")";
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}
