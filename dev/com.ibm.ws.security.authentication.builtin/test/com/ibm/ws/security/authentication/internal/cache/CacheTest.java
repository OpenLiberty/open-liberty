/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Test;

import com.ibm.ws.security.authentication.cache.CacheEvictionListener;
import com.ibm.ws.security.authentication.cache.CacheObject;

/**
 *
 */
public class CacheTest {
    private final Mockery context = new JUnit4Mockery();

    private final long defaultTimeoutInMilliSeconds = 600000;

    private final Set<Cache> registeredCachesForStoppingEvictionTasks = new HashSet<Cache>();

    @After
    public void tearDown() throws Exception {
        context.assertIsSatisfied();
        stopEvictionTasks();
    }

    private void stopEvictionTasks() {
        for (Cache cache : registeredCachesForStoppingEvictionTasks) {
            cache.stopEvictionTask();
        }
    }

    /**
     * Constructor Cache(int,int,long) shall set the entry limit
     * to the 2nd integer value.
     */
    @Test
    public void constructor_IntIntLong() {
        Cache cache = new Cache(0, 12345, 0);
        assertEquals(12345, cache.getEntryLimit());
    }

    /**
     * insert shall not evict any entries in the cache until the entryLimit
     * is reached.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void insert_withoutEvict() {
        final CacheEvictionListener mockListener = context.mock(CacheEvictionListener.class);
        Set<CacheEvictionListener> mockCacheEvictionListenerSet = new HashSet<CacheEvictionListener>();
        mockCacheEvictionListenerSet.add(mockListener);
        context.checking(new Expectations() {
            {
                never(mockListener).evicted(with(any(ArrayList.class)));
            }
        });
        Cache cache = new Cache(0, 0, defaultTimeoutInMilliSeconds, mockCacheEvictionListenerSet);
        registeredCachesForStoppingEvictionTasks.add(cache);
        cache.insert("1", 1);
    }

    /**
     * There must be an eviction after the timeout / 2.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void insert_timeoutEvict() {
        final CacheEvictionListener mockListener = context.mock(CacheEvictionListener.class);
        Set<CacheEvictionListener> mockCacheEvictionListenerSet = new HashSet<CacheEvictionListener>();
        mockCacheEvictionListenerSet.add(mockListener);
        context.checking(new Expectations() {
            {
                allowing(mockListener).evicted(with(any(ArrayList.class)));
            }
        });
        Cache cache = new Cache(0, 0, 5, mockCacheEvictionListenerSet);
        registeredCachesForStoppingEvictionTasks.add(cache);
        cache.insert("1", 1);
    }

    /**
     * insert shall forcefully evict entries in the cache, however this does
     * not happen immediately, instead there are some internal tables that
     * need to be shifted around for this to occur.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void insert_withEvict() {
        final CacheEvictionListener mockListener = context.mock(CacheEvictionListener.class);
        Set<CacheEvictionListener> mockCacheEvictionListenerSet = new HashSet<CacheEvictionListener>();
        mockCacheEvictionListenerSet.add(mockListener);
        context.checking(new Expectations() {
            {
                one(mockListener).evicted(with(any(ArrayList.class)));
            }
        });
        Cache cache = new Cache(0, 1, defaultTimeoutInMilliSeconds, mockCacheEvictionListenerSet);
        registeredCachesForStoppingEvictionTasks.add(cache);
        cache.insert("1", 1);
        cache.insert("2", 2);
        cache.insert("3", 3);
        cache.insert("4", 4);
    }

    /**
     * isEvictionRequired shall return false if the entryLimit
     * is zero.
     */
    @Test
    public void isEvictionRequired_limitIsZero() {
        Cache cache = new Cache(0, 0, 0);
        assertFalse(cache.isEvictionRequired());
    }

    /**
     * isEvictionRequired shall return false if the entryLimit
     * is MAX INT.
     */
    @Test
    public void isEvictionRequired_limitIsMaxInt() {
        Cache cache = new Cache(0, Integer.MAX_VALUE, 0);
        assertFalse(cache.isEvictionRequired());
    }

    /**
     * isEvictionRequired shall return false if the cache
     * is empty.
     */
    @Test
    public void isEvictionRequired_empty() {
        Cache cache = new Cache(0, 12345, 0);
        assertFalse(cache.isEvictionRequired());
    }

    /**
     * isEvictionRequired shall return false if the cache
     * size is under entryLimit by 1 or less.
     */
    @Test
    public void isEvictionRequired_underByOne() {
        Cache cache = new Cache(10, 2, 0);
        cache.insert("1", 1);
        assertFalse(cache.isEvictionRequired());
    }

    /**
     * isEvictionRequired shall return false if the cache
     * size is equal to entryLimit.
     */
    @Test
    public void isEvictionRequired_equals() {
        Cache cache = new Cache(10, 2, 0);
        cache.insert("1", 1);
        cache.insert("2", 2);
        assertFalse(cache.isEvictionRequired());
    }

    /**
     * isEvictionRequired shall return true if the cache
     * size is over entryLimit by 1 or greater.
     */
    @Test
    public void isEvictionRequired_overByOne() {
        Cache cache = new Cache(10, 2, 0);
        cache.insert("1", 1);
        cache.insert("2", 2);
        cache.insert("3", 3);
        assertTrue(cache.isEvictionRequired());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void remove_EntryDoesNotExist_withoutEvict() {
        final CacheEvictionListener mockListener = context.mock(CacheEvictionListener.class);
        Set<CacheEvictionListener> mockCacheEvictionListenerSet = new HashSet<CacheEvictionListener>();
        mockCacheEvictionListenerSet.add(mockListener);
        context.checking(new Expectations() {
            {
                never(mockListener).evicted(with(any(ArrayList.class)));
            }
        });
        Cache cache = new Cache(10, Integer.MAX_VALUE, 0, mockCacheEvictionListenerSet);
        registeredCachesForStoppingEvictionTasks.add(cache);
        cache.remove("keyForEntryThatDoesNotExist");
    }

    @Test
    public void evictStaleEntries() {
        CacheEvictionListenerDouble cacheEvictionListener = new CacheEvictionListenerDouble();
        Set<CacheEvictionListener> evictionListeners = new HashSet<CacheEvictionListener>();
        evictionListeners.add(cacheEvictionListener);

        Cache cache = new Cache(10, Integer.MAX_VALUE, 0, evictionListeners);
        cache.insert("1", new CacheObject(new Subject()));
        cache.evictStaleEntries();
        cache.evictStaleEntries();
        cache.evictStaleEntries();
        Object victim = cacheEvictionListener.victims.get(0);
        assertNotNull("There must be an eviction victim.", victim);
        assertTrue("The victim must be an instance of CacheObject.", victim instanceof CacheObject);
    }

    class CacheEvictionListenerDouble implements CacheEvictionListener {
        public List<Object> victims;

        @Override
        public void evicted(List<Object> victims) {
            this.victims = victims;
        }
    };

}
