/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.converter.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class NullableConfigSource extends HashMap<String, String> implements ConfigSource {

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "NullableConfigSource";
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String key) {
        return get(key);
    }

}
