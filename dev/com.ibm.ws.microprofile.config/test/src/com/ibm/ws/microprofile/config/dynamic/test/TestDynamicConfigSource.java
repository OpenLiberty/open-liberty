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
package com.ibm.ws.microprofile.config.dynamic.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

@SuppressWarnings("serial")
public class TestDynamicConfigSource extends ConcurrentHashMap<String, String> implements ConfigSource {

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return Integer.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "DynamicConfigSourceTest";
    }
}
