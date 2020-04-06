/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Based on com.netflix.archaius.config.DefaultCompositeConfig and com.netflix.archaius.config.AbstractConfig

package com.ibm.ws.microprofile.config.archaius.composite;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SourcedValueImpl;
import com.ibm.ws.microprofile.config.interfaces.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.sources.StaticConfigSource;

public class CompositeConfig implements Closeable, ConfigListener {

    private final CopyOnWriteArrayList<PollingDynamicConfig> children = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<>();
    private final ConversionManager conversionManager;

    /**
     * Constructor
     *
     * @param sources
     * @param decoder
     * @param executor
     */
    public CompositeConfig(ConversionManager conversionManager, SortedSources sources, ScheduledExecutorService executor, long refreshInterval) {
        this.conversionManager = conversionManager;

        for (ConfigSource source : sources) {
            //add each archaius config to the composite config
            addConfig(source, executor, refreshInterval);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigAdded() {
        //pass the notification on to our listeners
        for (ConfigListener listener : this.listeners) {
            listener.onConfigAdded();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigUpdated() {
        //pass the notification on to our listeners
        for (ConfigListener listener : this.listeners) {
            listener.onConfigUpdated();
        }
    }

    /**
     *
     * @param source
     * @param executor
     * @param refreshInterval
     * @return
     */
    private PollingDynamicConfig addConfig(ConfigSource source, ScheduledExecutorService executor, long refreshInterval) {
        //if it is an internal static config source then it should not refresh
        //this is a hack for now ... dynamic config sources will be fixed properly in the next version
        if (source instanceof StaticConfigSource) {
            refreshInterval = 0;
        }

        //we wrap each source up as an archaius config
        PollingDynamicConfig archaiusConfig = new PollingDynamicConfig(source, executor, refreshInterval);

        //add each archaius config to the composite config
        this.children.add(archaiusConfig);

        onConfigAdded();
        archaiusConfig.addListener(this);

        return archaiusConfig;
    }

    /**
     * Register a listener that will receive a call for each property that is added, removed
     * or updated. It is recommended that the callbacks be invoked only after a full refresh
     * of the properties to ensure they are in a consistent state.
     *
     * @param listener
     */
    public void addListener(ConfigListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Return a set of all unique keys tracked by any child of this composite.
     * This can be an expensive operations as it requires iterating through all of
     * the children.
     *
     * TODO: Cache keys
     */
    public Set<String> getKeySet() {
        HashSet<String> result = new HashSet<>();

        for (PollingDynamicConfig config : this.children) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                result.add(key);
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CompositeConfig[");
        sb.append(hashCode());
        sb.append("](");
        sb.append(this.children.size());
        sb.append(" children");
        sb.append(")");
        return sb.toString();
    }

    @Trivial
    public String dump() {
        StringBuilder sb = new StringBuilder();
        Set<String> keys = getKeySet();
        keys = new TreeSet<>(keys);
        Iterator<String> keyItr = keys.iterator();
        while (keyItr.hasNext()) {
            String key = keyItr.next();
            SourcedValue rawCompositeValue = getRawCompositeValue(key);
            if (rawCompositeValue == null) {
                sb.append("null");
            } else {
                sb.append(rawCompositeValue);
            }
            if (keyItr.hasNext()) {
                sb.append("\n");
            }
        }

        return sb.toString();

    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        for (PollingDynamicConfig child : this.children) {
            child.close();
        }
    }

    public SourcedValue getSourcedValue(Type type, String key) {
        SourcedValue sourcedValue = null;
        SourcedValue rawProp = getRawCompositeValue(key);
        if (rawProp != null) {
            Object value = this.conversionManager.convert((String) rawProp.getValue(), type);
            sourcedValue = new SourcedValueImpl(key, value, type, rawProp.getSource());
        }
        return sourcedValue;
    }

    /**
     * @param key
     * @return
     */
    @Trivial
    private SourcedValue getRawCompositeValue(String key) {
        SourcedValue raw = null;
        for (PollingDynamicConfig child : this.children) {
            String value = child.getRawProperty(key);
            if ((value != null) || child.containsKey(key)) {
                String source = child.getSourceID();
                raw = new SourcedValueImpl(key, value, String.class, source);
                break;
            }
        }
        return raw;
    }

}
