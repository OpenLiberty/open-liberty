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
package com.ibm.ws.microprofile.config.basic.test;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.AbstractConfigSource;

/**
 *
 */
public class ValueOnlyConfigSource extends AbstractConfigSource implements ConfigSource {

    int getValueCount = 0;

    public ValueOnlyConfigSource(String id) {
        this(100, id);
    }

    public ValueOnlyConfigSource(int ordinal, String id) {
        super(ordinal, id);
    }

    //override getProperties so that it never returns anything useful!
    @Override
    public Map<String, String> getProperties() {
        return new HashMap<>();
    }

    @Override
    public String getValue(String propertyName) {
        getValueCount++;
        return super.getValue(propertyName);
    }
}
