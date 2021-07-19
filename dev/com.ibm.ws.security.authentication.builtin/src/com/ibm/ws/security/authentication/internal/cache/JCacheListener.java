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
package com.ibm.ws.security.authentication.internal.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.cache.CacheEvictionListener;

import io.openliberty.jcache.JCacheService;

/**
 * JCacheListener that converts JCache {@link CacheEntryRemovedListener} and {@link CacheEntryExpiredListener}
 * events into {@link CacheEvictionListener} events.
 *
 * @param <K> the type of keys maintained the cache
 * @param <V> the type of cached values
 */
public class JCacheListener<K, V> implements CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, CacheEntryCreatedListener<K, V>, CacheEntryUpdatedListener<K, V>, Serializable {

    private static final TraceComponent tc = Tr.register(JCacheListener.class, "Authentication");

    /** TODO GENERATE BETTER SERIALVERSIONUID... */
    private static final long serialVersionUID = 1L;

    /**
     * Listener for cache eviction notifications.
     */
    private transient final Set<CacheEvictionListener> cacheEvictionListenerSet;

    private transient JCacheService jCacheService = null;

    public JCacheListener(JCacheService jcacheService, Set<CacheEvictionListener> cacheEvictionListenerSet) {
        this.jCacheService = jcacheService;
        this.cacheEvictionListenerSet = cacheEvictionListenerSet;
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        /*
         * Don't go through the work of deserializing the objects if we have no listeners.
         */
        if (cacheEvictionListenerSet.isEmpty()) {
            return;
        }

        /*
         * Deserialize the removed victims.
         */
        List<Object> victims = new ArrayList<Object>();
        Iterator<CacheEntryEvent<? extends K, ? extends V>> iter = events.iterator();
        while (iter.hasNext()) {
            CacheEntryEvent<? extends K, ? extends V> event = iter.next();
            if (event.getValue() instanceof byte[]) {
                Object victim = jCacheService.serialize(event.getValue());
                if (victim != null) {
                    victims.add(victim);
                }
            }
        }

        /*
         * If there are any victims, notify listeners.
         */
        if (!victims.isEmpty()) {
            for (CacheEvictionListener cel : cacheEvictionListenerSet) {
                cel.evicted(victims);
            }
        }
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        /*
         * Don't go through the work of deserializing the objects if we have no listeners.
         */
        if (cacheEvictionListenerSet.isEmpty()) {
            return;
        }

        /*
         * Deserialize the expired victims.
         */
        List<Object> victims = new ArrayList<Object>();
        Iterator<CacheEntryEvent<? extends K, ? extends V>> iter = events.iterator();
        while (iter.hasNext()) {
            CacheEntryEvent<? extends K, ? extends V> event = iter.next();
            if (event.getValue() instanceof byte[]) {
                Object victim = jCacheService.deserialize((byte[]) event.getValue());
                if (victim != null) {
                    victims.add(victim);
                }
            }
        }

        /*
         * If there are any victims, notify listeners.
         */
        if (!victims.isEmpty()) {
            for (CacheEvictionListener cel : cacheEvictionListenerSet) {
                cel.evicted(victims);
            }
        }
    }

    @Trivial
    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        // TODO ONLY FOR DEBUGGING
        List<Object> updatedKeys = new ArrayList<Object>();
        Iterator<CacheEntryEvent<? extends K, ? extends V>> iter = events.iterator();
        while (iter.hasNext()) {
            CacheEntryEvent<? extends K, ? extends V> event = iter.next();
            updatedKeys.add(event.getKey());
        }

        Tr.debug(tc, "The following JCache keys were updated.", updatedKeys);
    }

    @Trivial
    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> events) throws CacheEntryListenerException {
        // TODO ONLY FOR DEBUGGING
        List<Object> updatedKeys = new ArrayList<Object>();
        Iterator<CacheEntryEvent<? extends K, ? extends V>> iter = events.iterator();
        while (iter.hasNext()) {
            CacheEntryEvent<? extends K, ? extends V> event = iter.next();
            updatedKeys.add(event.getKey());
        }

        Tr.debug(tc, "The following JCache keys were created.", updatedKeys);
    }

}
