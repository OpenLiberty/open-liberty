/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.config.internal.extension;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

import io.openliberty.checkpoint.spi.CheckpointHook;
import io.openliberty.checkpoint.spi.CheckpointPhase;

/**
 * Wrapper to record which configuration properties are accessed during checkpoint.
 * Additionally, to log a warning that if the value of the configuration key changes after the checkpoint action,
 * the application might not use the updated value.
 *
 */
public class ConfigSourceWrapper implements ConfigSource {
    private static final TraceComponent tc = Tr.register(ConfigSourceWrapper.class);
    private final ConfigSource source;
    /**
     * Reads and writes to the map must synchronize on the map.
     */
    private final Map<String, String> propertiesAccessedOnCheckpoint;
    private final CheckpointPhase phase;
    private final AtomicBoolean hookAdded = new AtomicBoolean();

    public ConfigSourceWrapper(ConfigSource source, CheckpointPhase phase) {
        this.source = source;
        propertiesAccessedOnCheckpoint = new HashMap<>();
        this.phase = phase;
    }

    @Override
    public String getName() {
        return source.getName();
    }

    /**
     * A Map entry used to record if something calls entry.get() to access the value in the map during checkpoint.
     * This is needed to provide a wrapper around the entrySet of the wrapped map.
     */
    @Trivial
    class RecordingConfigMapEntry extends SimpleEntry<String, String> {
        private static final long serialVersionUID = 1L;

        public RecordingConfigMapEntry(String key, String value) {
            super(key, value);
        }

        @Override
        public String getValue() {
            String value = super.getValue();
            recordConfigRead(getKey(), value);
            return value;
        }
    }

    /**
     * A Map used to record if something calls config source getProperties() to access the properties during checkpoint.
     * This is needed to provide a wrapper around config source properties.
     */
    @Trivial
    class RecordingConfigMap extends AbstractMap<String, String> {
        private final Map<String, String> properties;

        RecordingConfigMap(Map<String, String> properties) {
            this.properties = properties;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            Set<Entry<String, String>> entries = new HashSet<>();
            Set<Entry<String, String>> original = properties.entrySet();

            for (Entry<String, String> entry : original) {
                // Wrapping each entry from the entry set to record the values accessed when entry.getValue() is called during checkpoint.
                entries.add(new RecordingConfigMapEntry(entry.getKey(), entry.getValue()));
            }
            return entries;
        }

        // Overriding the Map.get for performance reasons instead of the default implementation of AbstractMap
        // which always iterates over the complete entrySet.
        @Override
        public String get(Object key) {
            String value = properties.get(key);
            recordConfigRead(String.valueOf(key), value);
            return value;
        }

        @Override
        public String toString() {
            return properties.toString();
        }
    }

    @Override
    public Map<String, String> getProperties() {
        if (!phase.restored()) {
            return new RecordingConfigMap(source.getProperties());
        }
        return source.getProperties();
    }

    @Override
    public Set<String> getPropertyNames() {
        return source.getPropertyNames();
    }

    @Override
    public String getValue(String propertyName) {
        String propertyValue = source.getValue(propertyName);
        recordConfigRead(propertyName, propertyValue);
        return propertyValue;
    }

    private void recordConfigRead(String propertyName, String propertyValue) {
        // Check if recording is enabled at this time.
        if (OLSmallRyeConfigExtension.isRecording()) {
            // Map gets filled only when a configuration property is accessed during the early startup of bean during checkpoint.
            synchronized (propertiesAccessedOnCheckpoint) {
                propertiesAccessedOnCheckpoint.putIfAbsent(propertyName, propertyValue);
            }

            // Register hook
            if (hookAdded.compareAndSet(false, true)) {
                phase.addMultiThreadedHook(new CheckpointHook() {
                    @Override
                    public void restore() {
                        synchronized (propertiesAccessedOnCheckpoint) {
                            debug(tc, () -> "Config Source: " + source.getName());
                            debug(tc, () -> "Properties accessed on checkpoint: " + propertiesAccessedOnCheckpoint);
                            // When a config property is accessed during the early startup of bean during checkpoint, it might not get updated on restore.
                            // Log a warning to let user know that the updated property might not be used.
                            for (Map.Entry<String, String> entry : propertiesAccessedOnCheckpoint.entrySet()) {
                                String propertyKey = entry.getKey();
                                String propertyValue = entry.getValue();
                                String currentPropertyValue = source.getValue(propertyKey);

                                if (!Objects.equals(propertyValue, currentPropertyValue)) {
                                    debug(tc, () -> "Configuration property " + propertyKey + " value at checkpoint = " + propertyValue + " and value at restore = "
                                                    + currentPropertyValue);
                                    Tr.warning(tc, "WARNING_UPDATED_CONFIG_PROPERTY_NOT_USED_CWWKC0651", entry.getKey());
                                }
                            }
                            propertiesAccessedOnCheckpoint.clear();
                        }
                    }
                });
            }
        }
    }

    @Trivial
    static void debug(TraceComponent trace, Supplier<String> message) {
        if (TraceComponent.isAnyTracingEnabled() && trace.isDebugEnabled()) {
            Tr.debug(trace, message.get());
        }
    }
}