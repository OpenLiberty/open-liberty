/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 *
 */
public class EnvConfigSource extends InternalConfigSource implements StaticConfigSource {

    private static final TraceComponent tc = Tr.register(EnvConfigSource.class);
    private final String name;

    @Trivial
    public EnvConfigSource() {
        name = Tr.formatMessage(tc, "environment.variables.config.source");
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {

        Map<String, String> props = AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            @Trivial
            public Map<String, String> run() {
                return System.getenv();
            }
        });

        return new ConcurrentHashMap<>(props);
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ConfigConstants.ORDINAL_ENVIRONMENT_VARIABLES;
    }

    @Override
    public String toString() {
        return "Environment Variables Config Source";
    }

    @Override
    public String getValue(String propertyName) {
        return super.getValue(propertyName);
    }

}
