/*******************************************************************************

    Copyright (c) 2017 IBM Corporation and others.
    All rights reserved. This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html
    Contributors:

    IBM Corporation - initial API and implementation

*******************************************************************************/

package com.ibm.ws.microprofile.config.converter.test;

import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * A config source backed by a map
 */
public class SimpleConfigSource extends ConcurrentHashMap<String, String> implements ConfigSource {

    private static final long serialVersionUID = 1L;

    /** {@inheritDoc} */
    @Override
    public ConcurrentHashMap<String, String> getProperties() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return this.get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.getClass().getName();
    }

}
