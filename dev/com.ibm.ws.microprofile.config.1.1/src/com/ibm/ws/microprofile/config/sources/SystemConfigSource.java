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
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

/**
 *
 */
public class SystemConfigSource extends InternalConfigSource implements StaticConfigSource {

    private static final TraceComponent tc = Tr.register(SystemConfigSource.class);
    private final String name;

    @Trivial
    public SystemConfigSource() {
        name = Tr.formatMessage(tc, "system.properties.config.source");
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
        return ConfigConstants.ORDINAL_SYSTEM_PROPERTIES;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        HashMap<String, String> props = new HashMap<>();
        Properties sysProps = getSystemProperties();
        Set<String> keys = sysProps.stringPropertyNames();
        for (String key : keys) {
            if (key != null) {
                String value = sysProps.getProperty(key);
                if (value != null) { //it is possible that a property could be removed while we are looking at them
                    props.put(key, value);
                }
            }
        }

        return props;
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
