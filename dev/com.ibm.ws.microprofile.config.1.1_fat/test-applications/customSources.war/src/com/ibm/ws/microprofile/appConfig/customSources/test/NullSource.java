/*******************************************************************************
 * Copyright (c) 2016, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.appConfig.customSources.test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class NullSource implements ConfigSource {

    @Override
    public ConcurrentMap<String, String> getProperties() {
        return null;
    }

    @Override
    public int getOrdinal() {
        return 0;
    }

    @Override
    public String getValue(String propertyName) {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Set<String> getPropertyNames() {
        return Collections.<String> emptySet();
    }

}
