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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.impl.ConversionManager;
import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.ibm.ws.microprofile.config.sources.InternalConfigSource;

public class CompositeConfig implements Closeable, ConfigListener {

    private static final TraceComponent tc = Tr.register(CompositeConfig.class);
    private final CopyOnWriteArrayList<PollingDynamicConfig> children = new CopyOnWriteArrayList<PollingDynamicConfig>();
    private final CopyOnWriteArrayList<ConfigListener> listeners = new CopyOnWriteArrayList<ConfigListener>();
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
        for (ConfigListener listener : listeners) {
            listener.onConfigAdded();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigUpdated() {
        //pass the notification on to our listeners
        for (ConfigListener listener : listeners) {
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
        //if it is an internal config source then it should not refresh
        //this is a hack for now ... dynamic config sources will be fixed properly in the next version
        if (source instanceof InternalConfigSource) {
            refreshInterval = 0;
        }

        //we wrap each source up as an archaius config
        PollingDynamicConfig archaiusConfig = new PollingDynamicConfig(source, executor, refreshInterval);

        //add each archaius config to the composite config
        children.add(archaiusConfig);

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
        listeners.add(listener);
    }

    /**
     * Return a set of all unique keys tracked by any child of this composite.
     * This can be an expensive operations as it requires iterating through all of
     * the children.
     *
     * TODO: Cache keys
     */
    public Set<String> getKeySet() {
        boolean dumpEnabled = TraceComponent.isAnyTracingEnabled() && tc.isDumpEnabled();
        StringBuilder dump = null; //dump debug only
        boolean first = true; //dump debug only

        HashSet<String> result = new HashSet<>();

        if (dumpEnabled) {
            dump = new StringBuilder("getKeySet: [");
        }

        for (PollingDynamicConfig config : children) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                boolean added = result.add(key);
                if (dumpEnabled && added) {
                    if (!first) {
                        dump.append(";\n");
                    } else {
                        first = false;
                    }
                    dump.append("Key=");
                    dump.append(key);
                    dump.append(", Source=");
                    dump.append(config.getSourceID());
                }
            }
        }
        if (dumpEnabled) {
            dump.append("]");
            Tr.dump(tc, dump.toString());
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    @Trivial
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (PollingDynamicConfig child : children) {
            sb.append(child).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        Set<String> keys = getKeySet();
        Iterator<String> keyItr = keys.iterator();
        while (keyItr.hasNext()) {
            String key = keyItr.next();
            sb.append(key);
            sb.append("=");
            SourcedValue rawCompositeValue = getRawCompositeValue(key);
            if (rawCompositeValue == null) {
                sb.append("null");
            } else {
                sb.append(rawCompositeValue);
            }
            if (keyItr.hasNext()) {
                sb.append(",");
            }
        }

        sb.append("]");
        return sb.toString();

    }

    /** {@inheritDoc} */
    @Override
    public void close() {
        for (PollingDynamicConfig child : children) {
            child.close();
        }
    }

    public SourcedValue getSourcedValue(Type type, String key) {
        SourcedValue rawProp = getRawCompositeValue(key);
        if (rawProp == null) {
            return null;
        } else {
            Object value = this.conversionManager.convert((String) rawProp.getValue(), type);
            SourcedValue composite = new SourcedValueImpl(value, type, rawProp.getSource());
            return composite;
        }
    }

    /**
     * @param key
     * @return
     */
    private SourcedValue getRawCompositeValue(String key) {
        for (PollingDynamicConfig child : children) {
            if (child.containsKey(key)) {
                String value = child.getRawProperty(key);
                String source = child.getSourceID();
                SourcedValue raw = new SourcedValueImpl(value, String.class, source);
                return raw;
            }
        }
        return null;
    }

}
