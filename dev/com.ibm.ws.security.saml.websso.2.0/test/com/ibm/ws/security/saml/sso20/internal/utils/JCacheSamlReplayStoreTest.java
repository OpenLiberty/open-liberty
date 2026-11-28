/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.saml.sso20.internal.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.cache.Cache;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import io.openliberty.jcache.CacheService;
import test.common.SharedOutputManager;

public class JCacheSamlReplayStoreTest {

    static SharedOutputManager outputMgr = SharedOutputManager.getInstance();
    @Rule
    public TestRule managerRule = outputMgr;

    private CacheService cacheService;
    private Cache<Object, Object> cache;
    private JCacheSamlReplayStore store;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        cacheService = mock(CacheService.class);
        cache = mock(Cache.class);
        when(cacheService.getCache()).thenReturn(cache);
        store = new JCacheSamlReplayStore(cacheService);
    }

    @Test
    public void putIfAbsentTrueMeansFirstUse() throws Exception {
        when(cache.putIfAbsent(eq("k"), any())).thenReturn(true);
        assertFalse(store.registerIfAbsent("k", 1L));
    }

    @Test
    public void putIfAbsentFalseMeansReplay() throws Exception {
        when(cache.putIfAbsent(eq("k"), any())).thenReturn(false);
        assertTrue(store.registerIfAbsent("k", 1L));
    }

    @Test
    public void nullCacheIsAStoreFailure() {
        when(cacheService.getCache()).thenReturn(null);
        try {
            store.registerIfAbsent("k", 1L);
            fail("Expected SamlReplayStoreException");
        } catch (SamlReplayStoreException expected) {
            // expected
        }
    }

    @Test
    public void providerExceptionIsWrapped() {
        RuntimeException boom = new RuntimeException("boom");
        when(cache.putIfAbsent(eq("k"), any())).thenThrow(boom);
        try {
            store.registerIfAbsent("k", 1L);
            fail("Expected SamlReplayStoreException");
        } catch (SamlReplayStoreException expected) {
            assertSame(boom, expected.getCause());
        }
    }
}
