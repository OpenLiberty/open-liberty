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
package com.ibm.ws.microprofile.archaius.impl.test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

public class EnvTestSource implements ConfigSource {

    private static final int DEFAULT_ENV_ORDINAL = ConfigConstants.ORDINAL_ENVIRONMENT_VARIABLES;

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return DEFAULT_ENV_ORDINAL;
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return new ConcurrentHashMap<>(System.getenv());
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return System.getenv(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "System.getenv(propertyName)";
    }
}
