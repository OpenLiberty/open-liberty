/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

import io.openliberty.jcache.JCacheObject;

/**
 * Proxy class for {@link javax.cache.Cache} that will handle all serialization
 * and deserialization internally. Serialization of Liberty classes can only be
 * handled by Liberty's {@link SerializationService} so that work is done here.
 *
 * @see Cache
 */
public class JCacheProxy implements Cache<Object, Object> {

    @SuppressWarnings("unused")
    private final static TraceComponent tc = Tr.register(JCacheProxy.class);

    private final Cache<Object, Object> jCache;
    private final JCacheServiceImpl jCacheService;

    /**
     * Instantiate a new {@link JCacheProxy}.
     *
     * @param jCache        The {@link Cache} that this instance will proxy requests to.
     * @param jCacheService The {@link JCacheServiceImpl} that manages the {@link Cache}.
     */
    public JCacheProxy(Cache<Object, Object> jCache, JCacheServiceImpl jCacheService) {
        if (jCache == null) {
            throw new NullPointerException("The JCache cannot be null.");
        }
        if (jCacheService == null) {
            throw new NullPointerException("The JCacheConfig cannot be null.");
        }

        this.jCache = jCache;
        this.jCacheService = jCacheService;
    }

    @Override
    public void clear() {
        jCache.clear();
    }

    @Override
    public void close() {
        jCache.close();
    }

    @Override
    public boolean containsKey(Object key) {
        return jCache.containsKey(key);
    }

    @Override
    public void deregisterCacheEntryListener(
                                             CacheEntryListenerConfiguration<Object, Object> cacheEntryListenerConfiguration) {
        jCache.deregisterCacheEntryListener(cacheEntryListenerConfiguration);
    }

    @Override
    @Sensitive
    public Object get(Object key) {
        /*
         * Get the object, and deserialize it if it has not already been deserialized.
         */
        JCacheObject jObject = (JCacheObject) jCache.get(key);
        return deserializeIfNecessary(jObject);
    }

    @Override
    @Sensitive
    public Map<Object, Object> getAll(Set<? extends Object> keys) {

        /*
         * Deserialize all of the values.
         */
        Map<Object, Object> values = jCache.getAll(keys);
        Map<Object, Object> results = new HashMap<Object, Object>();
        for (Map.Entry<Object, Object> entry : values.entrySet()) {
            results.put(entry.getKey(), deserializeIfNecessary((JCacheObject) entry.getValue()));
        }

        return results;
    }

    @Override
    @Sensitive
    public Object getAndPut(Object key, @Sensitive Object value) {
        JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
        JCacheObject oldValue = (JCacheObject) jCache.getAndPut(key, newValue);

        if (oldValue != null) {
            return deserializeIfNecessary(oldValue);
        } else {
            return null;
        }
    }

    @Override
    @Sensitive
    public Object getAndRemove(Object key) {
        return deserializeIfNecessary((JCacheObject) jCache.getAndRemove(key));
    }

    @Override
    @Sensitive
    public Object getAndReplace(Object key, @Sensitive Object value) {
        JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
        JCacheObject oldValue = (JCacheObject) jCache.getAndReplace(key, newValue);

        if (oldValue != null) {
            return deserializeIfNecessary(oldValue);
        } else {
            return null;
        }
    }

    @Override
    public CacheManager getCacheManager() {
        return jCache.getCacheManager();
    }

    @Override
    public <C extends Configuration<Object, Object>> C getConfiguration(Class<C> clazz) {
        return jCache.getConfiguration(clazz);
    }

    @Override
    public String getName() {
        return jCache.getName();
    }

    @Override
    public <T> T invoke(Object key, EntryProcessor<Object, Object, T> entryProcessor, Object... arguments) throws EntryProcessorException {
        return jCache.invoke(arguments, entryProcessor, arguments);
    }

    @Override
    public <T> Map<Object, EntryProcessorResult<T>> invokeAll(Set<? extends Object> keys,
                                                              EntryProcessor<Object, Object, T> entryProcessor, Object... arguments) {
        return jCache.invokeAll(keys, entryProcessor, arguments);
    }

    @Override
    public boolean isClosed() {
        return jCache.isClosed();
    }

