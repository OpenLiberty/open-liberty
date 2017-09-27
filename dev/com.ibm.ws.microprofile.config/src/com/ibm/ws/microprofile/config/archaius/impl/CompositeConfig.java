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
//Based on com.netflix.archaius.config.DefaultCompositeConfig

package com.ibm.ws.microprofile.config.archaius.impl;

import java.io.Closeable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.microprofile.config.spi.ConfigSource;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.impl.SortedSources;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;
import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.config.AbstractConfig;
import com.netflix.archaius.exceptions.ParseException;

public class CompositeConfig extends AbstractConfig implements Closeable, ConfigListener {

    private static final TraceComponent tc = Tr.register(CompositeConfig.class);
    private final CopyOnWriteArrayList<PollingDynamicConfig> children = new CopyOnWriteArrayList<PollingDynamicConfig>();
    private final CachedCompositePropertyFactory factory;

    /**
     * Constructor
     *
     * @param sources
     * @param decoder
     * @param executor
     */
    public CompositeConfig(SortedSources sources, Decoder decoder, ScheduledExecutorService executor, long refreshInterval) {
        setDecoder(decoder);

        //a default property factory does the job of applying the type conversions and then caching those converted values
        this.factory = new CachedCompositePropertyFactory(this);

        for (ConfigSource source : sources) {
            //add each archaius config to the composite config
            addConfig(source, executor, refreshInterval);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigAdded(Config config) {
        notifyConfigAdded(CompositeConfig.this);
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigRemoved(Config config) {
        notifyConfigRemoved(CompositeConfig.this);
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigUpdated(Config config) {
        notifyConfigUpdated(CompositeConfig.this);
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable error, Config config) {
        notifyError(error, CompositeConfig.this);
    }

    /**
     * Add a config
     *
     * @param source
     * @param executor
     * @return
     * @throws ConfigException
     */
    private PollingDynamicConfig addConfig(ConfigSource source, ScheduledExecutorService executor, long refreshInterval) {
        //we wrap each source up as an archaius config
        PollingDynamicConfig archaiusConfig = new PollingDynamicConfig(source, executor, refreshInterval);

        //add each archaius config to the composite config
        children.add(archaiusConfig);

        archaiusConfig.setStrInterpolator(getStrInterpolator());
        archaiusConfig.setDecoder(getDecoder());
        notifyConfigAdded(archaiusConfig);
        archaiusConfig.addListener(this);

        return archaiusConfig;
    }

    /** {@inheritDoc} */
    @Override
    public Object getRawProperty(String key) {
        CachedCompositeValue<String> rawValue = getRawCompositeValue(key);
        Object raw = rawValue == null ? null : rawValue.getValue();
        return raw;
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> getList(String key, Class<T> type) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child.getList(key, type);
            }
        }
        return notFound(key);
    }

    /** {@inheritDoc} */
    @Override
    public List<?> getList(String key) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return child.getList(key);
            }
        }
        return notFound(key);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsKey(String key) {
        for (Config child : children) {
            if (child.containsKey(key)) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        for (Config child : children) {
            if (!child.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Return a set of all unique keys tracked by any child of this composite.
     * This can be an expensive operations as it requires iterating through all of
     * the children.
     *
     * TODO: Cache keys
     */
    @Override
    public Iterator<String> getKeys() {
        Set<String> result = getKeySet();
        return result.iterator();
    }

    Set<String> getKeySet() {
        HashSet<String> result = new HashSet<>();
        for (PollingDynamicConfig config : children) {
            Iterator<String> iter = config.getKeys();
            while (iter.hasNext()) {
                String key = iter.next();
                boolean added = result.add(key);
                if (added && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "getKeySet", "Key={0}, Source={1}", key, config.getSourceID());
                }
            }
        }
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized <T> T accept(Visitor<T> visitor) {
        T result = null;

        for (Config child : children) {
            result = child.accept(visitor);
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
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

        Iterator<String> keyItr = getKeys();
        while (keyItr.hasNext()) {
            String key = keyItr.next();
            sb.append(key);
            sb.append("=");
            CachedCompositeValue<String> rawCompositeValue = getRawCompositeValue(key);
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

    /**
     * Get a value as a particular type
     *
     * @param <T>
     * @param propertyName
     * @param propertyType
     * @return the value as an object of the passed in Type (or a ConversionException)
     */
    protected <T> T getTypedValue(String propertyName, Class<T> propertyType) {
        CachedCompositePropertyContainer container = this.factory.getProperty(propertyName);
        CachedCompositeProperty<T> property = container.asType(propertyType);
        T value = property.get();
        return value;
    }

    public <T> CachedCompositeValue<T> getCompositeValue(Class<T> type, String key) {
        CachedCompositeValue<String> rawProp = getRawCompositeValue(key);
        if (rawProp == null) {
            return null;
        } else {
            try {
                T value = getDecoder().decode(type, rawProp.getValue());
                CachedCompositeValue<T> composite = new CachedCompositeValue<T>(value, rawProp.getSource());
                return composite;
            } catch (NumberFormatException nfe) {
                throw new ParseException("Error parsing value \'" + rawProp.getValue() + "\' for property \'" + key + "\'", nfe);
            }
        }
    }

    /**
     * @param key
     * @return
     */
    private CachedCompositeValue<String> getRawCompositeValue(String key) {
        for (PollingDynamicConfig child : children) {
            if (child.containsKey(key)) {
                String value = (String) child.getRawProperty(key);
                String source = child.getSourceID();
                CachedCompositeValue<String> raw = new CachedCompositeValue<String>(value, source);
                return raw;
            }
        }
        return null;
    }

    /**
     * @param propertyName
     * @param propertyType
     * @return
     */
    public <T> SourcedValue<T> getSourcedValue(String propertyName, Class<T> propertyType) {
        CachedCompositePropertyContainer container = this.factory.getProperty(propertyName);
        CachedCompositeProperty<T> property = container.asType(propertyType);
        SourcedValue<T> value = property.getSourced();
        return value;
    }
}
