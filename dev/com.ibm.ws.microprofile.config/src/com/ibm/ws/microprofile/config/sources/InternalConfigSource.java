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
package com.ibm.ws.microprofile.config.sources;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.microprofile.config.interfaces.ConfigStartException;

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
    @FFDCIgnore({ ConfigStartException.class })
    public String getValue(String propertyName) {
        String theValue = null;
        try {
            theValue = getProperties().get(propertyName);
        } catch (ConfigStartException cse) {
            // Swallow this exception
        }

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
}
