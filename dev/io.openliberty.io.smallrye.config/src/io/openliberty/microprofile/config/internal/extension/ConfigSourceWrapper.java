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

public class ConfigSourceWrapper implements ConfigSource {
    private static final TraceComponent tc = Tr.register(ConfigSourceWrapper.class);
    private final ConfigSource source;
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
                entries.add(new RecordingConfigMapEntry(entry.getKey(), entry.getValue()));
            }
            return entries;
        }

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
        if (OLSmallRyeConfigExtension.isRecording()) {
            // Map gets filled only when a config property is accessed during the early startup of bean during checkpoint.
            propertiesAccessedOnCheckpoint.putIfAbsent(propertyName, propertyValue);

            //register hook
            if (hookAdded.compareAndSet(false, true)) {
                phase.addMultiThreadedHook(new CheckpointHook() {
                    @Override
                    public void restore() {
                        debug(tc, () -> "Config Source: " + source.getName());
                        debug(tc, () -> "Properties accessed on checkpoint: " + propertiesAccessedOnCheckpoint);
                        // When a config property is accessed during the early startup of bean during checkpoint, it might not get updated on restore.
                        // Throw a warning to let user know that the updated property might not be used.
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