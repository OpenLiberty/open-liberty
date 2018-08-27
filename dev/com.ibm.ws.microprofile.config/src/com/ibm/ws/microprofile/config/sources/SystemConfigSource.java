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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

/**
 *
 */
public class SystemConfigSource extends InternalConfigSource implements StaticConfigSource {

    private static final TraceComponent tc = Tr.register(SystemConfigSource.class);

    public SystemConfigSource() {
        super(getSystemOrdinal(), Tr.formatMessage(tc, "system.properties.config.source"));
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        ConcurrentMap<String, String> props = new ConcurrentHashMap<>();
        Properties sysProps = getSystemProperties();
        Set<String> keys = sysProps.stringPropertyNames();
        for (String key : keys) {
            if (key != null) {
                props.put(key, sysProps.getProperty(key));
            }
        }

        return props;
    }

    @Trivial
    public static int getSystemOrdinal() {
        String ordinalProp = getOrdinalSystemProperty();
        int ordinal = ConfigConstants.ORDINAL_SYSTEM_PROPERTIES;
        if (ordinalProp != null) {
            ordinal = Integer.parseInt(ordinalProp);
        }
        return ordinal;
    }

    @Trivial
    private static String getOrdinalSystemProperty() {
        String prop = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            @Trivial
            public String run() {
                return System.getProperty(ConfigConstants.ORDINAL_PROPERTY);
            }
        });
        return prop;
    }

    @Trivial
    private static Properties getSystemProperties() {
        Properties prop = AccessController.doPrivileged(new PrivilegedAction<Properties>() {
            @Override
            @Trivial
            public Properties run() {
                return System.getProperties();
            }
        });
        return prop;
    }
}
