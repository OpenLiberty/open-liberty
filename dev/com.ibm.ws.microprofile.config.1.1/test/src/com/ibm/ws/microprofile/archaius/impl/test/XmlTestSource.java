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
package com.ibm.ws.microprofile.archaius.impl.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class XmlTestSource implements ConfigSource {

    final Properties p = new Properties();
    private final int ordinal = 100;
    private String name;

    public XmlTestSource(String resourceName) {
        try {
            p.loadFromXML(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public XmlTestSource() {
        this("META-INF/config.xml");
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return propertiesToMap(p);
    }

    /**
     * @return
     */
    private ConcurrentMap<String, String> propertiesToMap(Properties p) {
        ConcurrentMap<String, String> map = new ConcurrentHashMap<String, String>();
        Set<Entry<Object, Object>> entries = p.entrySet();
        for (Iterator<Entry<Object, Object>> iterator = entries.iterator(); iterator.hasNext();) {
            Entry<Object, Object> entry = iterator.next();
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
        return map;
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return p.getProperty(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return name;
    }
};