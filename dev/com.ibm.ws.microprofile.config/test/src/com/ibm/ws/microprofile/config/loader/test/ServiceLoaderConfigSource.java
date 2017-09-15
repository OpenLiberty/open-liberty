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
package com.ibm.ws.microprofile.config.loader.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

@SuppressWarnings("serial")
public class ServiceLoaderConfigSource extends ConcurrentHashMap<String, String> implements ConfigSource {

    public ServiceLoaderConfigSource() {
        put("SLKey1", "SLValue1");
        put("SLKey2", "SLValue2");
        put("SLKey3", "SLValue3");
        put("SLKey4", "SLValue4");
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return 100;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "ServiceLoaderConfigSource";
    }
}
