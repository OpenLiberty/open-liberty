/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 *
 */
public class ChangeSupportConfigSource implements ConfigSource {

    HashMap<String, String> props = new HashMap<>();
    private final Set<Consumer<Set<String>>> callbacks = new HashSet<>();
    private final int ordinal;

    public ChangeSupportConfigSource() {
        this(ConfigSource.DEFAULT_ORDINAL);
    }

    public ChangeSupportConfigSource(int ordinal) {
        this.ordinal = ordinal;
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, String> getProperties() {
        return props;
    }

    /** {@inheritDoc} */
    @Override
    public String getValue(String propertyName) {
        return props.get(propertyName);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "ChangeSupportConfigSource";
    }

    @Override
    public int getOrdinal() {
        return this.ordinal;
    }

    @Override
    public ChangeSupport onAttributeChange(Consumer<Set<String>> callback) {
        this.callbacks.add(callback);
        return () -> ChangeSupport.Type.SUPPORTED;
    }

    private void notifyAttributeChange(String name) {
        for (Consumer<Set<String>> consumer : this.callbacks) {
            consumer.accept(Collections.singleton(name));
        }
    }

    public void put(String name, String value) {
        this.props.put(name, value);
        notifyAttributeChange(name);
    }

    public void remvoe(String name) {
        this.props.remove(name);
        notifyAttributeChange(name);
    }
}