    @Override
    public Iterator<Cache.Entry<Object, Object>> iterator() {
        return new JCacheProxyIterator(jCache.iterator());
    }

    @Override
    public void loadAll(Set<? extends Object> keys, boolean replaceExistingValues,
                        CompletionListener completionListener) {
        jCache.loadAll(keys, replaceExistingValues, completionListener);
    }

    @Override
    public void put(Object key, @Sensitive Object value) {
        JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
        jCache.put(key, newValue);
    }

    @Override
    public void putAll(@Sensitive Map<? extends Object, ? extends Object> map) {
        Map<Object, Object> values = new HashMap<Object, Object>();
        for (Map.Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
            values.put(key, newValue);
        }

        jCache.putAll(values);
    }

    @Override
    public boolean putIfAbsent(Object key, @Sensitive Object value) {
        JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
        return jCache.putIfAbsent(key, newValue);
    }

    @Override
    public void registerCacheEntryListener(
                                           CacheEntryListenerConfiguration<Object, Object> cacheEntryListenerConfiguration) {
        jCache.registerCacheEntryListener(cacheEntryListenerConfiguration);
    }

    @Override
    public boolean remove(Object key) {
        return jCache.remove(key);
    }

    @Override
    public boolean remove(Object key, @Sensitive Object oldValue) {
        JCacheObject oldValue1 = new JCacheObject(oldValue, jCacheService.serialize(oldValue));
        return jCache.remove(key, oldValue1);
    }

    @Override
    public void removeAll() {
        jCache.removeAll();
    }

    @Override
    public void removeAll(Set<? extends Object> keys) {
        jCache.removeAll(keys);
    }

    @Override
    public boolean replace(Object key, @Sensitive Object value) {
        JCacheObject newValue = new JCacheObject(value, jCacheService.serialize(value));
        return jCache.replace(key, newValue);
    }

    @Override
    public boolean replace(Object key, @Sensitive Object oldValue, @Sensitive Object newValue) {
        JCacheObject oldValue1 = new JCacheObject(oldValue, jCacheService.serialize(oldValue));
        JCacheObject newValue1 = new JCacheObject(newValue, jCacheService.serialize(newValue));
        return jCache.replace(key, oldValue1, newValue1);
    }

    @Override
    public <T> T unwrap(Class<T> clazz) {
        return jCache.unwrap(clazz);
    }

    /**
     * Deserialize the object contained within the {@link JCacheObject}.
     *
     * @param jObject The {@link JCacheObject} to deserialize the object within.
     * @return The deserialized object that was in the {@link JCacheObject}.
     */
    @Sensitive
    private Object deserializeIfNecessary(@Sensitive JCacheObject jObject) {

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
                    jObject.setObject(jCacheService.deserialize(jObject.getObjectBytes()));
                }
            }
        }

        return jObject.getObject();
    }

    /**
     * An iterator that will proxy requests to the JCache provider's iterator.
     */
    @Trivial // If ever removed, consider @Sensitive for values.
    private class JCacheProxyIterator implements Iterator<Cache.Entry<Object, Object>> {

        private Iterator<Cache.Entry<Object, Object>> iterator;

        JCacheProxyIterator(Iterator<Cache.Entry<Object, Object>> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasNext() {
            return iterator.hasNext();
        }

        @Override
        public Cache.Entry<Object, Object> next() {
            Cache.Entry<Object, Object> entry = iterator.next();
            return entry != null ? new JCacheEntryProxy(entry) : null;
        }

        /**
         * A Cache.Entry that will proxy requests to the JCache provider's Cache.Entry.
         * Most importantly, it handles deserialization on {@link #getValue()} calls.
         */
        private class JCacheEntryProxy implements Cache.Entry<Object, Object> {

            private Cache.Entry<Object, Object> entry;

            JCacheEntryProxy(Cache.Entry<Object, Object> entry) {
                this.entry = entry;
            }

            @Override
            public Object getKey() {
                return entry.getKey();
            }

            @Override
            public Object getValue() {
                return deserializeIfNecessary((JCacheObject) entry.getValue());
            }

            @Override
            public <T> T unwrap(Class<T> arg0) {
                return entry.unwrap(arg0);
            }
        }
    }
}
