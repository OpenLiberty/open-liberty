/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal_fat.apps.converter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.microprofile.config.spi.ConfigSource;

public class MySource implements ConfigSource {

    public ConcurrentMap<String, String> props;
    public int ordinal = 700;
    public String id = "mySource";

    public ConcurrentMap<String, String> getProps() {
        return props;
    }

    public void setProps(ConcurrentMap<String, String> props) {
        this.props = props;
    }

    public void setOrdinal(int ordinal) {
        this.ordinal = ordinal;
    }

    public void setid(String id) {
        this.id = id;
    }

    public MySource() {
        props = new ConcurrentHashMap<String, String>();
    }

    /**
     * @param p
     * @param v
     */
    public MySource(String p, String v) {
        props = new ConcurrentHashMap<String, String>();
        put(p, v);
    }

    public MySource put(String key, String value) {
        props.put(key, value);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ConcurrentMap<String, String> getProperties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public int getOrdinal() {
        return ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public Set<String> getPropertyNames() {
        return getProperties().keySet();
    }
}