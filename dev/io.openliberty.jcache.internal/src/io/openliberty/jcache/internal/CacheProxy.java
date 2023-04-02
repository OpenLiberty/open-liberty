/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache.internal;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Configuration;
import javax.cache.integration.CompletionListener;
import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.serialization.SerializationService;

import io.openliberty.jcache.CacheObject;

/**
 * Proxy class for {@link javax.cache.Cache} that will handle all serialization
 * and deserialization internally. Serialization of Liberty classes can only be
 * handled by Liberty's {@link SerializationService} so that work is done here.
 *
 * @see Cache
 */
public class CacheProxy implements Cache<Object, Object> {

    @SuppressWarnings("unused")
    private final static TraceComponent tc = Tr.register(CacheProxy.class);

    private final Cache<Object, Object> cache;
    private final CacheServiceImpl cacheService;

    /**
     * Instantiate a new {@link CacheProxy}.
     *
     * @param cache        The {@link Cache} that this instance will proxy requests to.
     * @param cacheService The {@link CacheServiceImpl} that manages the {@link Cache}.
     */
    public CacheProxy(Cache<Object, Object> cache, CacheServiceImpl cacheService) {
        if (cache == null) {
            throw new NullPointerException("The Cache cannot be null.");
        }
        if (cacheService == null) {
            throw new NullPointerException("The CacheService cannot be null.");
        }

        this.cache = cache;
        this.cacheService = cacheService;
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public void close() {
        cache.close();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public void deregisterCacheEntryListener(CacheEntryListenerConfiguration<Object, Object> cacheEntryListenerConfiguration) {
        cache.deregisterCacheEntryListener(cacheEntryListenerConfiguration);
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeserializeException if the retrieved value cannot be deserialized.
     */
    @Override
    @Sensitive
    public Object get(Object key) {
        /*
         * Get the object, and deserialize it if it has not already been deserialized.
         */
        CacheObject jObject = (CacheObject) cache.get(key);
        return deserializeIfNecessary(jObject);
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeserializeException if any of the retrieved values cannot be deserialized.
     */
    @Override
    @Sensitive
    public Map<Object, Object> getAll(Set<? extends Object> keys) {

        /*
         * Deserialize all of the values.
         */
        Map<Object, Object> values = cache.getAll(keys);
        Map<Object, Object> results = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : values.entrySet()) {
            results.put(entry.getKey(), deserializeIfNecessary((CacheObject) entry.getValue()));
        }

        return results;
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeserializeException if the old value cannot be deserialized.
     */
    @Override
    @Sensitive
    public Object getAndPut(Object key, @Sensitive Object value) {
        CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
        CacheObject oldValue = (CacheObject) cache.getAndPut(key, newValue);

        if (oldValue != null) {
            return deserializeIfNecessary(oldValue);
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeserializeException if the retrieved value cannot be deserialized.
     */
    @Override
    @Sensitive
    public Object getAndRemove(Object key) {
        return deserializeIfNecessary((CacheObject) cache.getAndRemove(key));
    }

    /**
     * {@inheritDoc}
     *
     * @throws DeserializeException if the retrieved value cannot be deserialized.
     */
    @Override
    @Sensitive
    public Object getAndReplace(Object key, @Sensitive Object value) {
        CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
        CacheObject oldValue = (CacheObject) cache.getAndReplace(key, newValue);

        if (oldValue != null) {
            return deserializeIfNecessary(oldValue);
        } else {
            return null;
        }
    }

    @Override
    public CacheManager getCacheManager() {
        return cache.getCacheManager();
    }

    @Override
    public <C extends Configuration<Object, Object>> C getConfiguration(Class<C> clazz) {
        return cache.getConfiguration(clazz);
    }

    @Override
    public String getName() {
        return cache.getName();
    }

    @Override
    public <T> T invoke(Object key, EntryProcessor<Object, Object, T> entryProcessor, Object... arguments) throws EntryProcessorException {
        return cache.invoke(arguments, entryProcessor, arguments);
    }

    @Override
    public <T> Map<Object, EntryProcessorResult<T>> invokeAll(Set<? extends Object> keys,
                                                              EntryProcessor<Object, Object, T> entryProcessor, Object... arguments) {
        return cache.invokeAll(keys, entryProcessor, arguments);
    }

    @Override
    public boolean isClosed() {
        return cache.isClosed();
    }

    /**
     * {@inheritDoc}
     *
     * The Iterator returned from this method can throw DeserializationException when accessing the values.
     */
    @Override
    public Iterator<Cache.Entry<Object, Object>> iterator() {
        return new CacheProxyIterator(cache.iterator());
    }

    @Override
    public void loadAll(Set<? extends Object> keys, boolean replaceExistingValues,
                        CompletionListener completionListener) {
        cache.loadAll(keys, replaceExistingValues, completionListener);
    }

    @Override
    public void put(Object key, @Sensitive Object value) {
        CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
        cache.put(key, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * @throws SerializeException if any values in map cannot be serialized.
     */
    @Override
    public void putAll(@Sensitive Map<? extends Object, ? extends Object> map) {
        Map<Object, Object> values = new HashMap<Object, Object>();
        for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
            values.put(key, newValue);
        }

        cache.putAll(values);
    }

    /**
     * {@inheritDoc}
     *
     * @throws SerializeException if value cannot be serialized.
     */
    @Override
    public boolean putIfAbsent(Object key, @Sensitive Object value) {
        CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
        return cache.putIfAbsent(key, newValue);
    }

    @Override
    public void registerCacheEntryListener(
                                           CacheEntryListenerConfiguration<Object, Object> cacheEntryListenerConfiguration) {
        cache.registerCacheEntryListener(cacheEntryListenerConfiguration);
    }

    @Override
    public boolean remove(Object key) {
        return cache.remove(key);
    }

    /**
     * {@inheritDoc}
     *
     * @throws SerializeException if oldValue cannot be serialized.
     */
    @Override
    public boolean remove(Object key, @Sensitive Object oldValue) {
        CacheObject oldValue1 = new CacheObject(oldValue, cacheService.serialize(oldValue));
        return cache.remove(key, oldValue1);
    }

    @Override
    public void removeAll() {
        cache.removeAll();
    }

    @Override
    public void removeAll(Set<? extends Object> keys) {
        cache.removeAll(keys);
    }

    /**
     * {@inheritDoc}
     *
     * @throws SerializeException if value cannot be serialized.
     */
    @Override
    public boolean replace(Object key, @Sensitive Object value) {
        CacheObject newValue = new CacheObject(value, cacheService.serialize(value));
        return cache.replace(key, newValue);
    }

    /**
     * {@inheritDoc}
     *
     * @throws SerializeException if oldValue or newValue cannot be serialized.
     */
    @Override
    public boolean replace(Object key, @Sensitive Object oldValue, @Sensitive Object newValue) {
        CacheObject oldValue1 = new CacheObject(oldValue, cacheService.serialize(oldValue));
        CacheObject newValue1 = new CacheObject(newValue, cacheService.serialize(newValue));
        return cache.replace(key, oldValue1, newValue1);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return cache.unwrap(clazz);
    }

    /**
     * Deserialize the object contained within the {@link CacheObject}.
     *
     * @param jObject The {@link CacheObject} to deserialize the object within.
     * @return The deserialized object that was in the {@link CacheObject}.
     * @throws DeserializeException if jObject cannot be deserialized.
     */
    @Sensitive
    private Object deserializeIfNecessary(@Sensitive CacheObject jObject) {

        if (jObject == null) {
            return null;
        }

        /*
         * Only deserialize if it hasn't been done for this instance already.
         */
        if (jObject.getObject() == null) {
            synchronized (jObject) {
                /*
                 * Check again in case we were waiting while another thread deserialized the
                 * object.
                 */
                if (jObject.getObject() == null) {
                    jObject.setObject(cacheService.deserialize(jObject.getObjectBytes()));
                }
            }
        }

        return jObject.getObject();
    }

    /**
     * An iterator that will proxy requests to the JCache provider's iterator.
     */
    @Trivial // If ever removed, consider @Sensitive for values.
    private class CacheProxyIterator implements Iterator<Cache.Entry<Object, Object>> {

        private Iterator<Cache.Entry<Object, Object>> iterator;

        CacheProxyIterator(Iterator<Cache.Entry<Object, Object>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Cache.Entry<Object, Object> next() {
            Cache.Entry<Object, Object> entry = iterator.next();
            return entry != null ? new CacheEntryProxy(entry) : null;
        }

        /**
         * A Cache.Entry that will proxy requests to the JCache provider's Cache.Entry.
         * Most importantly, it handles deserialization on {@link #getValue()} calls.
         */
        private class CacheEntryProxy implements Cache.Entry<Object, Object> {

            private Cache.Entry<Object, Object> entry;

            CacheEntryProxy(Cache.Entry<Object, Object> entry) {
                this.entry = entry;
            }

            @Override
            public Object getKey() {
                return entry.getKey();
            }

            @Override
            public Object getValue() {
                return deserializeIfNecessary((CacheObject) entry.getValue());
            }

            @Override
            public <T> T unwrap(Class<T> arg0) {
                return entry.unwrap(arg0);
            }
        }
    }
}
