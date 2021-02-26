/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
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
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 *
 */
public abstract class InternalConfigSource implements ConfigSource {

    // Transient to make it easier for subclasses to be serializable
    private transient int ordinal = Integer.MIN_VALUE;

    /** {@inheritDoc} */
    @Override
    @Trivial
    @FFDCIgnore(NumberFormatException.class)
    public int getOrdinal() {
        // Cache the ordinal on first lookup to ensure it doesn't change
        // Potentially having it change would make sorting the config source list a mess
        if (ordinal == Integer.MIN_VALUE) {
            String ordinalString = getValue(CONFIG_ORDINAL);
            if (ordinalString != null) {
                try {
                    ordinal = Integer.parseInt(ordinalString);
                } catch (NumberFormatException e) {
                    ordinal = getDefaultOrdinal();
                }
            } else {
                ordinal = getDefaultOrdinal();
            }
        }

        return ordinal;
    }

    /**
     * The default ordinal if {@code config_ordinal} is not defined in the config source
     *
     * @return the default ordinal
     */
    protected abstract int getDefaultOrdinal();

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        String theValue = null;
        Map<String, String> theValueMap = getProperties();
        theValue = theValueMap.get(propertyName);
        return theValue;
    }

    @Override
    public String toString() {
        if (ordinal == Integer.MIN_VALUE) {
            // We could call getOrdinal here, but don't want to mutate state inside toString()
            return getName() + "(getOrdinal not yet called)";
        } else {
            return getName() + "(" + ordinal + ")";
        }
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}
