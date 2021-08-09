/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.internal.utils;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ibm.ws.security.common.structures.Cache;

public class TokenCacheTest {

    private Cache cache;

    @Before
    public void setUp() throws Exception {
        cache = new Cache(50000, 600000L); 
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void putByAccessToken() {
        String accessToken = "12345";
        String socialLoginConfigurationId = "123";
        CacheToken cacheToken = new CacheToken(accessToken, socialLoginConfigurationId);
        cache.put(accessToken, cacheToken);
        CacheToken tokenFromCache = (CacheToken) cache.get(accessToken);
        assertSame("The token must be cached.", cacheToken, tokenFromCache);
    }

}
