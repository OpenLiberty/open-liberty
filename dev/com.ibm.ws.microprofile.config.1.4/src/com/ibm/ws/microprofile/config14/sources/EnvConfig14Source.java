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
package com.ibm.ws.microprofile.config14.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config13.sources.EnvConfig13Source;

/**
 * Enhancements to the EnvConfigSource class for MP Config 1.4.
 */
public class EnvConfig14Source extends EnvConfig13Source {

    private final ConcurrentMap<String, String> environment;

    /**
     * @param ordinal
     */
    public EnvConfig14Source() {
        super();
        this.environment = getEnvironment();
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return this.environment;
    }

    private ConcurrentMap<String, String> getEnvironment() {

        Map<String, String> props = AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            @Trivial
            public Map<String, String> run() {
                return System.getenv();
            }
        });

        return new ConcurrentHashMap<>(props);
    }

    @Override
    public String toString() {
        return "Environment Variables Config 1.4 Source";
    }
}
