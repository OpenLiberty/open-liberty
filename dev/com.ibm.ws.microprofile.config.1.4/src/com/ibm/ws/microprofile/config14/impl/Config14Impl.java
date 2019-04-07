/*******************************************************************************
 * Copyright (c) 2018, 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.config14.impl;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.ConfigAccessor;
import org.eclipse.microprofile.config.ConfigAccessorBuilder;
import org.eclipse.microprofile.config.ConfigSnapshot;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSource.ChangeSupport;
import org.eclipse.microprofile.config.spi.Converter;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.impl.AbstractConfig;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config.impl.SourcedValueImpl;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config14.interfaces.Config14Constants;
import com.ibm.ws.microprofile.config14.interfaces.WebSphereConfig14;

public class Config14Impl extends AbstractConfig implements WebSphereConfig14 {

    private static final TraceComponent tc = Tr.register(Config14Impl.class);

    private final Map<PropertyChangeListener, String> listeners = new HashMap<>();
    private final Set<ChangeSupport> changeSupportListeners = new HashSet<>();

    /**
     * The sources passed in should have already been wrapped up as an unmodifiable copy
     *
     * @param sources
     * @param converters
     * @param executor
     */
    public Config14Impl(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        super(conversionManager, sources);
        for (ConfigSource source : sources) {
            ChangeSupport changeSupport = source.onAttributeChange(this);
            this.changeSupportListeners.add(changeSupport);
            //TODO do something with changeSupport
        }
    }

    @Override
    protected Object getValue(String propertyName, Type propertyType, boolean optional, String defaultString) {
        Object value = super.getValue(propertyName, propertyType, optional, defaultString);
        if (ConfigProperty.NULL_VALUE.equals(value)) {
            value = null;
        }
        return value;
    }

    @Override
    public Object getValue(String propertyName, Type propertyType, boolean optional, String defaultString, boolean evaluateVariables) {
        Object value = null;
        assertNotClosed();

        SourcedValue sourced = getSourcedValue(propertyName, propertyType, evaluateVariables);
        if (sourced != null) {
            value = sourced.getValue();
        } else {
            if (optional) {
                value = convertValue(defaultString, propertyType);
            } else {
                //TODO make sure this message is accessible from this bundle
                throw new NoSuchElementException(Tr.formatMessage(tc, "no.such.element.CWMCG0015E", propertyName));
            }
        }
        return value;
    }

    @Override
    public SourcedValue getSourcedValue(String key, Type type) {
        SourcedValue sourcedValue = getSourcedValue(key, type, Config14Constants.CONFIG_EVALUATE_VARIABLES_DEFAULT);
        return sourcedValue;
    }

    @Override
    public SourcedValue getSourcedValue(String key, Type type, boolean evaluateVariables) {
        assertNotClosed();
        SourcedValue sourcedValue = getSourcedValue(Collections.singletonList(key), type, null, ConfigProperty.UNCONFIGURED_VALUE,
                                                    evaluateVariables,
                                                    null);
        return sourcedValue;
    }

    @Override
    public SourcedValue getSourcedValue(List<String> keys, Type type, Class<?> genericSubType, Object defaultValue, boolean evaluateVariables,
                                        Converter<?> converter) {
        assertNotClosed();
        SourcedValue sourcedValue = null; //sourcedValue is the fully resolved and converted value from the config
        SourcedValue rawProp = null; //rawProp is the unconverted String from the config
        Iterator<String> itr = keys.iterator();
        String key = null;
        while (itr.hasNext()) { //go through the available keys in order to find one which doesn't return null
            key = itr.next();
            rawProp = getRawSourcedValue(key);
            if (rawProp != null) {
                break;
            }
        }

        if (rawProp == null) { //if rawProp is still null then use the defaults if available
            if (!ConfigProperty.UNCONFIGURED_VALUE.equals(defaultValue)) {
                sourcedValue = new SourcedValueImpl(key, defaultValue, type, genericSubType, Config14Constants.DEFAULT_VALUE_SOURCE_NAME);
            }
        }

        //convert the raw String value
        //if sourceValue is not null at this point then it means the defaultValue was used and there is no need to convert the raw String property
        if (sourcedValue == null && rawProp != null) {
            String stringValue = (String) rawProp.getValue();
            if (evaluateVariables && stringValue != null) {
                stringValue = PropertyResolverUtil.resolve(this, stringValue);
            }

            Object value = null;
            if (converter == null) {
                value = this.getConversionManager().convert(stringValue, type, genericSubType);
            } else {
                value = converter.convert(stringValue);
            }
            sourcedValue = new SourcedValueImpl(rawProp, value);
        }

        return sourcedValue;
    }

    /**
     * @param key
     * @return An unconverted SourcedValue
     */
    @Trivial
    private SourcedValue getRawSourcedValue(String key) {
        SourcedValue rawProp = null;
        for (ConfigSource child : getConfigSources()) {
            String value = child.getValue(key);
            if (value != null || child.getPropertyNames().contains(key)) {
                String source = child.getName();
                rawProp = new SourcedValueImpl(key, value, String.class, source);
                break;
            }
        }
        return rawProp;
    }

    /** {@inheritDoc} */
    @Override
    protected Set<String> getKeySet() {
        HashSet<String> result = new HashSet<>();

        for (ConfigSource config : getConfigSources()) {
            Map<String, String> props = config.getProperties();
            if (props != null) {
                Set<String> keys = props.keySet();
                result.addAll(keys);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String dump() {
        assertNotClosed();
        StringBuilder sb = new StringBuilder();
        Set<String> keys = getKeySet();
        keys = new TreeSet<String>(keys);
        Iterator<String> keyItr = keys.iterator();
        while (keyItr.hasNext()) {
            String key = keyItr.next();
            SourcedValue rawSourcedValue = getRawSourcedValue(key);
            if (rawSourcedValue == null) {
                sb.append("null");
            } else {
                sb.append(rawSourcedValue);
            }
            if (keyItr.hasNext()) {
                sb.append("\n");
            }
        }

        return sb.toString();

    }

    /** {@inheritDoc} */
    @Override
    public <T> ConfigAccessorBuilder<T> access(String propertyName, Class<T> type) {
        assertNotClosed();
        ConfigAccessorBuilder<T> accessor = new ConfigAccessorBuilderImpl<>(this, propertyName, type);
        return accessor;
    }

    /** {@inheritDoc} */
    @Override
    public ConfigSnapshot snapshotFor(ConfigAccessor<?>... configValues) {
        assertNotClosed();
        return new ConfigSnapshotImpl(configValues);
    }

    /** {@inheritDoc} */
    @Override
    public void accept(Set<String> propertyNames) {
        if (!isClosed()) {
            synchronized (this) {
                for (Map.Entry<PropertyChangeListener, String> entry : listeners.entrySet()) {
                    String propertyName = entry.getValue();
                    if (propertyNames.contains(propertyName)) {
                        PropertyChangeListener listener = entry.getKey();
                        listener.onPropertyChanged();
                    }
                }
            }
        }
    }

    @Override
    public void close() {
        if (!isClosed()) {
            for (ChangeSupport changeSupport : this.changeSupportListeners) {
                changeSupport.close();
            }
            super.close();
        }
    }

    @Override
    public void registerPropertyChangeListener(PropertyChangeListener listener, String propertyName) {
        assertNotClosed();
        synchronized (this) {
            this.listeners.put(listener, propertyName);
        }
    }
}
