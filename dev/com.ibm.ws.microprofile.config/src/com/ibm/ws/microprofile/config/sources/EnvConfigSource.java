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
package com.ibm.ws.microprofile.config.sources;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

/**
 *
 */
public class EnvConfigSource extends AbstractConfigSource implements ConfigSource {

    /**
     * @param ordinal
     */
    public EnvConfigSource() {
        super(getEnvOrdinal(), "Environment Variables Config Source");
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {

        Map<String, String> props = AccessController.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            public Map<String, String> run() {
                return System.getenv();
            }
        });

        return new ConcurrentHashMap<>(props);
    }

    public static int getEnvOrdinal() {
        String ordinalProp = getOrdinalEnvVar();
        int ordinal = ConfigConstants.ORDINAL_ENVIRONMENT_VARIABLES;
        if (ordinalProp != null) {
            ordinal = Integer.parseInt(ordinalProp);
        }
        return ordinal;
    }

    private static String getOrdinalEnvVar() {
        String prop = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getenv(ConfigConstants.ORDINAL_PROPERTY);
            }
        });
        return prop;
    }
}
