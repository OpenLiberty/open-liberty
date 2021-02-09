/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config.sources;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.interfaces.ConfigConstants;

import io.openliberty.microprofile.config.internal.common.ConfigException;
import io.openliberty.microprofile.config.internal.common.InternalConfigSource;

public class PropertiesConfigSource extends InternalConfigSource implements StaticConfigSource {

    private static final TraceComponent tc = Tr.register(PropertiesConfigSource.class);

    private final ConcurrentMap<String, String> properties;
    private final String id;

    @Trivial
    public PropertiesConfigSource(URL resource) {
        this(loadProperties(resource), resource.toString());
    }

    @Trivial
    public PropertiesConfigSource(ConcurrentMap<String, String> properties, String id) {
        this.id = Tr.formatMessage(tc, "properties.file.config.source", id);
        this.properties = properties;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String getName() {
        return this.id;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    protected int getDefaultOrdinal() {
        return ConfigConstants.ORDINAL_PROPERTIES_FILE;
    }

    @Override
    public ConcurrentMap<String, String> getProperties() {
        return this.properties;
    }

    public static ConcurrentMap<String, String> loadProperties(URL resource) {
        ConcurrentMap<String, String> props = new ConcurrentHashMap<>();

        InputStream stream = null;

        try {
            stream = resource.openStream();
            Properties properties = new Properties();
            properties.load(stream);
            Set<String> propNames = properties.stringPropertyNames();
            for (String name : propNames) {
                props.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            throw new ConfigException(e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new ConfigException(e);
                }
            }
        }

        return props;
    }
}
