/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        // TODO Auto-generated method stub
        return 0;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return Collections.<String> emptySet();
    }

}
